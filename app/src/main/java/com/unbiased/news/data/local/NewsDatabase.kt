package com.unbiased.news.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.unbiased.news.data.model.ArticleEntity
import com.unbiased.news.data.model.SourceEntity

@Database(
    entities = [
        ArticleEntity::class,
        SourceEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class NewsDatabase : RoomDatabase() {
    abstract fun articleDao(): ArticleDao
    abstract fun sourceDao(): SourceDao
}
