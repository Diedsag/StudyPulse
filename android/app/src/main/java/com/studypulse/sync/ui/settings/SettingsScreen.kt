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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.studypulse.sync.ui.sync.SyncViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    syncViewModel: SyncViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val syncState by syncViewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
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
                    text = "Подключение",
                    style = MaterialTheme.typography.titleMedium
                )

                OutlinedTextField(
                    value = state.serverUrl,
                    onValueChange = viewModel::onServerUrlChange,
                    label = { Text("Адрес сервера") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = state.nickname,
                    onValueChange = viewModel::onNicknameChange,
                    label = { Text("Никнейм") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Идентификатор устройства: " + state.deviceUid.ifBlank { "не создан" },
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
                        Text("Сохранить")
                    }

                    Button(onClick = viewModel::generateDeviceUid) {
                        Text("Создать UID")
                    }

                    Button(onClick = viewModel::checkConnection) {
                        Text("Проверить")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                when (val status = state.status) {
                    is ConnectionStatus.Idle -> {
                        Text("Статус: ожидание")
                    }

                    is ConnectionStatus.Loading -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp))
                            Text("Проверяем подключение...")
                        }
                    }

                    is ConnectionStatus.Connected -> {
                        Text(
                            text = "Подключено. Время сервера: ${status.time}",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    is ConnectionStatus.Error -> {
                        Text(
                            text = "Ошибка: ${status.message}",
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
                    text = "Синхронизация",
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = "Очередь событий: ${syncState.pendingCount}",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = "Последняя синхронизация: ${formatTimestamp(syncState.lastSyncAt)}",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = syncViewModel::sync,
                        enabled = !syncState.isSyncing
                    ) {
                        Text("Синхронизировать")
                    }

                    if (syncState.isSyncing) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp))
                    }
                }

                syncState.message?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Text(
                    text = "XP: ${syncState.totalXp}, недельный XP: ${syncState.weeklyXp}, " +
                        "уровень: ${syncState.level}, streak: ${syncState.streakDays}",
                    style = MaterialTheme.typography.bodySmall
                )
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
                    text = "Подсказка",
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

private fun formatTimestamp(value: Long?): String {
    if (value == null) return "—"

    return try {
        Instant
            .ofEpochMilli(value)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
            .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
    } catch (_: Throwable) {
        "—"
    }
}