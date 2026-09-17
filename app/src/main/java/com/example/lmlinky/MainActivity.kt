package com.example.lmlinky

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
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

                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                    onResult = { /* Permission result handled */ }
                )

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val hasPermission = ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED

                        if (!hasPermission) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

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
