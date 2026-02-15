package com.unbiased.news.di

import android.content.Context
import androidx.room.Room
import com.unbiased.news.data.local.ArticleDao
import com.unbiased.news.data.local.NewsDatabase
import com.unbiased.news.data.local.SourceDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideNewsDatabase(
        @ApplicationContext context: Context
    ): NewsDatabase = Room.databaseBuilder(
        context,
        NewsDatabase::class.java,
        "unbiased_news_db"
    )
        .fallbackToDestructiveMigration()
        .build()

    @Provides
    fun provideArticleDao(database: NewsDatabase): ArticleDao = database.articleDao()

    @Provides
    fun provideSourceDao(database: NewsDatabase): SourceDao = database.sourceDao()
}
