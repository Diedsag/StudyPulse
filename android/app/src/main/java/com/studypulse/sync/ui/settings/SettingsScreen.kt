package com.studypulse.sync.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("StudyPulse Settings") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Connection",
                        style = MaterialTheme.typography.titleMedium
                    )

                    OutlinedTextField(
                        value = state.serverUrl,
                        onValueChange = viewModel::onServerUrlChange,
                        label = { Text("Server URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = state.nickname,
                        onValueChange = viewModel::onNicknameChange,
                        label = { Text("Nickname") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "Device UID: " + state.deviceUid.ifBlank { "not generated" },
                        style = MaterialTheme.typography.bodyMedium
                    )

                    if (state.isDirty) {
                        Text(
                            text = "Есть несохранённые изменения",
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(onClick = viewModel::saveSettings) {
                            Text("Save")
                        }

                        Button(onClick = viewModel::generateDeviceUid) {
                            Text("Generate UID")
                        }

                        Button(onClick = viewModel::checkConnection) {
                            Text("Check")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    when (val status = state.status) {
                        is ConnectionStatus.Idle -> {
                            Text("Status: idle")
                        }

                        is ConnectionStatus.Loading -> {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp))
                                Text("Checking connection...")
                            }
                        }

                        is ConnectionStatus.Connected -> {
                            Text(
                                text = "Connected. Server time: ${status.time}",
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        is ConnectionStatus.Error -> {
                            Text(
                                text = "Error: ${status.message}",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Hint",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = "Для Android emulator обычно используйте: http://10.0.2.2:5000"
                    )

                    Text(
                        text = "Для физического устройства используйте IP компьютера, например: http://192.168.1.77:5000"
                    )
                }
            }
        }
    }
}