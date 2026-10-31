package com.studypulse.sync.ui.focus

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun FocusTimerScreen(
    slotId: Long,
    onExit: () -> Unit,
    viewModel: FocusViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(slotId) {
        viewModel.start(slotId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(onClick = onExit) {
            Text("Назад")
        }

        if (state.isLoading) {
            CircularProgressIndicator()
            Text("Загружаем слот...")
            return@Column
        }

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Фокус-таймер",
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = state.taskTitle.ifBlank { "Учебный слот" },
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = formatSeconds(state.remainingSeconds),
                    style = MaterialTheme.typography.displaySmall,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = if (state.isFinished) {
                        "Время вышло"
                    } else if (state.isRunning) {
                        "Идёт фокус"
                    } else {
                        "Готов к старту"
                    },
                    color = if (state.isFinished) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = viewModel::toggle,
                        enabled = state.totalSeconds > 0 && !state.isFinished
                    ) {
                        Text(if (state.isRunning) "Пауза" else "Старт")
                    }

                    OutlinedButton(
                        onClick = viewModel::reset,
                        enabled = state.totalSeconds > 0
                    ) {
                        Text("Сброс")
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = viewModel::completeSlot,
                    enabled = !state.isCompleting
                ) {
                    Text("Отметить выполненным")
                }

                if (state.isCompleting) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        Text("Сохраняем...")
                    }
                }

                state.message?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

private fun formatSeconds(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60

    return "%02d:%02d".format(minutes, seconds)
}