package com.unbiased.news.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface OpenRouterApi {

    @POST("chat/completions")
    suspend fun analyzeHeadlines(
        @Header("Authorization") auth: String,
        @Body request: OpenRouterRequest
    ): Response<OpenRouterResponse>
}

data class OpenRouterRequest(
    val model: String = "openai/gpt-4o-mini",
    val messages: List<Message>,
    val response_format: ResponseFormat? = ResponseFormat(type = "json_object")
)

data class Message(
    val role: String,
    val content: String
)

data class ResponseFormat(
    val type: String = "json_object"
)

data class OpenRouterResponse(
    val id: String,
    val choices: List<Choice>,
    val usage: Usage?
)

data class Choice(
    val index: Int,
    val message: Message,
    val finish_reason: String
)

data class Usage(
    val prompt_tokens: Int,
    val completion_tokens: Int,
    val total_tokens: Int
)
