package com.unbiased.news.di

import android.content.Context
import com.unbiased.news.data.remote.GeminiApi
import com.unbiased.news.data.remote.OpenRouterApi
import com.unbiased.news.data.remote.RssParser
import com.unbiased.news.data.repository.AssetLoader
import com.unbiased.news.data.repository.NewsRepositoryImpl
import com.unbiased.news.data.repository.UserSettingsRepositoryImpl
import com.unbiased.news.domain.librarian.LibrarianEngine
import com.unbiased.news.domain.repository.NewsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideAssetLoader(
        @ApplicationContext context: Context
    ): AssetLoader = AssetLoader(context)

    @Provides
    @Singleton
    fun provideLibrarianEngine(
        openRouterApi: OpenRouterApi,
        geminiApi: GeminiApi,
        okHttpClient: OkHttpClient
    ): LibrarianEngine = LibrarianEngine(openRouterApi, geminiApi, okHttpClient)

    @Provides
    @Singleton
    fun provideRssParser(
        okHttpClient: OkHttpClient
    ): RssParser = RssParser(okHttpClient)

    @Provides
    @Singleton
    fun provideNewsRepository(
        assetLoader: AssetLoader,
        librarianEngine: LibrarianEngine,
        articleDao: com.unbiased.news.data.local.ArticleDao,
        sourceDao: com.unbiased.news.data.local.SourceDao
    ): NewsRepository = NewsRepositoryImpl(articleDao, sourceDao, assetLoader, librarianEngine)

    @Provides
    @Singleton
    fun provideUserSettingsRepository(
        @ApplicationContext context: Context
    ): UserSettingsRepositoryImpl = UserSettingsRepositoryImpl(context)
}
