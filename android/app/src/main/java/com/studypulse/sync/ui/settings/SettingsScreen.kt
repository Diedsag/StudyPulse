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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.studypulse.sync.data.backup.BackupInfo
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
    var devExpanded by remember { mutableStateOf(false) }
    var restoreDialogBackup by remember { mutableStateOf<BackupInfo?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Привет, ${state.displayName.ifBlank { "Студент" }}",
                    style = MaterialTheme.typography.titleMedium
                )

                SwitchRow(
                    title = "Напоминания",
                    subtitle = "О занятиях и дедлайнах",
                    checked = state.profileNotificationsEnabled,
                    onCheckedChange = viewModel::toggleProfileNotifications
                )

                SwitchRow(
                    title = "Автоплан",
                    subtitle = "Предлагать учебные слоты самому",
                    checked = state.autoPlanEnabled,
                    onCheckedChange = viewModel::toggleAutoPlan
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = syncSummary(syncState.isSyncing, syncState.pendingCount, syncState.lastSyncAt),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (state.profileNotificationsEnabled) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = "Типы напоминаний", style = MaterialTheme.typography.titleMedium)

                    SwitchRow(
                        title = "Учебные слоты",
                        subtitle = "За 15 минут до занятия",
                        checked = state.slotRemindersEnabled,
                        onCheckedChange = viewModel::toggleSlotReminders
                    )

                    SwitchRow(
                        title = "Дедлайны",
                        subtitle = "За 24 часа и за 3 часа",
                        checked = state.deadlineRemindersEnabled,
                        onCheckedChange = viewModel::toggleDeadlineReminders
                    )

                    SwitchRow(
                        title = "Утренний дайджест",
                        subtitle = "Сводка плана в 07:00",
                        checked = state.morningDigestEnabled,
                        onCheckedChange = viewModel::toggleMorningDigest
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    SwitchRow(
                        title = "Тихие часы",
                        subtitle = "Не будить ночью",
                        checked = state.quietHoursEnabled,
                        onCheckedChange = viewModel::toggleQuietHours
                    )

                    if (state.quietHoursEnabled) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CounterCell(
                                label = "С",
                                value = "${state.quietHoursStart.toString().padStart(2, '0')}:00",
                                onDecrement = viewModel::decrementQuietStart,
                                onIncrement = viewModel::incrementQuietStart
                            )
                            CounterCell(
                                label = "До",
                                value = "${state.quietHoursEnd.toString().padStart(2, '0')}:00",
                                onDecrement = viewModel::decrementQuietEnd,
                                onIncrement = viewModel::incrementQuietEnd
                            )
                        }
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = "Резервная копия", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Сохрани копию плана, расписания, заданий и материалов на устройстве.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = viewModel::createBackup,
                    enabled = !state.isCreatingBackup && !state.isRestoringBackup,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state.isCreatingBackup) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(if (state.isCreatingBackup) "Создание..." else "Создать резервную копию")
                }

                if (state.backups.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Доступные копии:", style = MaterialTheme.typography.labelMedium)
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.backups) { backup ->
                            BackupItem(
                                backup = backup,
                                onRestore = { restoreDialogBackup = backup },
                                onDelete = { viewModel.deleteBackup(backup.path) }
                            )
                        }
                    }
                }

                state.message?.let { Text(text = it, style = MaterialTheme.typography.bodySmall) }
            }
        }

        if (restoreDialogBackup != null) {
            AlertDialog(
                onDismissRequest = { restoreDialogBackup = null },
                title = { Text("Восстановить копию?") },
                text = { Text("Текущие данные будут заменены данными из копии от ${restoreDialogBackup?.formattedDate()}.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.restoreBackup(restoreDialogBackup!!.path)
                            restoreDialogBackup = null
                        },
                        enabled = !state.isRestoringBackup
                    ) {
                        if (state.isRestoringBackup) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(if (state.isRestoringBackup) "Восстановление..." else "Восстановить")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { restoreDialogBackup = null }) { Text("Отмена") }
                }
            )
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { devExpanded = !devExpanded }) {
                    Text(text = if (devExpanded) "Скрыть для разработчиков" else "Для разработчиков")
                }

                if (devExpanded) {
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
                        text = "UID устройства: ${state.deviceUid.ifBlank { "не задан" }}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Участие в рейтинге", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = state.ratingOptIn, onCheckedChange = viewModel::onRatingOptInChange)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = viewModel::saveSettings) { Text("Сохранить") }
                        OutlinedButton(onClick = viewModel::generateDeviceUid) { Text("Новый UID") }
                        OutlinedButton(onClick = viewModel::checkConnection) { Text("Проверить") }
                    }
                    when (val status = state.status) {
                        is ConnectionStatus.Idle -> Text("Статус: ожидание", style = MaterialTheme.typography.bodySmall)
                        is ConnectionStatus.Loading -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp))
                            Text("Проверяем...")
                        }
                        is ConnectionStatus.Connected -> Text(
                            text = "Подключено. Время сервера: ${status.time}",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall
                        )
                        is ConnectionStatus.Error -> Text(
                            text = "Ошибка: ${status.message}",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (state.isDirty) {
                        Text(text = "Есть несохранённые изменения", color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun CounterCell(
    label: String,
    value: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelMedium)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(onClick = onDecrement) { Text("−") }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.width(64.dp),
                textAlign = TextAlign.Center
            )
            OutlinedButton(onClick = onIncrement) { Text("+") }
        }
    }
}

@Composable
private fun BackupItem(
    backup: BackupInfo,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = backup.formattedDate(), style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = backup.formattedSize(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onRestore) { Text("Восстановить") }
                OutlinedButton(onClick = onDelete) { Text("Удалить") }
            }
        }
    }
}

private fun syncSummary(isSyncing: Boolean, pendingCount: Int, lastSyncAt: Long?): String = when {
    isSyncing -> "Синхронизируем прогресс..."
    pendingCount > 0 -> "Есть несохранённые изменения. Синхронизируем автоматически, когда появится сеть."
    lastSyncAt != null -> "Прогресс синхронизирован (${formatTimestamp(lastSyncAt)})."
    else -> "Прогресс будет синхронизирован автоматически."
}

private fun formatTimestamp(value: Long?): String {
    if (value == null) return "-"
    return try {
        Instant.ofEpochMilli(value).atZone(ZoneId.systemDefault())
            .toLocalDateTime().format(DateTimeFormatter.ofPattern("dd.MM HH:mm"))
    } catch (_: Throwable) {
        "-"
    }
}