package com.unbiased.news.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "articles",
    foreignKeys = [
        ForeignKey(
            entity = SourceEntity::class,
            parentColumns = ["sourceId"],
            childColumns = ["sourceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sourceId"), Index("topicId"), Index("pubDate")]
)
data class ArticleEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String?,
    val link: String,
    val imageUrl: String?,
    val pubDate: Long,
    val sourceName: String,
    val sourceId: String,
    val relevanceScore: Float = 0f,
    val topicId: String,
    val createdAt: Long = System.currentTimeMillis()
)
