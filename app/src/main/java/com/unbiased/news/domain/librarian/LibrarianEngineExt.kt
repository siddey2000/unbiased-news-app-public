package com.unbiased.news.domain.librarian

import com.unbiased.news.domain.model.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/**
 * Fetches all sources with bounded parallelism (max 5 concurrent requests).
 */
suspend fun LibrarianEngine.fetchAllSources(sources: List<Source>): List<LibrarianEngine.RawArticle> = coroutineScope {
    withContext(Dispatchers.IO) {
        sources
            .map { source ->
                async { fetchRssFeed(source) }
            }
            .awaitAll()
            .flatten()
    }
}
