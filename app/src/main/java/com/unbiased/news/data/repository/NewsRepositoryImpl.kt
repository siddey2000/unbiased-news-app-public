package com.unbiased.news.data.repository

import com.unbiased.news.data.local.ArticleDao
import com.unbiased.news.data.local.SourceDao
import com.unbiased.news.data.model.ArticleEntity
import com.unbiased.news.domain.librarian.LibrarianEngine
import com.unbiased.news.domain.librarian.fetchAllSources
import com.unbiased.news.domain.librarian.toArticleEntity
import com.unbiased.news.domain.librarian.toDomainArticle
import com.unbiased.news.domain.model.Article
import com.unbiased.news.domain.model.Source
import com.unbiased.news.domain.repository.NewsRepository
import com.unbiased.news.domain.repository.SyncResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// Import extension function from AssetLoader
import com.unbiased.news.data.repository.toDomainSource

@Singleton
class NewsRepositoryImpl @Inject constructor(
    private val articleDao: ArticleDao,
    private val sourceDao: SourceDao,
    private val assetLoader: AssetLoader,
    private val librarianEngine: LibrarianEngine
) : NewsRepository {

    /**
     * Filters sources by topic to optimize sync performance.
     * Only fetches from sources relevant to the topic.
     */
    private fun filterSourcesByTopic(sources: List<Source>, topicId: String): List<Source> {
        // Map topics to relevant source categories
        val categoryMap = mapOf(
            "tech" to listOf("tech"),
            "politics" to listOf("politics"),
            "economy" to listOf("business"),
            "world" to listOf("world"),
            "science" to listOf("tech", "science", "general"),
            "health" to listOf("science", "health", "general"),
            "culture" to listOf("culture", "general")
        )

        val targetCategories = categoryMap[topicId] ?: return sources
        return sources.filter { it.category in targetCategories || it.category == "general" }
    }

    override suspend fun initializeSourcesIfNeeded() {
        val count = sourceDao.getSourceCount()
        if (count == 0) {
            val sources = assetLoader.loadSources()
            sourceDao.insertSources(sources.map { it.toEntity() })
        }
    }

    override fun getEnabledSources(): Flow<List<Source>> =
        sourceDao.getEnabledSources().map { entities ->
            entities.map { it.toDomainSource() }
        }

    override fun getAllSources(): Flow<List<Source>> =
        sourceDao.getAllSources().map { entities ->
            entities.map { it.toDomainSource() }
        }

    override suspend fun updateSourceEnabled(sourceId: String, enabled: Boolean) {
        sourceDao.updateSourceEnabled(sourceId, enabled)
    }

    override fun getArticlesByTopic(topicId: String): Flow<List<Article>> =
        articleDao.getArticlesByTopic(topicId).map { entities ->
            entities.map { it.toDomainArticle() }
        }

    override fun getRecentArticles(limit: Int): Flow<List<Article>> =
        articleDao.getRecentArticles(limit).map { entities ->
            entities.map { it.toDomainArticle() }
        }

    override suspend fun hasCachedArticlesForTopic(topicId: String): Boolean {
        return articleDao.getArticleCountByTopic(topicId) > 0
    }

    override suspend fun getCachedArticleCount(topicId: String): Int {
        return articleDao.getArticleCountByTopic(topicId)
    }

    /**
     * Syncs articles for a topic with NetworkBoundResource pattern.
     *
     * Flow:
     * 1. Fetch RSS feeds from all enabled sources
     * 2. Analyze with AI for relevance
     * 3. Save to local database
     * 4. Return success with counts
     *
     * On failure, returns cached data status if available.
     */
    override suspend fun syncArticlesForTopic(
        topicId: String,
        apiKey: String,
        provider: LibrarianEngine.LlmProvider
    ): SyncResult {
        return try {
            // Validate API key first
            if (!librarianEngine.validateApiKey(apiKey, provider)) {
                val cachedCount = getCachedArticleCount(topicId)
                return if (cachedCount > 0) {
                    SyncResult.Cached(
                        articlesCount = cachedCount,
                        reason = "Invalid API key format"
                    )
                } else {
                    SyncResult.Error(
                        message = "Invalid API key format for ${provider.name}",
                        hasCachedData = false
                    )
                }
            }

            // Get enabled sources
            val allSources = sourceDao.getEnabledSources().map { entities ->
                entities.map { it.toDomainSource() }
            }.first()

            if (allSources.isEmpty()) {
                return SyncResult.Error(
                    message = "No enabled sources found",
                    hasCachedData = hasCachedArticlesForTopic(topicId)
                )
            }

            // Filter sources by topic for performance optimization
            val sources = filterSourcesByTopic(allSources, topicId)

            if (sources.isEmpty()) {
                return SyncResult.Error(
                    message = "No sources found for topic: $topicId",
                    hasCachedData = hasCachedArticlesForTopic(topicId)
                )
            }

            // Fetch only relevant RSS feeds in parallel
            val allRawArticles = librarianEngine.fetchAllSources(sources)

            if (allRawArticles.isEmpty()) {
                val cachedCount = getCachedArticleCount(topicId)
                return if (cachedCount > 0) {
                    SyncResult.Cached(
                        articlesCount = cachedCount,
                        reason = "No articles fetched from sources"
                    )
                } else {
                    SyncResult.Error(
                        message = "Failed to fetch articles from any source",
                        hasCachedData = false
                    )
                }
            }

            // Filter by AI relevance
            val scoredArticles = librarianEngine.analyzeRelevance(
                articles = allRawArticles,
                topic = topicId,
                apiKey = apiKey,
                provider = provider
            )

            // Convert to entities and save
            val articleEntities = scoredArticles.map { scored ->
                scored.article.toArticleEntity(topicId, scored.relevanceScore)
            }

            articleDao.replaceArticlesForTopic(topicId, articleEntities)

            SyncResult.Success(
                articlesFetched = allRawArticles.size,
                articlesFiltered = allRawArticles.size - scoredArticles.size,
                source = "network"
            )
        } catch (e: Exception) {
            val cachedCount = getCachedArticleCount(topicId)
            if (cachedCount > 0) {
                SyncResult.Cached(
                    articlesCount = cachedCount,
                    reason = "Network error: ${e.message}"
                )
            } else {
                SyncResult.Error(
                    message = e.message ?: "Unknown error during sync",
                    throwable = e,
                    hasCachedData = false
                )
            }
        }
    }

    override suspend fun clearOldArticles(olderThanTimestamp: Long) {
        articleDao.deleteOldArticles(olderThanTimestamp)
    }
}
