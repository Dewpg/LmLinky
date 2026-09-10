package com.example.lmlinky.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ModelListResponse(
    val data: List<ModelData> = emptyList()
)

@Serializable
data class ModelData(
    val id: String,
    val objectType: String? = null,
    val ownedBy: String? = null
)

@Serializable
data class ChatMessage(
    val role: String,
    val content: String
)

@Serializable
data class ServerConnection(
    val id: String,
    val name: String,
    val url: String,
    val apiKey: String = ""
)

@Serializable
data class ChatSession(
    val id: String,
    val title: String = "New Chat",
    val timestamp: Long = System.currentTimeMillis(),
    val messages: List<ChatMessage> = emptyList()
)

@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Float? = null,
    val stream: Boolean = true
)

@Serializable
data class ChatCompletionChunk(
    val id: String? = null,
    val choices: List<ChunkChoice> = emptyList()
)

@Serializable
data class ChunkChoice(
    val delta: ChunkDelta? = null,
    @SerialName("finish_reason")
    val finishReason: String? = null
)

@Serializable
data class ChunkDelta(
    val role: String? = null,
    val content: String? = null
)
