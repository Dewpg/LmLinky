package com.example.lmlinky.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.BufferedReader
import java.util.concurrent.TimeUnit

class LmStudioApiClient {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    fun cleanBaseUrl(rawUrl: String): String {
        var url = rawUrl.trim()
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://$url"
        }
        return url.removeSuffix("/")
    }

    suspend fun testConnection(baseUrl: String, apiKey: String? = null): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val url = "${cleanBaseUrl(baseUrl)}/v1/models"
            val requestBuilder = Request.Builder().url(url).get()
            if (!apiKey.isNullOrBlank()) {
                requestBuilder.header("Authorization", "Bearer ${apiKey.trim()}")
            }

            client.newCall(requestBuilder.build()).execute().use { response ->
                response.isSuccessful
            }
        }
    }

    suspend fun fetchModels(baseUrl: String, apiKey: String? = null): Result<List<ModelData>> = withContext(Dispatchers.IO) {
        runCatching {
            val url = "${cleanBaseUrl(baseUrl)}/v1/models"
            val requestBuilder = Request.Builder().url(url).get()
            if (!apiKey.isNullOrBlank()) {
                requestBuilder.header("Authorization", "Bearer ${apiKey.trim()}")
            }

            client.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) {
                    throw Exception("HTTP Error: ${response.code} ${response.message}")
                }
                val bodyString = response.body?.string() ?: ""
                val modelResponse = json.decodeFromString<ModelListResponse>(bodyString)
                modelResponse.data
            }
        }
    }

    fun streamChatCompletion(
        baseUrl: String,
        completionRequest: ChatCompletionRequest,
        apiKey: String? = null
    ): Flow<String> = flow {
        val url = "${cleanBaseUrl(baseUrl)}/v1/chat/completions"
        val requestBodyJson = json.encodeToString(completionRequest)
        val body = requestBodyJson.toRequestBody("application/json; charset=utf-8".toMediaType())

        val requestBuilder = Request.Builder()
            .url(url)
            .post(body)
            .header("Accept", "text/event-stream")

        if (!apiKey.isNullOrBlank()) {
            requestBuilder.header("Authorization", "Bearer ${apiKey.trim()}")
        }

        val call = client.newCall(requestBuilder.build())
        val response = call.execute()

        if (!response.isSuccessful) {
            val errorBody = response.body?.string() ?: "Unknown error"
            throw Exception("LM Studio returned HTTP ${response.code}: $errorBody")
        }

        val bodyStream = response.body ?: throw Exception("Empty response body from LM Studio")
        val reader = BufferedReader(bodyStream.charStream())

        try {
            var line: String? = reader.readLine()
            while (line != null) {
                val trimmed = line.trim()
                if (trimmed.startsWith("data:")) {
                    val dataPart = trimmed.substring(5).trim()
                    if (dataPart == "[DONE]") {
                        break
                    }
                    if (dataPart.isNotEmpty()) {
                        runCatching {
                            val chunk = json.decodeFromString<ChatCompletionChunk>(dataPart)
                            chunk.choices.firstOrNull()?.delta?.content?.let { content ->
                                if (content.isNotEmpty()) {
                                    emit(content)
                                }
                            }
                        }
                    }
                }
                line = reader.readLine()
            }
        } finally {
            reader.close()
            response.close()
        }
    }.flowOn(Dispatchers.IO)
}
