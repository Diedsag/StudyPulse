package com.studypulse.sync.ui.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.studypulse.sync.data.local.entity.SubjectEntity
import com.studypulse.sync.data.local.entity.TaskEntity
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun TasksScreen(
    onOpenPlanner: () -> Unit = {},
    viewModel: TasksViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = androidx.compose.runtime.remember { SnackbarHostState() }

    LaunchedEffect(state.snackbar) {
        val message = state.snackbar
        if (message != null && state.canUndo) {
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = "Отменить",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undo()
            } else {
                viewModel.consumeSnackbar()
            }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = viewModel::startAdd) { Text("Добавить задание") }
                    OutlinedButton(onClick = onOpenPlanner) { Text("Спланировать") }
                }

                state.message?.let { Text(text = it, style = MaterialTheme.typography.bodyMedium) }

                if (state.isFormOpen) {
                    TaskForm(state = state, viewModel = viewModel)
                }

                if (state.tasks.isEmpty()) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(text = "Пока нет заданий", style = MaterialTheme.typography.titleMedium)
                            Text(text = "Добавь задание вручную или скачай учебный пакет.", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                LazyColumn(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.tasks, key = { it.externalId }) { task ->
                        TaskCard(
                            task = task,
                            onDone = { viewModel.markDone(task) },
                            onEdit = { viewModel.startEdit(task) },
                            onDelete = { viewModel.delete(task) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskForm(state: TasksUiState, viewModel: TasksViewModel) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = if (state.editingExternalId == null) "Новое задание" else "Редактирование задания",
                style = MaterialTheme.typography.titleMedium
            )
            if (state.subjects.isNotEmpty()) {
                Text(text = "Предметы", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { viewModel.selectSubject(null) }) { Text("Без предмета") }
                    state.subjects.forEach { subject ->
                        OutlinedButton(onClick = { viewModel.selectSubject(subject) }) { Text(subject.name) }
                    }
                }
            }
            OutlinedTextField(value = state.subjectName, onValueChange = viewModel::onSubjectNameChange, label = { Text("Предмет") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = state.title, onValueChange = viewModel::onTitleChange, label = { Text("Название") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = state.deadlineDate, onValueChange = viewModel::onDeadlineChange, label = { Text("Дедлайн (ГГГГ-ММ-ДД)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = state.effortMinutes, onValueChange = viewModel::onEffortChange, label = { Text("Трудоёмкость, мин") }, singleLine = true, modifier = Modifier.weight(1f))
                if (state.editingExternalId != null) {
                    OutlinedTextField(value = state.remainingMinutes.toString(), onValueChange = viewModel::onRemainingChange, label = { Text("Осталось, мин") }, singleLine = true, modifier = Modifier.weight(1f))
                }
            }
            Text(text = "Приоритет", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1 to "Низкий", 2 to "Средний", 3 to "Высокий").forEach { (value, label) ->
                    if (state.priority == value) Button(onClick = { viewModel.onPriorityChange(value) }) { Text(label) }
                    else OutlinedButton(onClick = { viewModel.onPriorityChange(value) }) { Text(label) }
                }
            }
            OutlinedTextField(value = state.description, onValueChange = viewModel::onDescriptionChange, label = { Text("Описание") }, minLines = 2, maxLines = 4, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = viewModel::save) { Text("Сохранить") }
                OutlinedButton(onClick = viewModel::toggleForm) { Text("Скрыть форму") }
            }
        }
    }
}

@Composable
private fun TaskCard(task: TaskEntity, onDone: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = task.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            task.subject?.let { Text(text = "Предмет: $it", style = MaterialTheme.typography.bodyMedium) }
            Text(text = "Дедлайн: ${formatDate(task.deadlineDate)}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Трудоёмкость: ${task.effortMinutes} мин", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Осталось: ${task.remainingMinutes} мин", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Приоритет: ${priorityLabel(task.priority)}", style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "Статус: ${statusLabel(task.status)}",
                color = if (task.status == "DONE") MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
            )
            Text(text = "Источник: ${sourceLabel(task.source)}", style = MaterialTheme.typography.bodySmall)
            task.description?.let {
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = it, style = MaterialTheme.typography.bodyMedium, maxLines = 5, overflow = TextOverflow.Ellipsis)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (task.status != "DONE") Button(onClick = onDone) { Text("Выполнено") }
                if (task.source == "LOCAL") {
                    OutlinedButton(onClick = onEdit) { Text("Редактировать") }
                    OutlinedButton(onClick = onDelete) { Text("Удалить") }
                }
            }
        }
    }
}

private fun formatDate(rawDate: String): String = try {
    LocalDate.parse(rawDate).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
} catch (_: Throwable) {
    rawDate
}

private fun priorityLabel(priority: Int): String = when (priority) {
    1 -> "низкий"
    3 -> "высокий"
    else -> "средний"
}

private fun statusLabel(status: String): String = when (status) {
    "PENDING" -> "не начато"
    "IN_PROGRESS" -> "в работе"
    "DONE" -> "выполнено"
    "OVERDUE" -> "просрочено"
    else -> status
}

private fun sourceLabel(source: String): String = when (source) {
    "LOCAL" -> "локальное"
    "PACKAGE" -> "из пакета"
    "MATERIAL" -> "из материала"
    else -> source
}