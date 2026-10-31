package com.studypulse.sync.ui.schedule

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.studypulse.sync.data.local.entity.SubjectEntity
import com.studypulse.sync.data.repository.ClassOccurrence

private val weekdays = listOf(1 to "Пн", 2 to "Вт", 3 to "Ср", 4 to "Чт", 5 to "Пт", 6 to "Сб", 7 to "Вс")
private val parities = listOf("EVERY" to "Каждую", "NUMERATOR" to "Числитель", "DENOMINATOR" to "Знаменатель")

@Composable
fun ScheduleScreen(
    onOpenImport: () -> Unit = {},
    viewModel: ScheduleViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.snackbar) {
        val message = state.snackbar
        if (message != null && state.canUndo) {
            val result = snackbarHostState.showSnackbar(message = message, actionLabel = "Отменить", duration = SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed) viewModel.undo() else viewModel.consumeSnackbar()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = viewModel::startAdd) { Text("Добавить занятие") }
                    OutlinedButton(onClick = onOpenImport) { Text("Импорт из текста") }
                }
                state.message?.let { Text(text = it, style = MaterialTheme.typography.bodyMedium) }

                if (state.isFormOpen) {
                    ScheduleForm(state = state, viewModel = viewModel)
                }

                if (state.occurrences.isEmpty()) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(text = "Расписание пусто", style = MaterialTheme.typography.titleMedium)
                            Text(text = "Добавь занятие вручную, импортируй из текста или скачай учебный пакет.", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                LazyColumn(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.occurrences, key = { "${it.templateExternalId}-${it.date}" }) { occurrence ->
                        OccurrenceCard(
                            occurrence = occurrence,
                            onCancel = { viewModel.cancelOccurrence(occurrence) },
                            onEdit = { viewModel.startEdit(occurrence) },
                            onDelete = { viewModel.deleteTemplate(occurrence) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduleForm(state: ScheduleUiState, viewModel: ScheduleViewModel) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = if (state.editingExternalId == null) "Новое занятие" else "Редактирование занятия",
                style = MaterialTheme.typography.titleMedium
            )
            if (state.subjects.isNotEmpty()) {
                Text(text = "Предметы", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { viewModel.selectSubject(null) }) { Text("Без предмета") }
                    state.subjects.forEach { subject -> OutlinedButton(onClick = { viewModel.selectSubject(subject) }) { Text(subject.name) } }
                }
            }
            OutlinedTextField(value = state.subjectName, onValueChange = viewModel::onSubjectNameChange, label = { Text("Название предмета") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = state.lessonType, onValueChange = viewModel::onLessonTypeChange, label = { Text("Тип занятия") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Text(text = "День недели", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                weekdays.forEach { (value, label) ->
                    if (state.dayOfWeek == value) Button(onClick = { viewModel.onDayOfWeekChange(value) }) { Text(label) }
                    else OutlinedButton(onClick = { viewModel.onDayOfWeekChange(value) }) { Text(label) }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = state.startTime, onValueChange = viewModel::onStartTimeChange, label = { Text("Начало") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = state.endTime, onValueChange = viewModel::onEndTimeChange, label = { Text("Конец") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            OutlinedTextField(value = state.room, onValueChange = viewModel::onRoomChange, label = { Text("Аудитория") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = state.teacher, onValueChange = viewModel::onTeacherChange, label = { Text("Преподаватель") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Text(text = "Чётность", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                parities.forEach { (value, label) ->
                    if (state.parity == value) Button(onClick = { viewModel.onParityChange(value) }) { Text(label) }
                    else OutlinedButton(onClick = { viewModel.onParityChange(value) }) { Text(label) }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = viewModel::save) { Text("Сохранить") }
                OutlinedButton(onClick = viewModel::toggleForm) { Text("Скрыть форму") }
            }
        }
    }
}

@Composable
private fun OccurrenceCard(occurrence: ClassOccurrence, onCancel: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = occurrence.displayDate, style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = occurrence.weekday, style = MaterialTheme.typography.bodySmall)
            }
            Text(text = "${occurrence.startTime}–${occurrence.endTime}", style = MaterialTheme.typography.titleMedium)
            Text(text = occurrence.subject, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            occurrence.lessonType?.let { Text(text = "Тип: $it", style = MaterialTheme.typography.bodyMedium) }
            occurrence.room?.let { Text(text = "Аудитория: $it", style = MaterialTheme.typography.bodyMedium) }
            occurrence.teacher?.let { Text(text = "Преподаватель: $it", style = MaterialTheme.typography.bodyMedium) }
            Text(text = "Источник: ${sourceLabel(occurrence.source)}", style = MaterialTheme.typography.bodySmall)
            Text(
                text = if (occurrence.isCancelled) "Статус: отменено" else "Статус: запланировано",
                color = if (occurrence.isCancelled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!occurrence.isCancelled) Button(onClick = onCancel) { Text("Отменить") }
                if (occurrence.source == "LOCAL") {
                    OutlinedButton(onClick = onEdit) { Text("Редактировать") }
                    OutlinedButton(onClick = onDelete) { Text("Удалить") }
                }
            }
        }
    }
}

private fun sourceLabel(source: String): String = when (source) {
    "LOCAL" -> "локальное"
    "PACKAGE" -> "из пакета"
    else -> source
}