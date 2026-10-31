package com.studypulse.sync.ui.schedule

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.studypulse.sync.data.repository.ClassOccurrence

@Composable
fun ScheduleScreen(
    viewModel: ScheduleViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        state.errorMessage?.let { message ->
            Text(
                text = "Ошибка: $message",
                color = MaterialTheme.colorScheme.error
            )
        }

        if (state.occurrences.isEmpty()) {
            Text(
                text = "Нет занятий. Скачай учебный пакет во вкладке Packages.",
                style = MaterialTheme.typography.bodyLarge
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = state.occurrences,
                key = { occurrence ->
                    "${occurrence.templateExternalId}|${occurrence.date}|${occurrence.startTime}"
                }
            ) { occurrence ->
                OccurrenceCard(
                    occurrence = occurrence,
                    isCancelling = state.cancellingKey ==
                        "${occurrence.templateExternalId}|${occurrence.date}|${occurrence.startTime}",
                    onCancel = {
                        viewModel.cancelOccurrence(occurrence)
                    }
                )
            }
        }
    }
}

@Composable
private fun OccurrenceCard(
    occurrence: ClassOccurrence,
    isCancelling: Boolean,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "${occurrence.displayDate}, ${occurrence.weekday}",
                style = MaterialTheme.typography.labelLarge
            )

            Text(
                text = "${occurrence.startTime}–${occurrence.endTime}",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = occurrence.subject,
                style = MaterialTheme.typography.bodyLarge
            )

            occurrence.lessonType?.let { type ->
                Text(
                    text = "Тип: $type",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            occurrence.room?.let { room ->
                Text(
                    text = "Аудитория: $room",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            occurrence.teacher?.let { teacher ->
                Text(
                    text = "Преподаватель: $teacher",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (occurrence.isCancelled) {
                Text(
                    text = "Статус: отменено",
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onCancel,
                        enabled = !isCancelling
                    ) {
                        Text("Отменить занятие")
                    }

                    if (isCancelling) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}