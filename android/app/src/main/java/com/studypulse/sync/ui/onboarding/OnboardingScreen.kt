package com.studypulse.sync.ui.onboarding

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        viewModel.next()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "StudyPulse",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = "Шаг ${state.step + 1} из ${viewModel.totalSteps}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        when (state.step) {
            0 -> WelcomeStep()
            1 -> NameStep(
                displayName = state.displayName,
                onDisplayNameChange = viewModel::onDisplayNameChange
            )
            2 -> StudyPreferencesStep(
                startHour = state.studyStartHour,
                endHour = state.studyEndHour,
                dailyMinutes = state.dailyMinutes,
                onIncrementStart = viewModel::incrementStartHour,
                onDecrementStart = viewModel::decrementStartHour,
                onIncrementEnd = viewModel::incrementEndHour,
                onDecrementEnd = viewModel::decrementEndHour,
                onIncrementDaily = viewModel::incrementDailyMinutes,
                onDecrementDaily = viewModel::decrementDailyMinutes
            )
            3 -> NotificationsStep(
                notificationsEnabled = state.notificationsEnabled,
                autoPlanEnabled = state.autoPlanEnabled,
                onNotificationsChange = viewModel::toggleNotifications,
                onAutoPlanChange = viewModel::toggleAutoPlan
            )
        }

        state.message?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.step > 0) {
                OutlinedButton(
                    onClick = viewModel::back,
                    enabled = !state.isSaving
                ) {
                    Text("Назад")
                }
            }

            Button(
                onClick = {
                    val shouldRequestNotificationPermission =
                        state.step == 3 &&
                            state.notificationsEnabled &&
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED

                    if (shouldRequestNotificationPermission) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        viewModel.next()
                    }
                },
                enabled = !state.isSaving,
                modifier = Modifier.weight(1f)
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Text(
                    text = if (state.step == viewModel.totalSteps - 1) {
                        "Завершить"
                    } else {
                        "Далее"
                    }
                )
            }
        }
    }
}

@Composable
private fun WelcomeStep() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Добро пожаловать",
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text = "StudyPulse поможет планировать учёбу, помнить о дедлайнах и повторять материалы без лишней ручной настройки.",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "Мы быстро настроим:",
                style = MaterialTheme.typography.titleSmall
            )

            Text(text = "• как тебя зовут;", style = MaterialTheme.typography.bodyMedium)
            Text(text = "• когда тебе удобно учиться;", style = MaterialTheme.typography.bodyMedium)
            Text(text = "• включать ли напоминания;", style = MaterialTheme.typography.bodyMedium)
            Text(text = "• строить ли план автоматически.", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun NameStep(
    displayName: String,
    onDisplayNameChange: (String) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Как тебя зовут?",
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text = "Это имя будет использоваться в приветствии и напоминаниях.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = displayName,
                onValueChange = onDisplayNameChange,
                label = { Text("Имя") },
                placeholder = { Text("Например: Иван") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun StudyPreferencesStep(
    startHour: Int,
    endHour: Int,
    dailyMinutes: Int,
    onIncrementStart: () -> Unit,
    onDecrementStart: () -> Unit,
    onIncrementEnd: () -> Unit,
    onDecrementEnd: () -> Unit,
    onIncrementDaily: () -> Unit,
    onDecrementDaily: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Когда ты обычно учишься?",
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text = "Приложение будет учитывать это время при планировании учебных слотов.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            CounterRow(
                label = "Начало учёбы",
                value = "${startHour.toString().padStart(2, '0')}:00",
                onDecrement = onDecrementStart,
                onIncrement = onIncrementStart
            )

            CounterRow(
                label = "Окончание учёбы",
                value = "${endHour.toString().padStart(2, '0')}:00",
                onDecrement = onDecrementEnd,
                onIncrement = onIncrementEnd
            )

            CounterRow(
                label = "Сколько минут в день готов учиться",
                value = "$dailyMinutes мин",
                onDecrement = onDecrementDaily,
                onIncrement = onIncrementDaily
            )
        }
    }
}

@Composable
private fun NotificationsStep(
    notificationsEnabled: Boolean,
    autoPlanEnabled: Boolean,
    onNotificationsChange: (Boolean) -> Unit,
    onAutoPlanChange: (Boolean) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Напоминания и автоматический план",
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text = "Можно включить напоминания о занятиях и дедлайнах, а также позволить приложению самому предлагать план на день.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Уведомления",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Напоминать о слотах и дедлайнах",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = notificationsEnabled,
                    onCheckedChange = onNotificationsChange
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Автоплан",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Предлагать учебные слоты автоматически",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = autoPlanEnabled,
                    onCheckedChange = onAutoPlanChange
                )
            }
        }
    }
}

@Composable
private fun CounterRow(
    label: String,
    value: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(onClick = onDecrement) {
                Text("−")
            }

            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.width(90.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            OutlinedButton(onClick = onIncrement) {
                Text("+")
            }
        }
    }
}