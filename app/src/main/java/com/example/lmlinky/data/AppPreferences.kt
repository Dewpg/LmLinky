package com.example.lmlinky.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AppPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("lmlinky_prefs", Context.MODE_PRIVATE)

    private val json = Json { ignoreUnknownKeys = true }

    var serverUrl: String
        get() = prefs.getString(KEY_SERVER_URL, DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL
        set(value) = prefs.edit().putString(KEY_SERVER_URL, value).apply()

    var apiKey: String
        get() = prefs.getString(KEY_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_API_KEY, value).apply()

    var selectedModel: String
        get() = prefs.getString(KEY_SELECTED_MODEL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SELECTED_MODEL, value).apply()

    var systemPrompt: String
        get() = prefs.getString(KEY_SYSTEM_PROMPT, DEFAULT_SYSTEM_PROMPT) ?: DEFAULT_SYSTEM_PROMPT
        set(value) = prefs.edit().putString(KEY_SYSTEM_PROMPT, value).apply()

    var temperature: Float
        get() = prefs.getFloat(KEY_TEMPERATURE, DEFAULT_TEMPERATURE)
        set(value) = prefs.edit().putFloat(KEY_TEMPERATURE, value).apply()

    var activeSessionId: String
        get() = prefs.getString(KEY_ACTIVE_SESSION_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_ACTIVE_SESSION_ID, value).apply()

    var activeConnectionId: String
        get() = prefs.getString(KEY_ACTIVE_CONNECTION_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_ACTIVE_CONNECTION_ID, value).apply()

    fun getServerConnections(): List<ServerConnection> {
        val rawJson = prefs.getString(KEY_SERVER_CONNECTIONS, null) ?: return emptyList()
        return runCatching {
            json.decodeFromString<List<ServerConnection>>(rawJson)
        }.getOrElse { emptyList() }
    }

    fun saveServerConnections(connections: List<ServerConnection>) {
        val rawJson = json.encodeToString(connections)
        prefs.edit().putString(KEY_SERVER_CONNECTIONS, rawJson).apply()
    }

    fun getChatSessions(): List<ChatSession> {
        val rawJson = prefs.getString(KEY_CHAT_SESSIONS, null) ?: return emptyList()
        return runCatching {
            json.decodeFromString<List<ChatSession>>(rawJson)
        }.getOrElse { emptyList() }
    }

    fun saveChatSessions(sessions: List<ChatSession>) {
        val rawJson = json.encodeToString(sessions)
        prefs.edit().putString(KEY_CHAT_SESSIONS, rawJson).apply()
    }

    companion object {
        const val DEFAULT_SERVER_URL = "" // Blank default so users specify their own
        const val DEFAULT_SYSTEM_PROMPT = "You are a helpful AI assistant."
        const val DEFAULT_TEMPERATURE = 0.7f

        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_SELECTED_MODEL = "selected_model"
        private const val KEY_SYSTEM_PROMPT = "system_prompt"
        private const val KEY_TEMPERATURE = "temperature"
        private const val KEY_ACTIVE_SESSION_ID = "active_session_id"
        private const val KEY_ACTIVE_CONNECTION_ID = "active_connection_id"
        private const val KEY_SERVER_CONNECTIONS = "server_connections"
        private const val KEY_CHAT_SESSIONS = "chat_sessions"
    }
}
