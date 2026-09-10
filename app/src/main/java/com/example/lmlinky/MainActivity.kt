package com.example.lmlinky

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lmlinky.ui.ChatScreen
import com.example.lmlinky.ui.ChatViewModel
import com.example.lmlinky.ui.theme.LmLinkyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LmLinkyTheme {
                val viewModel: ChatViewModel = viewModel()
                val uiState by viewModel.uiState.collectAsState()

                ChatScreen(
                    uiState = uiState,
                    onInputTextChange = viewModel::onInputTextChange,
                    onSendMessage = viewModel::sendMessage,
                    onStopGeneration = viewModel::stopGeneration,
                    onClearChat = viewModel::clearChat,
                    onRefreshModels = viewModel::refreshModels,
                    onSelectModel = viewModel::selectModel,
                    onToggleSettings = viewModel::toggleSettingsDialog,
                    onToggleHistory = viewModel::toggleHistoryDialog,
                    onNewChat = viewModel::createNewSession,
                    onSelectSession = viewModel::selectSession,
                    onDeleteSession = viewModel::deleteSession,
                    onSelectConnection = viewModel::selectConnectionProfile,
                    onDeleteConnectionProfile = viewModel::deleteConnectionProfile,
                    onTestConnection = viewModel::testServerConnection,
                    onSaveConnectionProfile = viewModel::saveConnectionProfile
                )
            }
        }
    }
}
