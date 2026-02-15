package com.unbiased.news.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unbiased.news.data.repository.UserSettingsRepositoryImpl
import com.unbiased.news.domain.librarian.LibrarianEngine
import com.unbiased.news.domain.model.Article
import com.unbiased.news.domain.model.DefaultTopics
import com.unbiased.news.domain.model.Topic
import com.unbiased.news.domain.repository.NewsRepository
import com.unbiased.news.domain.repository.SyncResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val newsRepository: NewsRepository,
    private val userSettings: UserSettingsRepositoryImpl
) : ViewModel() {

    private val _selectedTopic = MutableStateFlow<Topic>(DefaultTopics.TECH)
    val selectedTopic: StateFlow<Topic> = _selectedTopic.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _syncResult = MutableStateFlow<SyncResult?>(null)
    val syncResult: StateFlow<SyncResult?> = _syncResult.asStateFlow()

    val articles: StateFlow<List<Article>> = _selectedTopic
        .flatMapLatest { topic -> newsRepository.getArticlesByTopic(topic.id) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val topics: List<Topic> = DefaultTopics.ALL

    init {
        initializeAndSync()
    }

    private fun initializeAndSync() {
        viewModelScope.launch {
            newsRepository.initializeSourcesIfNeeded()
        }
    }

    fun selectTopic(topic: Topic) {
        _selectedTopic.value = topic
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _syncResult.value = null

            val apiKey = userSettings.getStoredApiKey()
            if (apiKey.isNullOrBlank()) {
                _error.value = "Please configure your API key in Settings"
                _isLoading.value = false
                return@launch
            }

            val provider = when (userSettings.getStoredLlmProvider()) {
                "gemini" -> LibrarianEngine.LlmProvider.GEMINI
                else -> LibrarianEngine.LlmProvider.OPENROUTER
            }

            val result = newsRepository.syncArticlesForTopic(
                topicId = _selectedTopic.value.id,
                apiKey = apiKey,
                provider = provider
            )

            _syncResult.value = result

            when (result) {
                is SyncResult.Error -> {
                    if (!result.hasCachedData) {
                        _error.value = result.message
                    }
                }
                is SyncResult.Cached -> {
                    // Using cached data, no error needed
                }
                is SyncResult.Success -> {
                    // Sync successful
                }
            }

            _isLoading.value = false
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun clearSyncResult() {
        _syncResult.value = null
    }
}
