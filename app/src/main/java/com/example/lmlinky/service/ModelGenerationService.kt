package com.example.lmlinky.service

import android.R
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.lmlinky.MainActivity
import com.example.lmlinky.data.ChatCompletionRequest
import com.example.lmlinky.data.ChatMessage
import com.example.lmlinky.data.LmStudioApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

sealed interface GenerationEvent {
    data class Token(val token: String, val assistantId: String) : GenerationEvent
    data class Error(val errorMessage: String, val assistantId: String) : GenerationEvent
    data class Finished(val assistantId: String) : GenerationEvent
}

class ModelGenerationService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val apiClient = LmStudioApiClient()
    private val json = Json { ignoreUnknownKeys = true }

    private var streamingJob: Job? = null
    private var currentAssistantId: String? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_GENERATION -> {
                val serverUrl = intent.getStringExtra(EXTRA_SERVER_URL) ?: ""
                val apiKey = intent.getStringExtra(EXTRA_API_KEY)
                val model = intent.getStringExtra(EXTRA_MODEL) ?: ""
                val systemPrompt = intent.getStringExtra(EXTRA_SYSTEM_PROMPT) ?: ""
                val temperature = intent.getFloatExtra(EXTRA_TEMPERATURE, 0.7f)
                val assistantId = intent.getStringExtra(EXTRA_ASSISTANT_ID) ?: ""
                val rawMessagesJson = intent.getStringExtra(EXTRA_MESSAGES_JSON) ?: "[]"

                val messages = runCatching {
                    json.decodeFromString<List<ChatMessage>>(rawMessagesJson)
                }.getOrElse { emptyList() }

                startGeneration(
                    serverUrl = serverUrl,
                    apiKey = apiKey,
                    model = model,
                    systemPrompt = systemPrompt,
                    temperature = temperature,
                    messages = messages,
                    assistantId = assistantId
                )
            }
            ACTION_STOP_GENERATION -> {
                stopGeneration()
            }
        }
        return START_NOT_STICKY
    }

    private fun startGeneration(
        serverUrl: String,
        apiKey: String?,
        model: String,
        systemPrompt: String,
        temperature: Float,
        messages: List<ChatMessage>,
        assistantId: String
    ) {
        streamingJob?.cancel()
        currentAssistantId = assistantId

        startForegroundServiceNotification(model)

        _isGenerating.value = true

        val apiMessages = mutableListOf<ChatMessage>()
        if (systemPrompt.isNotBlank()) {
            apiMessages.add(ChatMessage(role = "system", content = systemPrompt))
        }
        apiMessages.addAll(messages)

        val request = ChatCompletionRequest(
            model = model,
            messages = apiMessages,
            temperature = temperature,
            stream = true
        )

        streamingJob = serviceScope.launch {
            apiClient.streamChatCompletion(serverUrl, request, apiKey)
                .catch { error ->
                    val errorMsg = error.localizedMessage ?: "Connection error"
                    _events.emit(GenerationEvent.Error(errorMsg, assistantId))
                    finishGeneration()
                }
                .collect { token ->
                    _events.emit(GenerationEvent.Token(token, assistantId))
                }

            _events.emit(GenerationEvent.Finished(assistantId))
            finishGeneration()
        }
    }

    private fun stopGeneration() {
        streamingJob?.cancel()
        streamingJob = null

        currentAssistantId?.let { id ->
            serviceScope.launch {
                _events.emit(GenerationEvent.Finished(id))
            }
        }

        finishGeneration()
    }

    private fun finishGeneration() {
        _isGenerating.value = false
        currentAssistantId = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun startForegroundServiceNotification(modelName: String) {
        val notification = buildNotification(modelName)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(modelName: String): Notification {
        val activityIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, ModelGenerationService::class.java).apply {
            action = ACTION_STOP_GENERATION
        }

        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val displayModel = modelName.ifBlank { "LM Studio" }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("LmLinky - Generating Response")
            .setContentText("Waiting for model ($displayModel)...")
            .setSmallIcon(R.drawable.stat_notify_chat)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(contentPendingIntent)
            .addAction(
                R.drawable.ic_menu_close_clear_cancel,
                "Stop",
                stopPendingIntent
            )
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Model Generation",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows ongoing notification banner when generating AI responses"
            }

            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        streamingJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "model_generation_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START_GENERATION = "com.example.lmlinky.action.START_GENERATION"
        const val ACTION_STOP_GENERATION = "com.example.lmlinky.action.STOP_GENERATION"

        const val EXTRA_SERVER_URL = "extra_server_url"
        const val EXTRA_API_KEY = "extra_api_key"
        const val EXTRA_MODEL = "extra_model"
        const val EXTRA_SYSTEM_PROMPT = "extra_system_prompt"
        const val EXTRA_TEMPERATURE = "extra_temperature"
        const val EXTRA_ASSISTANT_ID = "extra_assistant_id"
        const val EXTRA_MESSAGES_JSON = "extra_messages_json"

        private val _events = MutableSharedFlow<GenerationEvent>(extraBufferCapacity = 1000)
        val events: SharedFlow<GenerationEvent> = _events.asSharedFlow()

        private val _isGenerating = MutableStateFlow(false)
        val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

        fun start(
            context: Context,
            serverUrl: String,
            apiKey: String?,
            model: String,
            systemPrompt: String,
            temperature: Float,
            messages: List<ChatMessage>,
            assistantId: String
        ) {
            val intent = Intent(context, ModelGenerationService::class.java).apply {
                action = ACTION_START_GENERATION
                putExtra(EXTRA_SERVER_URL, serverUrl)
                putExtra(EXTRA_API_KEY, apiKey)
                putExtra(EXTRA_MODEL, model)
                putExtra(EXTRA_SYSTEM_PROMPT, systemPrompt)
                putExtra(EXTRA_TEMPERATURE, temperature)
                putExtra(EXTRA_ASSISTANT_ID, assistantId)
                putExtra(EXTRA_MESSAGES_JSON, Json.encodeToString(messages))
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, ModelGenerationService::class.java).apply {
                action = ACTION_STOP_GENERATION
            }
            context.startService(intent)
        }
    }
}
