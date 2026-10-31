package com.studypulse.sync.ui.plan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.studypulse.sync.data.local.dao.SlotWithTaskMaterial
import com.studypulse.sync.ui.review.ReviewRequest
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlanScreen(
    onStartReview: (ReviewRequest) -> Unit = {},
    onOpenFocus: (Long) -> Unit = {},
    viewModel: PlanViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val busy = state.isGenerating || state.isReplanning || state.isAccepting

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = viewModel::generatePlan, enabled = !busy) { Text("Сгенерировать план") }
            Button(
                onClick = viewModel::acceptPlan,
                enabled = state.proposedSlots.isNotEmpty() && !busy
            ) { Text("Принять план") }
            OutlinedButton(onClick = viewModel::replan, enabled = !busy) { Text("Перепланировать") }
            if (busy) CircularProgressIndicator(modifier = Modifier.size(18.dp))
        }

        state.message?.let { Text(text = it, style = MaterialTheme.typography.bodyLarge) }

        if (state.warnings.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "Внимание", style = MaterialTheme.typography.titleSmall)
                    state.warnings.forEach { w ->
                        Text(text = w, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        if (state.proposedSlots.isEmpty() && state.plannedSlots.isEmpty()) {
            Text(text = "Слотов пока нет. Скачай пакет или добавь задания, затем построй план.", style = MaterialTheme.typography.bodyLarge)
        }

        LazyColumn(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.proposedSlots.isNotEmpty()) {
                item { Text(text = "Предложенный план", style = MaterialTheme.typography.titleMedium) }
                items(state.proposedSlots, key = { "proposed-${it.slot.id}" }) { swt ->
                    SlotCard(swt, showActions = false, isCompleting = false, onComplete = {}, onStartReview, onOpenFocus)
                }
            }
            if (state.plannedSlots.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Принятый план", style = MaterialTheme.typography.titleMedium)
                }
                items(state.plannedSlots, key = { "planned-${it.slot.id}" }) { swt ->
                    SlotCard(
                        slotWithTask = swt,
                        showActions = true,
                        isCompleting = state.completingSlotId == swt.slot.id,
                        onComplete = { viewModel.completeSlot(swt.slot) },
                        onStartReview = onStartReview,
                        onOpenFocus = onOpenFocus
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
    onStartReview: (ReviewRequest) -> Unit,
    onOpenFocus: (Long) -> Unit
) {
    val slot = slotWithTask.slot
    val durationMinutes = ((slot.endDateTime - slot.startDateTime) / 60_000L).toInt()

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = formatDate(slot.startDateTime), style = MaterialTheme.typography.labelLarge)
            Text(
                text = slotWithTask.taskTitle,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            slotWithTask.taskSubject?.let { Text(text = "Предмет: $it", style = MaterialTheme.typography.bodyMedium) }
            slotWithTask.materialTitle?.let {
                Text(text = "Материал: $it", style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(text = "${formatTime(slot.startDateTime)}–${formatTime(slot.endDateTime)}", style = MaterialTheme.typography.bodyLarge)
            Text(text = "Длительность: $durationMinutes мин", style = MaterialTheme.typography.bodyMedium)

            // Почему предложен этот слот
            val deadline = slotWithTask.taskDeadline
            val priority = slotWithTask.taskPriority
            val reasonParts = mutableListOf<String>()
            if (!deadline.isNullOrBlank()) reasonParts += "дедлайн ${formatDate(deadline)}"
            reasonParts += "приоритет ${priorityLabel(priority)}"
            Text(
                text = "Почему здесь: " + reasonParts.joinToString(", "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(text = "Статус: ${slot.status}", color = MaterialTheme.colorScheme.primary)

            if (showActions) {
                Spacer(modifier = Modifier.height(4.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onComplete, enabled = !isCompleting, modifier = Modifier.fillMaxWidth()) { Text("Выполнено") }
                    OutlinedButton(
                        onClick = { onOpenFocus(slot.id) },
                        enabled = !isCompleting,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Фокус-таймер") }
                    OutlinedButton(
                        onClick = {
                            onStartReview(
                                ReviewRequest(
                                    packageExternalId = slot.packageExternalId,
                                    materialExternalId = slot.materialExternalId
                                )
                            )
                        },
                        enabled = !isCompleting,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Повторить карточки") }
                    if (isCompleting) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp))
                            Text("Сохраняем...")
                        }
                    }
                }
            }
        }
    }
}

private fun priorityLabel(priority: Int?): String = when (priority) {
    1 -> "низкий"
    3 -> "высокий"
    else -> "средний"
}

private fun formatDate(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault())
        .toLocalDate().format(DateTimeFormatter.ofPattern("dd.MM"))

private fun formatTime(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault())
        .toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))

private fun formatDate(raw: String): String = try {
    LocalDate.parse(raw).format(DateTimeFormatter.ofPattern("dd.MM"))
} catch (_: Throwable) {
    raw
}