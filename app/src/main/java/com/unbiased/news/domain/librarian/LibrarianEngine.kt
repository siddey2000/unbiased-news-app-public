package com.unbiased.news.domain.librarian

import android.util.Log
import com.unbiased.news.data.remote.GeminiApi
import com.unbiased.news.data.remote.GeminiContent
import com.unbiased.news.data.remote.GeminiGenerationConfig
import com.unbiased.news.data.remote.GeminiPart
import com.unbiased.news.data.remote.GeminiRequest
import com.unbiased.news.data.remote.Message
import com.unbiased.news.data.remote.OpenRouterApi
import com.unbiased.news.data.remote.OpenRouterRequest
import com.unbiased.news.domain.model.Article
import com.unbiased.news.domain.model.Source
import com.unbiased.news.data.model.ArticleEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.min
import kotlin.math.pow

/**
 * The LibrarianEngine is the core AI logic component.
 * It takes raw articles and filters them by relevance using the user's LLM.
 */
class LibrarianEngine(
    private val openRouterApi: OpenRouterApi,
    private val geminiApi: GeminiApi,
    private val okHttpClient: OkHttpClient
) {
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        internal const val RELEVANCE_THRESHOLD = 0.5f

        // Retry configuration
        private const val MAX_RETRIES = 3
        private const val INITIAL_RETRY_DELAY_MS = 1000L
        private const val MAX_RETRY_DELAY_MS = 10000L

        // Batching configuration (avoid token limits)
        private const val MAX_ARTICLES_PER_BATCH = 20

        private val DATE_FORMATS = listOf(
            "EEE, dd MMM yyyy HH:mm:ss Z",   // RFC 822 (standard)
            "EEE, dd MMM yyyy HH:mm:ss z",   // RFC 822 variant
            "yyyy-MM-dd'T'HH:mm:ssZ",         // ISO 8601
            "yyyy-MM-dd'T'HH:mm:ss'Z'",       // ISO 8601 UTC
            "yyyy-MM-dd HH:mm:ss"             // Fallback
        )
    }

    /**
     * Fetches RSS feed from a source and parses it into raw articles.
     */
    suspend fun fetchRssFeed(source: Source): List<RawArticle> = withContext(Dispatchers.IO) {
        try {
            Log.d("LibrarianEngine", "Fetching RSS from: ${source.name} - ${source.url}")
            val request = Request.Builder().url(source.url).build()
            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                Log.w("LibrarianEngine", "Failed to fetch ${source.name}: HTTP ${response.code}")
                return@withContext emptyList()
            }

            val feedContent = response.body?.string() ?: return@withContext emptyList()
            val articles = parseRssFeed(feedContent, source)
            Log.d("LibrarianEngine", "Parsed ${articles.size} articles from ${source.name}")
            articles
        } catch (e: Exception) {
            Log.e("LibrarianEngine", "Error fetching ${source.name}: ${e.message}")
            emptyList()
        }
    }

    /**
     * Analyzes articles for relevance to a given topic using LLM.
     * Returns only articles that meet the relevance threshold.
     * Batches large article sets to avoid token limits.
     */
    suspend fun analyzeRelevance(
        articles: List<RawArticle>,
        topic: String,
        apiKey: String,
        provider: LlmProvider = LlmProvider.OPENROUTER
    ): List<ScoredArticle> = withContext(Dispatchers.IO) {
        Log.d("LibrarianEngine", "Analyzing ${articles.size} articles for topic: $topic using ${provider.name}")
        if (articles.isEmpty()) return@withContext emptyList()

        // Process in batches to avoid token limits
        val allScores = mutableListOf<RelevanceScore>()
        val batches = articles.chunked(MAX_ARTICLES_PER_BATCH)

        for (batch in batches) {
            val systemPrompt = buildSystemPrompt()
            val userPrompt = buildUserPrompt(batch, topic)

            val batchScores = when (provider) {
                LlmProvider.OPENROUTER -> callOpenRouterWithRetry(systemPrompt, userPrompt, apiKey)
                LlmProvider.GEMINI -> callGeminiWithRetry(systemPrompt, userPrompt, apiKey)
            }
            Log.d("LibrarianEngine", "Batch returned ${batchScores.size} scores")
            allScores.addAll(batchScores)

            // Small delay between batches to respect rate limits
            if (batches.size > 1 && batch != batches.last()) {
                delay(500)
            }
        }

        val scoredArticles = articles.mapNotNull { article ->
            val score = allScores.find { it.id == article.id }?.relevance ?: 0f
            if (score >= RELEVANCE_THRESHOLD) {
                ScoredArticle(article, score)
            } else null
        }
        Log.d("LibrarianEngine", "${scoredArticles.size} articles passed relevance threshold (>= $RELEVANCE_THRESHOLD)")
        scoredArticles
    }

    private fun buildSystemPrompt(): String {
        return "You are the Librarian for the Unbiased News App. Analyze these headlines for relevance to the topic. " +
                "Return a JSON array of objects with 'id' and 'relevance' fields (0.0-1.0). " +
                "Example: [{\"id\":\"abc123\",\"relevance\":0.85}]. " +
                "Ignore political bias; focus only on factual topicality."
    }

    private fun buildUserPrompt(articles: List<RawArticle>, topic: String): String {
        val articleList = articles.joinToString(",") { article ->
            """{"id":"${article.id}","title":"${article.title.escapeJson()}","description":"${(article.description ?: "").escapeJson()}"}"""
        }
        return """{"topic":"$topic","articles":[$articleList]}"""
    }

    private suspend fun callOpenRouter(
        systemPrompt: String,
        userPrompt: String,
        apiKey: String
    ): List<RelevanceScore> {
        val request = OpenRouterRequest(
            model = "openai/gpt-4o-mini",
            messages = listOf(
                Message("system", systemPrompt),
                Message("user", userPrompt)
            )
        )

        val response = openRouterApi.analyzeHeadlines("Bearer $apiKey", request)

        if (!response.isSuccessful) {
            Log.e("LibrarianEngine", "OpenRouter API failed: HTTP ${response.code()} - ${response.errorBody()?.string()}")
            return emptyList()
        }

        val content = response.body()?.choices?.firstOrNull()?.message?.content
        if (content == null) {
            Log.e("LibrarianEngine", "OpenRouter returned empty content")
            return emptyList()
        }
        Log.d("LibrarianEngine", "OpenRouter response: ${content.take(200)}...")
        return parseScoresResponse(content)
    }

    /**
     * OpenRouter API call with exponential backoff retry logic.
     */
    private suspend fun callOpenRouterWithRetry(
        systemPrompt: String,
        userPrompt: String,
        apiKey: String
    ): List<RelevanceScore> {
        repeat(MAX_RETRIES) { attempt ->
            try {
                val result = callOpenRouter(systemPrompt, userPrompt, apiKey)
                if (result.isNotEmpty()) return result
            } catch (e: Exception) {
                Log.e("LibrarianEngine", "OpenRouter attempt ${attempt + 1} failed: ${e.message}")
            }

            if (attempt < MAX_RETRIES - 1) {
                val delayMs = calculateRetryDelay(attempt)
                Log.d("LibrarianEngine", "Retrying in ${delayMs}ms...")
                delay(delayMs)
            }
        }
        Log.e("LibrarianEngine", "OpenRouter failed after $MAX_RETRIES attempts")
        return emptyList()
    }

    private suspend fun callGemini(
        systemPrompt: String,
        userPrompt: String,
        apiKey: String
    ): List<RelevanceScore> {
        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(
                        GeminiPart("$systemPrompt\n\n$userPrompt")
                    )
                )
            ),
            generationConfig = GeminiGenerationConfig(responseMimeType = "application/json")
        )

        val response = geminiApi.analyzeHeadlines(
            model = "gemini-2.0-flash",
            apiKey = apiKey,
            request = request
        )

        if (!response.isSuccessful) return emptyList()

        val content = response.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: return emptyList()
        return parseScoresResponse(content)
    }

    /**
     * Gemini API call with exponential backoff retry logic.
     */
    private suspend fun callGeminiWithRetry(
        systemPrompt: String,
        userPrompt: String,
        apiKey: String
    ): List<RelevanceScore> {
        repeat(MAX_RETRIES) { attempt ->
            try {
                val result = callGemini(systemPrompt, userPrompt, apiKey)
                if (result.isNotEmpty()) return result
            } catch (e: Exception) {
                // Log error (in production, use proper logging)
            }

            if (attempt < MAX_RETRIES - 1) {
                val delayMs = calculateRetryDelay(attempt)
                delay(delayMs)
            }
        }
        return emptyList()
    }

    /**
     * Calculate exponential backoff delay with jitter.
     */
    private fun calculateRetryDelay(attempt: Int): Long {
        val exponentialDelay = INITIAL_RETRY_DELAY_MS * 2.0.pow(attempt).toLong()
        val cappedDelay = min(exponentialDelay, MAX_RETRY_DELAY_MS)
        // Add jitter (±20%)
        val jitter = (cappedDelay * 0.2 * Math.random()).toLong()
        return cappedDelay + jitter
    }

    private fun parseScoresResponse(content: String): List<RelevanceScore> {
        // Try different response formats the LLM might return
        val trimmedContent = content.trim()

        // Try direct array first: [{...}, {...}]
        if (trimmedContent.startsWith("[")) {
            return try {
                val scores = json.decodeFromString<List<RelevanceScore>>(trimmedContent)
                Log.d("LibrarianEngine", "Parsed ${scores.size} scores from direct array")
                scores
            } catch (e: Exception) {
                Log.e("LibrarianEngine", "Failed to parse direct array: ${e.message}")
                emptyList()
            }
        }

        // Try wrapped formats: {"scores": [...]}, {"results": [...]}, {"articles": [...]}
        return try {
            // Try standard ScoresResponse first
            val responseJson = json.decodeFromString<ScoresResponse>(trimmedContent)
            Log.d("LibrarianEngine", "Parsed ${responseJson.scores.size} scores from ScoresResponse")
            responseJson.scores
        } catch (e: Exception) {
            Log.w("LibrarianEngine", "Failed to parse as ScoresResponse: ${e.message}")
            try {
                // Extract array from generic wrapper using regex
                val arrayMatch = Regex("""\[\s*\{[^]]*"id"[^]]*"relevance"[^]]*\}.*\]""", RegexOption.DOT_MATCHES_ALL)
                    .find(trimmedContent)
                if (arrayMatch != null) {
                    val arrayContent = arrayMatch.value
                    val scores = json.decodeFromString<List<RelevanceScore>>(arrayContent)
                    Log.d("LibrarianEngine", "Parsed ${scores.size} scores from extracted array")
                    scores
                } else {
                    Log.e("LibrarianEngine", "Could not find score array in response")
                    emptyList()
                }
            } catch (e: Exception) {
                Log.e("LibrarianEngine", "Failed to parse scores: ${e.message}")
                emptyList()
            }
        }
    }

    /**
     * Sanitizes HTML from RSS descriptions.
     * Removes HTML tags and decodes common entities.
     */
    private fun sanitizeHtml(input: String?): String? {
        if (input.isNullOrBlank()) return null

        return input
            // Remove HTML tags
            .replace(Regex("<[^>]*>"), "")
            // Decode common HTML entities
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&nbsp;", " ")
            // Remove CDATA markers
            .replace(Regex("<!\\[CDATA\\[|\\]\\]>"), "")
            // Trim whitespace
            .trim()
            .take(500) // Limit description length
    }

    /**
     * Validates that the API key has the correct format.
     */
    fun validateApiKey(apiKey: String, provider: LlmProvider): Boolean {
        return when (provider) {
            LlmProvider.OPENROUTER -> apiKey.startsWith("sk-or-") || apiKey.startsWith("sk-")
            LlmProvider.GEMINI -> apiKey.length >= 20 // Gemini keys are typically 39 chars
        }
    }

    // RSS Parsing
    private fun parseRssFeed(feedContent: String, source: Source): List<RawArticle> {
        val articles = mutableListOf<RawArticle>()

        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false // Simpler parsing without namespaces
            val parser = factory.newPullParser()
            parser.setInput(StringReader(feedContent))

            var eventType = parser.eventType
            var currentArticle: MutableMap<String, String> = mutableMapOf()
            var inItem = false
            var currentTag: String? = null

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        val tagName = parser.name.lowercase()
                        currentTag = tagName
                        when (tagName) {
                            "item", "entry" -> {
                                inItem = true
                                currentArticle = mutableMapOf()
                            }
                            "enclosure" -> {
                                if (inItem) {
                                    val url = parser.getAttributeValue(null, "url")
                                    if (!url.isNullOrBlank()) {
                                        currentArticle["imageUrl"] = url
                                    }
                                }
                            }
                            "link" -> {
                                // Handle Atom-style link with href attribute
                                if (inItem) {
                                    val href = parser.getAttributeValue(null, "href")
                                    if (!href.isNullOrBlank()) {
                                        currentArticle["link"] = href
                                    }
                                }
                            }
                        }
                    }
                    XmlPullParser.TEXT -> {
                        // Capture text content for current tag
                        if (inItem && currentTag != null) {
                            val text = parser.text
                            if (!text.isNullOrBlank()) {
                                when (currentTag) {
                                    "title" -> currentArticle["title"] = text
                                    "link" -> {
                                        // RSS-style link as text content (only if not already set via href)
                                        if (!currentArticle.containsKey("link")) {
                                            currentArticle["link"] = text
                                        }
                                    }
                                    "description", "summary" -> currentArticle["description"] = text
                                    "pubdate", "published", "updated" -> currentArticle["pubdate"] = text
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        val tagName = parser.name.lowercase()
                        if (inItem && tagName in listOf("item", "entry")) {
                            val title = sanitizeHtml(currentArticle["title"])
                            val link = currentArticle["link"]
                            if (!title.isNullOrBlank() && !link.isNullOrBlank()) {
                                articles.add(
                                    RawArticle(
                                        id = generateId(link),
                                        title = title,
                                        description = sanitizeHtml(currentArticle["description"] ?: currentArticle["summary"]),
                                        link = link,
                                        imageUrl = currentArticle["imageUrl"],
                                        pubDate = parseDate(currentArticle["pubdate"] ?: currentArticle["published"]),
                                        sourceId = source.sourceId,
                                        sourceName = source.name
                                    )
                                )
                            } else {
                                Log.w("LibrarianEngine", "Skipped article from ${source.name}: title=$title, link=$link")
                            }
                            currentArticle = mutableMapOf()
                            inItem = false
                        }
                        currentTag = null
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e("LibrarianEngine", "RSS parse error for ${source.name}: ${e.message}")
        }

        return articles
    }

    fun parseDate(dateStr: String?): Long {
        if (dateStr.isNullOrBlank()) return System.currentTimeMillis()

        for (format in DATE_FORMATS) {
            try {
                return SimpleDateFormat(format, Locale.US).parse(dateStr)?.time
                    ?: continue
            } catch (e: Exception) {
                continue
            }
        }
        return System.currentTimeMillis()
    }

    fun generateId(url: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(url.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }.take(16)
    }

    private fun String.escapeJson(): String {
        return replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    // Data classes for internal use
    data class RawArticle(
        val id: String,
        val title: String,
        val description: String?,
        val link: String,
        val imageUrl: String?,
        val pubDate: Long,
        val sourceId: String,
        val sourceName: String
    )

    data class ScoredArticle(
        val article: RawArticle,
        val relevanceScore: Float
    )

    @Serializable
    data class RelevanceScore(
        val id: String,
        val relevance: Float,
        val reasoning: String? = null
    )

    @Serializable
    data class ScoresResponse(
        val scores: List<RelevanceScore>
    )

    enum class LlmProvider {
        OPENROUTER,
        GEMINI
    }
}

// Extension functions to convert between domain and data models
fun LibrarianEngine.RawArticle.toArticleEntity(topicId: String, relevanceScore: Float): ArticleEntity =
    ArticleEntity(
        id = id,
        title = title,
        description = description,
        link = link,
        imageUrl = imageUrl,
        pubDate = pubDate,
        sourceName = sourceName,
        sourceId = sourceId,
        relevanceScore = relevanceScore,
        topicId = topicId
    )

fun ArticleEntity.toDomainArticle(): Article =
    Article(
        id = id,
        title = title,
        description = description,
        link = link,
        imageUrl = imageUrl,
        pubDate = pubDate,
        sourceName = sourceName,
        sourceId = sourceId,
        relevanceScore = relevanceScore,
        topicId = topicId
    )
