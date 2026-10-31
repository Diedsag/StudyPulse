package com.studypulse.sync.ui.plan

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
import com.studypulse.sync.data.local.dao.SlotWithTask
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun PlanScreen(
    viewModel: PlanViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = viewModel::generatePlan,
                enabled = !state.isGenerating && !state.isAccepting
            ) {
                Text("Сгенерировать план")
            }

            Button(
                onClick = viewModel::acceptPlan,
                enabled = state.slots.isNotEmpty() && !state.isGenerating && !state.isAccepting
            ) {
                Text("Принять план")
            }

            if (state.isGenerating || state.isAccepting) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp))
            }
        }

        state.message?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge
            )
        }

        if (state.warnings.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Внимание",
                        style = MaterialTheme.typography.titleSmall
                    )

                    state.warnings.forEach { warning ->
                        Text(
                            text = warning,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        if (state.slots.isEmpty()) {
            Text(
                text = "Предложенных слотов пока нет. Нажми «Сгенерировать план».",
                style = MaterialTheme.typography.bodyLarge
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = state.slots,
                key = { it.slot.id }
            ) { slotWithTask ->
                SlotCard(slotWithTask = slotWithTask)
            }
        }
    }
}

@Composable
private fun SlotCard(
    slotWithTask: SlotWithTask
) {
    val slot = slotWithTask.slot
    val zone = ZoneId.systemDefault()

    val startDate = Instant.ofEpochMilli(slot.startDateTime).atZone(zone).toLocalDate()
    val startTime = Instant.ofEpochMilli(slot.startDateTime).atZone(zone).toLocalTime()
    val endTime = Instant.ofEpochMilli(slot.endDateTime).atZone(zone).toLocalTime()

    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    val durationMinutes = ((slot.endDateTime - slot.startDateTime) / 60_000L).toInt()

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = startDate.format(dateFormatter),
                style = MaterialTheme.typography.labelLarge
            )

            Text(
                text = slotWithTask.taskTitle,
                style = MaterialTheme.typography.titleMedium
            )

            slotWithTask.taskSubject?.let { subject ->
                Text(
                    text = "Предмет: $subject",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Text(
                text = "${startTime.format(timeFormatter)}–${endTime.format(timeFormatter)}",
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = "Длительность: $durationMinutes мин",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Статус: ${slot.status}",
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.width(8.dp))
            }
        }
    }
}