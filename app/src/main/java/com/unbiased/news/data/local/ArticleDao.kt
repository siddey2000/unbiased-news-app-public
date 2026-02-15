package com.unbiased.news.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.unbiased.news.data.model.ArticleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ArticleDao {

    @Query("SELECT * FROM articles WHERE topicId = :topicId ORDER BY relevanceScore DESC, pubDate DESC")
    fun getArticlesByTopic(topicId: String): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles ORDER BY pubDate DESC LIMIT :limit")
    fun getRecentArticles(limit: Int = 50): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE id = :id")
    suspend fun getArticleById(id: String): ArticleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticles(articles: List<ArticleEntity>)

    @Query("DELETE FROM articles WHERE topicId = :topicId")
    suspend fun deleteArticlesByTopic(topicId: String)

    @Query("DELETE FROM articles WHERE createdAt < :timestamp")
    suspend fun deleteOldArticles(timestamp: Long)

    @Transaction
    suspend fun replaceArticlesForTopic(topicId: String, articles: List<ArticleEntity>) {
        deleteArticlesByTopic(topicId)
        insertArticles(articles)
    }

    @Query("SELECT COUNT(*) FROM articles WHERE topicId = :topicId")
    suspend fun getArticleCountByTopic(topicId: String): Int
}
