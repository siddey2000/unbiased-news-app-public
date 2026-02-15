package com.unbiased.news.domain.repository

/**
 * Result wrapper for sync operations.
 * Provides detailed feedback about the sync state.
 */
sealed class SyncResult {
    data class Success(
        val articlesFetched: Int,
        val articlesFiltered: Int,
        val source: String = "network"
    ) : SyncResult()

    data class Cached(
        val articlesCount: Int,
        val reason: String = "Using cached data"
    ) : SyncResult()

    data class Error(
        val message: String,
        val throwable: Throwable? = null,
        val hasCachedData: Boolean = false
    ) : SyncResult()

    val isSuccess: Boolean get() = this is Success
    val isCached: Boolean get() = this is Cached
    val isError: Boolean get() = this is Error
}
