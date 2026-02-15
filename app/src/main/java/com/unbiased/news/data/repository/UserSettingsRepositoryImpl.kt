package com.unbiased.news.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserSettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val masterKey: MasterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_user_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val _apiKey = MutableStateFlow(getStoredApiKey())
    val apiKey: StateFlow<String?> = _apiKey.asStateFlow()

    private val _llmProvider = MutableStateFlow(getStoredLlmProvider())
    val llmProvider: StateFlow<String> = _llmProvider.asStateFlow()

    fun getStoredApiKey(): String? {
        return encryptedPrefs.getString(KEY_API_KEY, null)
    }

    fun saveApiKey(key: String) {
        encryptedPrefs.edit().putString(KEY_API_KEY, key).apply()
        _apiKey.value = key
    }

    fun clearApiKey() {
        encryptedPrefs.edit().remove(KEY_API_KEY).apply()
        _apiKey.value = null
    }

    fun getStoredLlmProvider(): String {
        return encryptedPrefs.getString(KEY_LLM_PROVIDER, DEFAULT_PROVIDER) ?: DEFAULT_PROVIDER
    }

    fun saveLlmProvider(provider: String) {
        encryptedPrefs.edit().putString(KEY_LLM_PROVIDER, provider).apply()
        _llmProvider.value = provider
    }

    companion object {
        private const val KEY_API_KEY = "api_key"
        private const val KEY_LLM_PROVIDER = "llm_provider"
        private const val DEFAULT_PROVIDER = "openrouter"
    }
}
