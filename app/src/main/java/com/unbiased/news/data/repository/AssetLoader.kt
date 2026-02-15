package com.unbiased.news.data.repository

import android.content.Context
import com.unbiased.news.data.model.SourceEntity
import com.unbiased.news.domain.model.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.BufferedReader

/**
 * Loads sources.json from assets and converts to domain objects.
 */
class AssetLoader(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun loadSources(): List<Source> = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.assets.open("sources.json")
            val content = inputStream.bufferedReader().use(BufferedReader::readText)
            val sourceDtos = json.decodeFromString<List<SourceDto>>(content)

            sourceDtos.map { dto ->
                Source(
                    sourceId = dto.sourceId,
                    name = dto.name,
                    url = dto.url,
                    category = dto.category,
                    region = dto.region ?: "global",
                    isEnabled = dto.isEnabled ?: true
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    @Serializable
    data class SourceDto(
        @SerialName("source_id")
        val sourceId: String,
        val name: String,
        val url: String,
        val category: String,
        val region: String? = null,
        val isEnabled: Boolean? = null
    )
}

// Extension to convert Source to SourceEntity
fun Source.toEntity(): SourceEntity =
    SourceEntity(
        sourceId = sourceId,
        name = name,
        url = url,
        category = category,
        region = region,
        isEnabled = isEnabled
    )

// Extension to convert SourceEntity to Source
fun SourceEntity.toDomainSource(): Source =
    Source(
        sourceId = sourceId,
        name = name,
        url = url,
        category = category,
        region = region,
        isEnabled = isEnabled
    )
