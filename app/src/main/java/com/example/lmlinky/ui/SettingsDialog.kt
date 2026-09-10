package com.example.lmlinky.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.lmlinky.data.ServerConnection
import kotlin.math.roundToInt

@Composable
fun SettingsDialog(
    serverUrl: String,
    apiKey: String,
    systemPrompt: String,
    temperature: Float,
    connections: List<ServerConnection>,
    activeConnectionId: String,
    isTestingConnection: Boolean,
    testConnectionMessage: String?,
    onDismiss: () -> Unit,
    onTestConnection: (url: String, apiKey: String) -> Unit,
    onSelectConnection: (String) -> Unit,
    onDeleteConnectionProfile: (String) -> Unit,
    onSaveConnectionProfile: (id: String?, name: String, url: String, apiKey: String, prompt: String, temp: Float) -> Unit
) {
    val activeConn = connections.firstOrNull { it.id == activeConnectionId }

    var editingConnectionId by remember { mutableStateOf<String?>(activeConn?.id) }
    var connectionNameState by remember { mutableStateOf(activeConn?.name ?: "LM Studio Server") }
    var urlState by remember { mutableStateOf(serverUrl) }
    var apiKeyState by remember { mutableStateOf(apiKey) }
    var promptState by remember { mutableStateOf(systemPrompt) }
    var tempState by remember { mutableFloatStateOf(temperature) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "LM Studio Settings")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Saved Connections Header with "+ Add New Connection"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Saved Connections",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedButton(
                        onClick = {
                            editingConnectionId = null
                            connectionNameState = "Server ${connections.size + 1}"
                            urlState = ""
                            apiKeyState = ""
                        },
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add New Connection",
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text("Add New")
                    }
                }

                // List of Saved Connections
                if (connections.isNotEmpty()) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        connections.forEach { conn ->
                            val isActive = conn.id == activeConnectionId

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        onSelectConnection(conn.id)
                                        editingConnectionId = conn.id
                                        connectionNameState = conn.name
                                        urlState = conn.url
                                        apiKeyState = conn.apiKey
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(horizontal = 10.dp, vertical = 8.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (isActive) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Active",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier
                                                        .padding(end = 4.dp)
                                                        .height(16.dp)
                                                        .width(16.dp)
                                                )
                                            }
                                            Text(
                                                text = conn.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Text(
                                            text = if (conn.url.isNotBlank()) conn.url else "No URL set",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            onDeleteConnectionProfile(conn.id)
                                            if (editingConnectionId == conn.id) {
                                                editingConnectionId = null
                                                connectionNameState = ""
                                                urlState = ""
                                                apiKeyState = ""
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Connection Profile",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier
                                                .height(20.dp)
                                                .width(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // Connection Details Form Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (editingConnectionId == null) "New Connection Details" else "Edit Connection Details",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    if (editingConnectionId != null) {
                        TextButton(
                            onClick = {
                                onDeleteConnectionProfile(editingConnectionId!!)
                                editingConnectionId = null
                                connectionNameState = ""
                                urlState = ""
                                apiKeyState = ""
                            }
                        ) {
                            Text("Delete Profile", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                // Connection Profile Name
                OutlinedTextField(
                    value = connectionNameState,
                    onValueChange = { connectionNameState = it },
                    label = { Text("Connection Name") },
                    placeholder = { Text("e.g. Home PC / Tailscale") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Server URL input
                OutlinedTextField(
                    value = urlState,
                    onValueChange = { urlState = it },
                    label = { Text("LM Studio Base URL") },
                    placeholder = { Text("http://100.x.y.z:1234") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // API Key input
                OutlinedTextField(
                    value = apiKeyState,
                    onValueChange = { apiKeyState = it },
                    label = { Text("API Key (Optional)") },
                    placeholder = { Text("Leave blank if no key required") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                // Test connection button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = { onTestConnection(urlState, apiKeyState) },
                        enabled = !isTestingConnection && urlState.isNotBlank()
                    ) {
                        if (isTestingConnection) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .height(16.dp)
                                    .width(16.dp),
                                strokeWidth = 2.dp
                            )
                        }
                        Text("Test Connection")
                    }
                }

                testConnectionMessage?.let { msg ->
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (msg.startsWith("Successfully")) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // System Prompt Input
                OutlinedTextField(
                    value = promptState,
                    onValueChange = { promptState = it },
                    label = { Text("System Prompt") },
                    placeholder = { Text("You are a helpful AI assistant.") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )

                // Temperature Slider
                Column {
                    val formattedTemp = (tempState * 100).roundToInt() / 100.0f
                    Text(
                        text = "Temperature: $formattedTemp",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = tempState,
                        onValueChange = { tempState = it },
                        valueRange = 0.0f..1.5f,
                        steps = 29
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveConnectionProfile(
                        editingConnectionId,
                        connectionNameState,
                        urlState,
                        apiKeyState,
                        promptState,
                        tempState
                    )
                }
            ) {
                Text(if (editingConnectionId == null) "Add Connection" else "Update Connection")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
