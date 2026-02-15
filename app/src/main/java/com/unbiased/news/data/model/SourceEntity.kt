package com.unbiased.news.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sources")
data class SourceEntity(
    @PrimaryKey
    val sourceId: String,
    val name: String,
    val url: String,
    val category: String,
    val region: String = "global",
    val isEnabled: Boolean = true
)
