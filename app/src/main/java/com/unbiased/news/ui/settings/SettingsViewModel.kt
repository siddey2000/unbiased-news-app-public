package com.unbiased.news.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unbiased.news.data.repository.UserSettingsRepositoryImpl
import com.unbiased.news.domain.librarian.LibrarianEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userSettings: UserSettingsRepositoryImpl,
    private val librarianEngine: LibrarianEngine
) : ViewModel() {

    private val _apiKey = MutableStateFlow(userSettings.getStoredApiKey() ?: "")
    val apiKey = _apiKey.asStateFlow()

    private val _selectedProvider = MutableStateFlow(userSettings.getStoredLlmProvider())
    val selectedProvider = _selectedProvider.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving = _isSaving.asStateFlow()

    private val _saveSuccess = MutableStateFlow<Boolean?>(null)
    val saveSuccess = _saveSuccess.asStateFlow()

    private val _validationError = MutableStateFlow<String?>(null)
    val validationError = _validationError.asStateFlow()

    private val _isValidating = MutableStateFlow(false)
    val isValidating = _isValidating.asStateFlow()

    private val _testConnectionResult = MutableStateFlow<TestConnectionResult?>(null)
    val testConnectionResult = _testConnectionResult.asStateFlow()

    /**
     * Validates the API key format before saving.
     */
    fun validateApiKey(key: String): Boolean {
        _validationError.value = null

        if (key.isBlank()) {
            _validationError.value = "API key cannot be empty"
            return false
        }

        val provider = when (_selectedProvider.value) {
            "gemini" -> LibrarianEngine.LlmProvider.GEMINI
            else -> LibrarianEngine.LlmProvider.OPENROUTER
        }

        if (!librarianEngine.validateApiKey(key.trim(), provider)) {
            _validationError.value = when (provider) {
                LibrarianEngine.LlmProvider.OPENROUTER ->
                    "Invalid OpenRouter key format. Should start with 'sk-' or 'sk-or-'"
                LibrarianEngine.LlmProvider.GEMINI ->
                    "Invalid Gemini key format. Key should be at least 20 characters"
            }
            return false
        }

        return true
    }

    fun selectProvider(provider: String) {
        userSettings.saveLlmProvider(provider)
        _selectedProvider.value = provider
        _validationError.value = null
        _testConnectionResult.value = null
    }

    fun saveApiKey(key: String) {
        viewModelScope.launch {
            // Validate first
            if (!validateApiKey(key)) {
                return@launch
            }

            _isSaving.value = true
            _saveSuccess.value = null

            try {
                if (key.isNotBlank()) {
                    userSettings.saveApiKey(key.trim())
                } else {
                    userSettings.clearApiKey()
                }
                _apiKey.value = key
                _saveSuccess.value = true
            } catch (e: Exception) {
                _saveSuccess.value = false
            } finally {
                _isSaving.value = false
            }
        }
    }

    /**
     * Tests the API connection by making a simple request.
     */
    fun testConnection() {
        viewModelScope.launch {
            val key = _apiKey.value
            if (key.isBlank()) {
                _testConnectionResult.value = TestConnectionResult.Error("No API key configured")
                return@launch
            }

            if (!validateApiKey(key)) {
                return@launch
            }

            _isValidating.value = true
            _testConnectionResult.value = null

            val provider = when (_selectedProvider.value) {
                "gemini" -> LibrarianEngine.LlmProvider.GEMINI
                else -> LibrarianEngine.LlmProvider.OPENROUTER
            }

            // Test with a simple relevance analysis
            val result = librarianEngine.analyzeRelevance(
                articles = emptyList(),
                topic = "test",
                apiKey = key.trim(),
                provider = provider
            )

            // Empty input returns empty list, which means connection works
            _testConnectionResult.value = if (result.isEmpty()) {
                TestConnectionResult.Success
            } else {
                TestConnectionResult.Error("Unexpected response from API")
            }

            _isValidating.value = false
        }
    }

    fun clearMessages() {
        _saveSuccess.value = null
        _validationError.value = null
        _testConnectionResult.value = null
    }
}

sealed class TestConnectionResult {
    data object Success : TestConnectionResult()
    data class Error(val message: String) : TestConnectionResult()
}
