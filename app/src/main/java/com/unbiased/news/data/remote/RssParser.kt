package com.unbiased.news.data.remote

import com.unbiased.news.domain.model.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import java.io.IOException
import java.io.StringReader
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Dedicated RSS/Atom feed parser with robust error handling.
 * Supports both RSS 2.0 and Atom 1.0 formats.
 */
class RssParser(
    private val okHttpClient: OkHttpClient
) {
    companion object {
        private val DATE_FORMATS = listOf(
            "EEE, dd MMM yyyy HH:mm:ss Z",   // RFC 822 (standard)
            "EEE, dd MMM yyyy HH:mm:ss z",   // RFC 822 variant
            "yyyy-MM-dd'T'HH:mm:ssZ",         // ISO 8601
            "yyyy-MM-dd'T'HH:mm:ss'Z'",       // ISO 8601 UTC
            "yyyy-MM-dd'T'HH:mm:ss.SSSZ",     // ISO 8601 with millis
            "yyyy-MM-dd HH:mm:ss",            // Fallback
            "dd MMM yyyy HH:mm:ss Z"          // Alternative format
        )

        private const val MAX_DESCRIPTION_LENGTH = 500
    }

    /**
     * Result of parsing an RSS feed.
     */
    sealed class ParseResult {
        data class Success(val articles: List<ParsedArticle>) : ParseResult()
        data class Error(val message: String, val exception: Exception? = null) : ParseResult()
    }

    /**
     * Represents a parsed article from an RSS/Atom feed.
     */
    data class ParsedArticle(
        val id: String,
        val title: String,
        val description: String?,
        val link: String,
        val imageUrl: String?,
        val pubDate: Long,
        val sourceId: String,
        val sourceName: String
    )

    /**
     * Feed format detection.
     */
    private enum class FeedFormat {
        RSS_2_0,
        ATOM_1_0,
        UNKNOWN
    }

    /**
     * Fetches and parses an RSS feed from a source.
     */
    suspend fun fetchAndParse(source: Source): ParseResult = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(source.url)
                .build()

            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext ParseResult.Error(
                    "HTTP ${response.code}: ${response.message}"
                )
            }

            val feedContent = response.body?.string()
            if (feedContent.isNullOrBlank()) {
                return@withContext ParseResult.Error("Empty response body")
            }

            val articles = parseFeed(feedContent, source)
            ParseResult.Success(articles)
        } catch (e: IOException) {
            ParseResult.Error("Network error: ${e.message}", e)
        } catch (e: XmlPullParserException) {
            ParseResult.Error("XML parsing error: ${e.message}", e)
        } catch (e: Exception) {
            ParseResult.Error("Unexpected error: ${e.message}", e)
        }
    }

    /**
     * Parses RSS/Atom feed content into articles.
     */
    private fun parseFeed(feedContent: String, source: Source): List<ParsedArticle> {
        val articles = mutableListOf<ParsedArticle>()

        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false // Simpler parsing without namespaces
            val parser = factory.newPullParser()
            parser.setInput(StringReader(feedContent))

            var eventType = parser.eventType
            var currentArticle: MutableMap<String, String> = mutableMapOf()
            var inItem = false
            var feedFormat = FeedFormat.UNKNOWN

            // Detect feed format from root element
            while (eventType != XmlPullParser.END_DOCUMENT && feedFormat == FeedFormat.UNKNOWN) {
                if (eventType == XmlPullParser.START_TAG) {
                    feedFormat = when (parser.name.lowercase()) {
                        "rss", "channel" -> FeedFormat.RSS_2_0
                        "feed" -> FeedFormat.ATOM_1_0
                        else -> FeedFormat.UNKNOWN
                    }
                }
                eventType = parser.next()
            }

            // Reset parser for actual parsing
            parser.setInput(StringReader(feedContent))
            eventType = parser.eventType

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        val tagName = parser.name.lowercase()
                        when (tagName) {
                            "item" -> {
                                inItem = true
                                currentArticle = mutableMapOf()
                            }
                            "entry" -> { // Atom format
                                inItem = true
                                currentArticle = mutableMapOf()
                            }
                            "enclosure" -> {
                                if (inItem) {
                                    val url = parser.getAttributeValue(null, "url")
                                    val type = parser.getAttributeValue(null, "type")
                                    // Only capture image enclosures
                                    if (url != null && (type?.startsWith("image") == true || type == null)) {
                                        currentArticle["imageUrl"] = url
                                    }
                                }
                            }
                            "link" -> {
                                // In Atom, link is in href attribute
                                if (inItem && feedFormat == FeedFormat.ATOM_1_0) {
                                    val href = parser.getAttributeValue(null, "href")
                                    if (!href.isNullOrBlank()) {
                                        // Prefer alternate link
                                        val rel = parser.getAttributeValue(null, "rel")
                                        if (rel == null || rel == "alternate") {
                                            currentArticle["link"] = href
                                        }
                                    }
                                }
                            }
                            "media:content", "media:thumbnail" -> {
                                if (inItem) {
                                    val url = parser.getAttributeValue(null, "url")
                                    if (!url.isNullOrBlank()) {
                                        currentArticle["imageUrl"] = url
                                    }
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        val tagName = parser.name.lowercase()
                        if (inItem) {
                            when (tagName) {
                                "item", "entry" -> {
                                    val article = createArticle(currentArticle, source)
                                    if (article != null) {
                                        articles.add(article)
                                    }
                                    inItem = false
                                    currentArticle = mutableMapOf()
                                }
                                "title" -> {
                                    val text = parser.text
                                    if (!text.isNullOrBlank()) {
                                        currentArticle["title"] = text
                                    }
                                }
                                "link" -> {
                                    // In RSS, link is text content
                                    val text = parser.text
                                    if (!text.isNullOrBlank() && feedFormat == FeedFormat.RSS_2_0) {
                                        currentArticle["link"] = text
                                    }
                                }
                                "description", "summary" -> {
                                    val text = parser.text
                                    if (!text.isNullOrBlank()) {
                                        currentArticle["description"] = text
                                    }
                                }
                                "content" -> {
                                    // Use content as description fallback in Atom
                                    if (!currentArticle.containsKey("description")) {
                                        val text = parser.text
                                        if (!text.isNullOrBlank()) {
                                            currentArticle["description"] = text
                                        }
                                    }
                                }
                                "pubdate", "published", "updated" -> {
                                    val text = parser.text
                                    if (!text.isNullOrBlank()) {
                                        currentArticle["pubdate"] = text
                                    }
                                }
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: XmlPullParserException) {
            // Return what we have so far
        } catch (e: Exception) {
            // Return what we have so far
        }

        return articles
    }

    /**
     * Creates a ParsedArticle from the collected data.
     */
    private fun createArticle(
        data: Map<String, String>,
        source: Source
    ): ParsedArticle? {
        val title = sanitizeHtml(data["title"]) ?: return null
        val link = data["link"]?.trim() ?: return null

        if (title.isBlank() || link.isBlank()) return null

        return ParsedArticle(
            id = generateId(link),
            title = title,
            description = sanitizeHtml(data["description"] ?: data["summary"]),
            link = link,
            imageUrl = data["imageUrl"]?.trim()?.takeIf { it.isNotBlank() },
            pubDate = parseDate(data["pubdate"]),
            sourceId = source.sourceId,
            sourceName = source.name
        )
    }

    /**
     * Generates a stable ID from a URL using SHA-256.
     */
    fun generateId(url: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(url.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }.take(16)
    }

    /**
     * Parses various date formats found in RSS feeds.
     */
    fun parseDate(dateStr: String?): Long {
        if (dateStr.isNullOrBlank()) return System.currentTimeMillis()

        val cleanDate = dateStr.trim()

        for (format in DATE_FORMATS) {
            try {
                val sdf = SimpleDateFormat(format, Locale.US)
                sdf.isLenient = false
                return sdf.parse(cleanDate)?.time ?: continue
            } catch (e: Exception) {
                continue
            }
        }

        // Fallback to current time
        return System.currentTimeMillis()
    }

    /**
     * Sanitizes HTML content from RSS descriptions.
     */
    private fun sanitizeHtml(input: String?): String? {
        if (input.isNullOrBlank()) return null

        return input
            // Remove CDATA sections
            .replace(Regex("<!\\[CDATA\\["), "")
            .replace(Regex("\\]\\]>"), "")
            // Remove HTML tags
            .replace(Regex("<[^>]*>"), "")
            // Decode HTML entities
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&nbsp;", " ")
            .replace(Regex("&#(\\d+);")) { match ->
                val code = match.groupValues[1].toIntOrNull()
                code?.toChar()?.toString() ?: match.value
            }
            // Normalize whitespace
            .replace(Regex("\\s+"), " ")
            .trim()
            // Limit length
            .take(MAX_DESCRIPTION_LENGTH)
    }
}
