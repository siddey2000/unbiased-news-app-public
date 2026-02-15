package com.unbiased.news.domain.repository

import com.unbiased.news.domain.librarian.LibrarianEngine
import com.unbiased.news.domain.model.Article
import com.unbiased.news.domain.model.Source
import kotlinx.coroutines.flow.Flow

interface NewsRepository {

    suspend fun initializeSourcesIfNeeded()

    fun getEnabledSources(): Flow<List<Source>>

    fun getAllSources(): Flow<List<Source>>

    suspend fun updateSourceEnabled(sourceId: String, enabled: Boolean)

    fun getArticlesByTopic(topicId: String): Flow<List<Article>>

    fun getRecentArticles(limit: Int = 50): Flow<List<Article>>

    /**
     * Syncs articles for a topic with detailed result feedback.
     * Implements NetworkBoundResource pattern:
     * 1. Fetch from network
     * 2. If success, save to database
     * 3. If failure, return cached data if available
     */
    suspend fun syncArticlesForTopic(
        topicId: String,
        apiKey: String,
        provider: LibrarianEngine.LlmProvider = LibrarianEngine.LlmProvider.OPENROUTER
    ): SyncResult

    /**
     * Checks if there is cached data for a topic.
     */
    suspend fun hasCachedArticlesForTopic(topicId: String): Boolean

    /**
     * Gets the count of cached articles for a topic.
     */
    suspend fun getCachedArticleCount(topicId: String): Int

    suspend fun clearOldArticles(olderThanTimestamp: Long)
}
