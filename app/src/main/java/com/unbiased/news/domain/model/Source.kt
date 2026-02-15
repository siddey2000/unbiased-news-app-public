package com.unbiased.news.domain.model

data class Source(
    val sourceId: String,
    val name: String,
    val url: String,
    val category: String,
    val region: String = "global",
    val isEnabled: Boolean = true
)
