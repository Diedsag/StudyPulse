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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.studypulse.sync.data.local.dao.SlotWithTaskMaterial
import com.studypulse.sync.data.local.entity.StudySlotEntity
import com.studypulse.sync.ui.review.ReviewRequest
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun PlanScreen(
    onStartReview: (ReviewRequest) -> Unit = {},
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
                enabled = !state.isGenerating && !state.isAccepting && state.completingSlotId == null
            ) {
                Text("Сгенерировать план")
            }

            Button(
                onClick = viewModel::acceptPlan,
                enabled = state.proposedSlots.isNotEmpty() &&
                    !state.isGenerating &&
                    !state.isAccepting &&
                    state.completingSlotId == null
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

        if (state.proposedSlots.isEmpty() && state.plannedSlots.isEmpty()) {
            Text(
                text = "Слотов пока нет. Скачай пакет, построй план и прими его.",
                style = MaterialTheme.typography.bodyLarge
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.proposedSlots.isNotEmpty()) {
                item {
                    Text(
                        text = "Предложенный план",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                items(
                    items = state.proposedSlots,
                    key = { "proposed-${it.slot.id}" }
                ) { slotWithTask ->
                    SlotCard(
                        slotWithTask = slotWithTask,
                        showActions = false,
                        isCompleting = false,
                        onComplete = {},
                        onStartReview = onStartReview
                    )
                }
            }

            if (state.plannedSlots.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Принятый план",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                items(
                    items = state.plannedSlots,
                    key = { "planned-${it.slot.id}" }
                ) { slotWithTask ->
                    SlotCard(
                        slotWithTask = slotWithTask,
                        showActions = true,
                        isCompleting = state.completingSlotId == slotWithTask.slot.id,
                        onComplete = {
                            viewModel.completeSlot(slotWithTask.slot)
                        },
                        onStartReview = onStartReview
                    )
                }
            }
        }
    }
}

@Composable
private fun SlotCard(
    slotWithTask: SlotWithTaskMaterial,
    showActions: Boolean,
    isCompleting: Boolean,
    onComplete: () -> Unit,
    onStartReview: (ReviewRequest) -> Unit
) {
    val slot = slotWithTask.slot
    val durationMinutes = ((slot.endDateTime - slot.startDateTime) / 60_000L).toInt()

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = formatDate(slot.startDateTime),
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

            slotWithTask.materialTitle?.let { materialTitle ->
                Text(
                    text = "Материал: $materialTitle",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Text(
                text = "${formatTime(slot.startDateTime)}–${formatTime(slot.endDateTime)}",
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = "Длительность: $durationMinutes мин",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "Статус: ${slot.status}",
                color = MaterialTheme.colorScheme.primary
            )

            if (showActions) {
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onComplete,
                        enabled = !isCompleting
                    ) {
                        Text("Выполнено")
                    }

                    OutlinedButton(
                        onClick = {
                            onStartReview(
                                ReviewRequest(
                                    packageExternalId = slot.packageExternalId,
                                    materialExternalId = slot.materialExternalId
                                )
                            )
                        },
                        enabled = !isCompleting
                    ) {
                        Text("Повторить")
                    }

                    if (isCompleting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

private fun formatDate(epochMillis: Long): String {
    val date = Instant
        .ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()

    return date.format(DateTimeFormatter.ofPattern("dd.MM"))
}

private fun formatTime(epochMillis: Long): String {
    val time = Instant
        .ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalTime()

    return time.format(DateTimeFormatter.ofPattern("HH:mm"))
}