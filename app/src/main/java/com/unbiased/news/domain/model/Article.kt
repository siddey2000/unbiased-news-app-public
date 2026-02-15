package com.unbiased.news.domain.model

data class Article(
    val id: String,
    val title: String,
    val description: String?,
    val link: String,
    val imageUrl: String?,
    val pubDate: Long,
    val sourceName: String,
    val sourceId: String,
    val relevanceScore: Float = 0f,
    val topicId: String
)
