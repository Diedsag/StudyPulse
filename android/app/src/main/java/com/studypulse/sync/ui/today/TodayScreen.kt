package com.studypulse.sync.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.studypulse.sync.data.local.entity.TaskEntity
import com.studypulse.sync.data.repository.DeadlineRisk
import com.studypulse.sync.ui.review.ReviewRequest
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun TodayScreen(
    onOpenFocus: (Long) -> Unit = {},
    onStartReview: (ReviewRequest) -> Unit = {},
    onGoToPlan: () -> Unit = {},
    onGoToTasks: () -> Unit = {},
    viewModel: TodayViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    if (state.isLoading) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator()
            Text("Загружаем сегодняшний план...")
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column {
            Text(text = state.greeting, style = MaterialTheme.typography.headlineSmall)
            Text(
                text = state.dateLabel.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        state.errorMessage?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }

        ProgressCard(done = state.doneTodayCount, total = state.totalTodayCount)

        if (state.risks.isNotEmpty()) {
            RiskCard(risks = state.risks, onGoToPlan = onGoToPlan)
        }

        NextSlotCard(
            slot = state.nextSlot,
            onOpenFocus = onOpenFocus,
            onGoToPlan = onGoToPlan
        )

        if (state.dueCardsCount > 0) {
            CardsCard(
                count = state.dueCardsCount,
                onReview = { onStartReview(ReviewRequest()) }
            )
        }

        if (state.urgentTasks.isNotEmpty()) {
            Text(text = "Срочные задания", style = MaterialTheme.typography.titleMedium)
            state.urgentTasks.forEach { task ->
                UrgentTaskCard(task = task, onGoToTasks = onGoToTasks)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun ProgressCard(done: Int, total: Int) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = "Прогресс дня", style = MaterialTheme.typography.titleMedium)
            Text(
                text = if (total == 0) "На сегодня слотов пока нет" else "Выполнено $done из $total слотов",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun RiskCard(risks: List<DeadlineRisk>, onGoToPlan: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Риск просрочки",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )
            risks.forEach { risk ->
                Text(
                    text = "«${risk.taskTitle}»: не хватает ${risk.deficitMinutes} мин " +
                        "(нужно ${risk.remainingMinutes}, свободно ${risk.availableMinutes}).",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            OutlinedButton(onClick = onGoToPlan) {
                Text("Перепланировать")
            }
        }
    }
}

@Composable
private fun NextSlotCard(
    slot: SlotWithTaskMaterial?,
    onOpenFocus: (Long) -> Unit,
    onGoToPlan: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "Ближайший слот", style = MaterialTheme.typography.titleMedium)
            if (slot == null) {
                Text(
                    text = "Ближайших слотов нет. Загляните в план.",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedButton(onClick = onGoToPlan) { Text("Открыть план") }
            } else {
                Text(
                    text = "${formatTime(slot.slot.startDateTime)}–${formatTime(slot.slot.endDateTime)}",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = slot.taskTitle,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                slot.materialTitle?.let {
                    Text(text = "Материал: $it", style = MaterialTheme.typography.bodyMedium)
                }
                Button(onClick = { onOpenFocus(slot.slot.id) }) {
                    Text("Запустить фокус")
                }
            }
        }
    }
}

@Composable
private fun CardsCard(count: Int, onReview: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "Повторение", style = MaterialTheme.typography.titleMedium)
            Text(text = "Готово к повторению: $count карточек", style = MaterialTheme.typography.bodyLarge)
            Button(onClick = onReview) { Text("Повторить") }
        }
    }
}

@Composable
private fun UrgentTaskCard(task: TaskEntity, onGoToTasks: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            task.subject?.let { Text(text = "Предмет: $it", style = MaterialTheme.typography.bodyMedium) }
            Text(
                text = "Дедлайн: ${formatDate(task.deadlineDate)} · осталось ${task.remainingMinutes} мин",
                style = MaterialTheme.typography.bodyMedium
            )
            OutlinedButton(onClick = onGoToTasks) { Text("К заданиям") }
        }
    }
}

private fun formatTime(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault())
        .toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))

private fun formatDate(raw: String): String = try {
    LocalDate.parse(raw).format(DateTimeFormatter.ofPattern("dd.MM"))
} catch (_: Throwable) {
    raw
}