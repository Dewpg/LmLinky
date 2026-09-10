package com.example.lmlinky.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lmlinky.data.AppPreferences
import com.example.lmlinky.data.ChatCompletionRequest
import com.example.lmlinky.data.ChatMessage
import com.example.lmlinky.data.ChatSession
import com.example.lmlinky.data.LmStudioApiClient
import com.example.lmlinky.data.ModelData
import com.example.lmlinky.data.ServerConnection
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class ChatMessageUI(
    val id: String = UUID.randomUUID().toString(),
    val role: String,
    val content: String,
    val isStreaming: Boolean = false,
    val isError: Boolean = false
)

enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

data class ChatUiState(
    val serverUrl: String = AppPreferences.DEFAULT_SERVER_URL,
    val apiKey: String = "",
    val connectionStatus: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    val connectionErrorMessage: String? = null,
    val availableModels: List<ModelData> = emptyList(),
    val selectedModel: String = "",
    val systemPrompt: String = AppPreferences.DEFAULT_SYSTEM_PROMPT,
    val temperature: Float = AppPreferences.DEFAULT_TEMPERATURE,
    val connections: List<ServerConnection> = emptyList(),
    val activeConnectionId: String = "",
    val sessions: List<ChatSession> = emptyList(),
    val activeSessionId: String = "",
    val messages: List<ChatMessageUI> = emptyList(),
    val inputText: String = "",
    val isGenerating: Boolean = false,
    val showSettingsDialog: Boolean = false,
    val showHistoryDialog: Boolean = false,
    val isTestingConnection: Boolean = false,
    val testConnectionMessage: String? = null
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = AppPreferences(application)
    private val apiClient = LmStudioApiClient()

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var streamJob: Job? = null

    init {
        loadSessionsAndSettings()
    }

    private fun loadSessionsAndSettings() {
        // Load server connections
        var storedConnections = prefs.getServerConnections()
        var connActiveId = prefs.activeConnectionId

        if (storedConnections.isEmpty() && prefs.serverUrl.isNotBlank()) {
            val initialConn = ServerConnection(
                id = UUID.randomUUID().toString(),
                name = "Default Server",
                url = prefs.serverUrl,
                apiKey = prefs.apiKey
            )
            storedConnections = listOf(initialConn)
            connActiveId = initialConn.id
            prefs.saveServerConnections(storedConnections)
            prefs.activeConnectionId = connActiveId
        } else if (storedConnections.isNotEmpty() && (connActiveId.isEmpty() || storedConnections.none { it.id == connActiveId })) {
            connActiveId = storedConnections.first().id
            prefs.activeConnectionId = connActiveId
        }

        val activeConn = storedConnections.firstOrNull { it.id == connActiveId }
        val currentUrl = activeConn?.url ?: prefs.serverUrl
        val currentApiKey = activeConn?.apiKey ?: prefs.apiKey

        // Load sessions
        var storedSessions = prefs.getChatSessions()
        var activeId = prefs.activeSessionId

        if (storedSessions.isEmpty()) {
            val newSession = ChatSession(id = UUID.randomUUID().toString(), title = "New Chat")
            storedSessions = listOf(newSession)
            activeId = newSession.id
        } else if (activeId.isEmpty() || storedSessions.none { it.id == activeId }) {
            activeId = storedSessions.first().id
        }

        prefs.saveChatSessions(storedSessions)
        prefs.activeSessionId = activeId

        val activeSession = storedSessions.firstOrNull { it.id == activeId }
        val currentMsgs = activeSession?.messages?.map { msg ->
            ChatMessageUI(role = msg.role, content = msg.content)
        } ?: emptyList()

        _uiState.update { state ->
            state.copy(
                serverUrl = currentUrl,
                apiKey = currentApiKey,
                connections = storedConnections,
                activeConnectionId = connActiveId,
                selectedModel = prefs.selectedModel,
                systemPrompt = prefs.systemPrompt,
                temperature = prefs.temperature,
                sessions = storedSessions,
                activeSessionId = activeId,
                messages = currentMsgs
            )
        }

        if (currentUrl.isNotBlank()) {
            refreshModels()
        }
    }

    fun onInputTextChange(newText: String) {
        _uiState.update { it.copy(inputText = newText) }
    }

    fun toggleSettingsDialog(show: Boolean) {
        _uiState.update {
            it.copy(
                showSettingsDialog = show,
                testConnectionMessage = null
            )
        }
    }

    fun toggleHistoryDialog(show: Boolean) {
        _uiState.update { it.copy(showHistoryDialog = show) }
    }

    fun selectModel(modelId: String) {
        prefs.selectedModel = modelId
        _uiState.update { it.copy(selectedModel = modelId) }
    }

    fun selectConnectionProfile(connectionId: String) {
        val targetConn = _uiState.value.connections.firstOrNull { it.id == connectionId } ?: return
        prefs.activeConnectionId = connectionId
        prefs.serverUrl = targetConn.url
        prefs.apiKey = targetConn.apiKey

        _uiState.update {
            it.copy(
                activeConnectionId = connectionId,
                serverUrl = targetConn.url,
                apiKey = targetConn.apiKey
            )
        }

        refreshModels()
    }

    fun deleteConnectionProfile(connectionId: String) {
        val remainingConns = _uiState.value.connections.filter { it.id != connectionId }
        prefs.saveServerConnections(remainingConns)

        if (remainingConns.isEmpty()) {
            prefs.activeConnectionId = ""
            prefs.serverUrl = ""
            prefs.apiKey = ""

            _uiState.update {
                it.copy(
                    connections = emptyList(),
                    activeConnectionId = "",
                    serverUrl = "",
                    apiKey = "",
                    availableModels = emptyList(),
                    selectedModel = "",
                    connectionStatus = ConnectionStatus.DISCONNECTED,
                    connectionErrorMessage = "No server connection profiles available."
                )
            }
            return
        }

        val nextActiveId = if (connectionId == _uiState.value.activeConnectionId) {
            remainingConns.first().id
        } else {
            _uiState.value.activeConnectionId
        }

        prefs.activeConnectionId = nextActiveId
        val activeConn = remainingConns.firstOrNull { it.id == nextActiveId }

        if (activeConn != null) {
            prefs.serverUrl = activeConn.url
            prefs.apiKey = activeConn.apiKey
        }

        _uiState.update {
            it.copy(
                connections = remainingConns,
                activeConnectionId = nextActiveId,
                serverUrl = activeConn?.url ?: "",
                apiKey = activeConn?.apiKey ?: ""
            )
        }

        refreshModels()
    }

    fun saveConnectionProfile(
        id: String?,
        name: String,
        url: String,
        apiKey: String,
        systemPrompt: String,
        temperature: Float
    ) {
        val cleanUrl = if (url.isNotBlank()) apiClient.cleanBaseUrl(url) else ""
        val connName = name.ifBlank { "LM Studio Server" }

        val updatedConns: List<ServerConnection>
        val targetId: String

        if (id != null && _uiState.value.connections.any { it.id == id }) {
            targetId = id
            updatedConns = _uiState.value.connections.map { conn ->
                if (conn.id == id) {
                    conn.copy(name = connName, url = cleanUrl, apiKey = apiKey)
                } else conn
            }
        } else {
            val newConn = ServerConnection(
                id = UUID.randomUUID().toString(),
                name = connName,
                url = cleanUrl,
                apiKey = apiKey
            )
            targetId = newConn.id
            updatedConns = _uiState.value.connections + newConn
        }

        prefs.saveServerConnections(updatedConns)
        prefs.activeConnectionId = targetId
        prefs.serverUrl = cleanUrl
        prefs.apiKey = apiKey
        prefs.systemPrompt = systemPrompt
        prefs.temperature = temperature

        _uiState.update {
            it.copy(
                connections = updatedConns,
                activeConnectionId = targetId,
                serverUrl = cleanUrl,
                apiKey = apiKey,
                systemPrompt = systemPrompt,
                temperature = temperature,
                showSettingsDialog = false
            )
        }

        refreshModels()
    }

    fun createNewSession() {
        stopGeneration()
        val newSession = ChatSession(id = UUID.randomUUID().toString(), title = "New Chat")
        val updatedSessions = listOf(newSession) + _uiState.value.sessions

        prefs.saveChatSessions(updatedSessions)
        prefs.activeSessionId = newSession.id

        _uiState.update {
            it.copy(
                sessions = updatedSessions,
                activeSessionId = newSession.id,
                messages = emptyList(),
                showHistoryDialog = false
            )
        }
    }

    fun selectSession(sessionId: String) {
        if (sessionId == _uiState.value.activeSessionId) {
            _uiState.update { it.copy(showHistoryDialog = false) }
            return
        }

        stopGeneration()
        val targetSession = _uiState.value.sessions.firstOrNull { it.id == sessionId } ?: return
        val sessionMsgs = targetSession.messages.map { ChatMessageUI(role = it.role, content = it.content) }

        prefs.activeSessionId = sessionId

        _uiState.update {
            it.copy(
                activeSessionId = sessionId,
                messages = sessionMsgs,
                showHistoryDialog = false
            )
        }
    }

    fun deleteSession(sessionId: String) {
        stopGeneration()
        val remainingSessions = _uiState.value.sessions.filter { it.id != sessionId }

        if (remainingSessions.isEmpty()) {
            val freshSession = ChatSession(id = UUID.randomUUID().toString(), title = "New Chat")
            val newSessions = listOf(freshSession)
            prefs.saveChatSessions(newSessions)
            prefs.activeSessionId = freshSession.id

            _uiState.update {
                it.copy(
                    sessions = newSessions,
                    activeSessionId = freshSession.id,
                    messages = emptyList()
                )
            }
            return
        }

        val nextActiveId = if (sessionId == _uiState.value.activeSessionId) {
            remainingSessions.first().id
        } else {
            _uiState.value.activeSessionId
        }

        prefs.saveChatSessions(remainingSessions)
        prefs.activeSessionId = nextActiveId

        val activeSession = remainingSessions.firstOrNull { it.id == nextActiveId }
        val sessionMsgs = activeSession?.messages?.map { ChatMessageUI(role = it.role, content = it.content) } ?: emptyList()

        _uiState.update {
            it.copy(
                sessions = remainingSessions,
                activeSessionId = nextActiveId,
                messages = sessionMsgs
            )
        }
    }

    fun refreshModels() {
        val rawUrl = _uiState.value.serverUrl.trim()
        if (rawUrl.isBlank()) {
            _uiState.update {
                it.copy(
                    connectionStatus = ConnectionStatus.DISCONNECTED,
                    connectionErrorMessage = "Server URL not configured. Tap Settings to set your LM Studio server URL."
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    connectionStatus = ConnectionStatus.CONNECTING,
                    connectionErrorMessage = null
                )
            }

            val result = apiClient.fetchModels(rawUrl, _uiState.value.apiKey)
            result.onSuccess { models ->
                val currentSelected = _uiState.value.selectedModel
                val updatedSelected = when {
                    models.any { it.id == currentSelected } -> currentSelected
                    models.isNotEmpty() -> models.first().id
                    else -> ""
                }

                if (updatedSelected != currentSelected) {
                    prefs.selectedModel = updatedSelected
                }

                _uiState.update {
                    it.copy(
                        connectionStatus = ConnectionStatus.CONNECTED,
                        availableModels = models,
                        selectedModel = updatedSelected,
                        connectionErrorMessage = null
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        connectionStatus = ConnectionStatus.ERROR,
                        connectionErrorMessage = error.localizedMessage ?: "Failed to connect to LM Studio server"
                    )
                }
            }
        }
    }

    fun testServerConnection(testUrl: String, testApiKey: String) {
        if (testUrl.isBlank()) {
            _uiState.update {
                it.copy(
                    isTestingConnection = false,
                    testConnectionMessage = "Please enter a valid Server URL."
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isTestingConnection = true, testConnectionMessage = null) }
            val result = apiClient.testConnection(testUrl, testApiKey)
            result.onSuccess {
                _uiState.update {
                    it.copy(
                        isTestingConnection = false,
                        testConnectionMessage = "Successfully connected to LM Studio!"
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isTestingConnection = false,
                        testConnectionMessage = "Connection failed: ${error.localizedMessage}"
                    )
                }
            }
        }
    }

    fun sendMessage() {
        val prompt = _uiState.value.inputText.trim()
        if (prompt.isEmpty() || _uiState.value.isGenerating) return

        if (_uiState.value.serverUrl.isBlank()) {
            toggleSettingsDialog(true)
            return
        }

        val userMessage = ChatMessageUI(role = "user", content = prompt)
        val assistantMessage = ChatMessageUI(role = "assistant", content = "", isStreaming = true)

        val updatedMessages = _uiState.value.messages + userMessage + assistantMessage

        _uiState.update {
            it.copy(
                messages = updatedMessages,
                inputText = "",
                isGenerating = true
            )
        }

        val apiMessages = mutableListOf<ChatMessage>()
        if (_uiState.value.systemPrompt.isNotBlank()) {
            apiMessages.add(ChatMessage(role = "system", content = _uiState.value.systemPrompt))
        }
        apiMessages.addAll(
            _uiState.value.messages.map { ChatMessage(role = it.role, content = it.content) }
        )
        apiMessages.add(ChatMessage(role = "user", content = prompt))

        val request = ChatCompletionRequest(
            model = _uiState.value.selectedModel,
            messages = apiMessages,
            temperature = _uiState.value.temperature,
            stream = true
        )

        val assistantId = assistantMessage.id

        streamJob = viewModelScope.launch {
            apiClient.streamChatCompletion(_uiState.value.serverUrl, request, _uiState.value.apiKey)
                .catch { error ->
                    _uiState.update { currentState ->
                        val newMsgs = currentState.messages.map { msg ->
                            if (msg.id == assistantId) {
                                msg.copy(
                                    content = if (msg.content.isEmpty()) "Error: ${error.localizedMessage}" else msg.content + "\n\n[Error: ${error.localizedMessage}]",
                                    isStreaming = false,
                                    isError = true
                                )
                            } else msg
                        }
                        currentState.copy(messages = newMsgs, isGenerating = false)
                    }
                    persistActiveSession()
                }
                .collect { token ->
                    _uiState.update { currentState ->
                        val newMsgs = currentState.messages.map { msg ->
                            if (msg.id == assistantId) {
                                msg.copy(content = msg.content + token)
                            } else msg
                        }
                        currentState.copy(messages = newMsgs)
                    }
                }

            _uiState.update { currentState ->
                val newMsgs = currentState.messages.map { msg ->
                    if (msg.id == assistantId) {
                        msg.copy(isStreaming = false)
                    } else msg
                }
                currentState.copy(messages = newMsgs, isGenerating = false)
            }
            persistActiveSession()
        }
    }

    fun stopGeneration() {
        streamJob?.cancel()
        streamJob = null

        _uiState.update { currentState ->
            val newMsgs = currentState.messages.map { msg ->
                if (msg.isStreaming) msg.copy(isStreaming = false) else msg
            }
            currentState.copy(messages = newMsgs, isGenerating = false)
        }
        persistActiveSession()
    }

    fun clearChat() {
        stopGeneration()
        _uiState.update { it.copy(messages = emptyList()) }
        persistActiveSession()
    }

    private fun persistActiveSession() {
        val activeId = _uiState.value.activeSessionId
        val currentMsgs = _uiState.value.messages
            .filter { !it.isError }
            .map { ChatMessage(role = it.role, content = it.content) }

        val firstUserMsg = currentMsgs.firstOrNull { it.role == "user" }?.content ?: ""
        val newTitle = if (firstUserMsg.isNotBlank()) {
            if (firstUserMsg.length > 30) firstUserMsg.take(30) + "..." else firstUserMsg
        } else {
            "New Chat"
        }

        val updatedSessions = _uiState.value.sessions.map { session ->
            if (session.id == activeId) {
                session.copy(
                    title = if (session.title == "New Chat") newTitle else session.title,
                    messages = currentMsgs,
                    timestamp = System.currentTimeMillis()
                )
            } else session
        }

        prefs.saveChatSessions(updatedSessions)
        _uiState.update { it.copy(sessions = updatedSessions) }
    }
}
