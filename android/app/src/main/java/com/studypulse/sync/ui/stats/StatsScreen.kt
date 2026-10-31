package com.studypulse.sync.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.studypulse.sync.core.network.dto.LeaderboardEntryDto
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun StatsScreen(
    viewModel: StatsViewModel = viewModel()
) {
    val lifecycleOwner = LocalContext.current as? LifecycleOwner

    LaunchedEffect(lifecycleOwner) {
        if (lifecycleOwner != null) {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.refreshAllPeriods()
            }
        } else {
            viewModel.refreshAllPeriods()
        }
    }

    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { viewModel.selectPeriod("week") },
                    enabled = !state.isLoading
                ) {
                    Text("Неделя")
                }

                OutlinedButton(
                    onClick = { viewModel.selectPeriod("all") },
                    enabled = !state.isLoading
                ) {
                    Text("Всё время")
                }

                Button(
                    onClick = viewModel::refresh,
                    enabled = !state.isLoading
                ) {
                    Text("Обновить")
                }

                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp))
                }
            }
        }

        state.errorMessage?.let { message ->
            item {
                Text(
                    text = "Ошибка: $message",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Мой прогресс",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = "Уровень: ${state.level}",
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Text(
                        text = "Всего XP: ${state.totalXp}",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        text = "Недельный XP: ${state.weeklyXp}",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        text = "Streak: ${state.streakDays} дн.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        text = if (state.ratingOptIn) {
                            "Участие в рейтинге: включено"
                        } else {
                            "Участие в рейтинге: выключено"
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        text = "Обновлено: ${formatTimestamp(state.fetchedAt)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (state.period == "week") {
                    "Недельный рейтинг"
                } else {
                    "Рейтинг за всё время"
                },
                style = MaterialTheme.typography.titleMedium
            )
        }

        if (state.leaderboard.isEmpty()) {
            item {
                Text(
                    text = "Нет данных рейтинга. Данные обновляются автоматически при открытии экрана.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            items(
                items = state.leaderboard,
                key = { "${state.period}-${it.rank}-${it.deviceUid}" }
            ) { entry ->
                LeaderboardRow(entry = entry)
            }
        }
    }
}

@Composable
private fun LeaderboardRow(
    entry: LeaderboardEntryDto
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "#${entry.rank} ${entry.nickname ?: entry.deviceUid}" +
                    if (entry.isCurrentUser) " ← вы" else "",
                style = MaterialTheme.typography.titleSmall
            )

            Text(
                text = "XP: ${entry.score}",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "Уровень: ${entry.level}, streak: ${entry.streakDays}",
                style = MaterialTheme.typography.bodySmall
            )
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