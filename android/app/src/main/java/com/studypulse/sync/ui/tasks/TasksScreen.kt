package com.studypulse.sync.ui.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.studypulse.sync.data.local.entity.TaskEntity
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun TasksScreen(
    viewModel: TasksViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (state.tasks.isEmpty()) {
            Text(
                text = "Нет заданий. Скачай учебный пакет во вкладке Packages.",
                style = MaterialTheme.typography.bodyLarge
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = state.tasks,
                key = { it.externalId }
            ) { task ->
                TaskCard(task = task)
            }
        }
    }
}

@Composable
private fun TaskCard(task: TaskEntity) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.titleMedium
            )

            task.subject?.let { subject ->
                Text(
                    text = "Предмет: $subject",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Text(
                text = "Дедлайн: ${formatDate(task.deadlineDate)}",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "Трудоёмкость: ${task.effortMinutes} мин",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "Осталось: ${task.remainingMinutes} мин",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "Приоритет: ${priorityLabel(task.priority)}",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "Статус: ${statusLabel(task.status)}",
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private fun formatDate(rawDate: String): String {
    return try {
        LocalDate.parse(rawDate).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
    } catch (_: Throwable) {
        rawDate
    }
}

private fun priorityLabel(priority: Int): String {
    return when (priority) {
        1 -> "низкий"
        3 -> "высокий"
        else -> "средний"
    }
}

private fun statusLabel(status: String): String {
    return when (status) {
        "PENDING" -> "не начато"
        "IN_PROGRESS" -> "в работе"
        "DONE" -> "выполнено"
        else -> status
    }
}