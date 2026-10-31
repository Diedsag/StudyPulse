package com.studypulse.sync.ui.schedule.import

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.studypulse.sync.data.import.ParsedLesson

private val weekdays = listOf(1 to "Пн", 2 to "Вт", 3 to "Ср", 4 to "Чт", 5 to "Пт", 6 to "Сб", 7 to "Вс")
private val parities = listOf("EVERY" to "Каждую", "NUMERATOR" to "Числитель", "DENOMINATOR" to "Знаменатель")

@Composable
fun ScheduleImportScreen(
    onExit: () -> Unit,
    viewModel: ScheduleImportViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(onClick = onExit) {
            Text("Назад")
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Импорт расписания из текста",
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = "Вставьте текст расписания. Приложение попытается распознать дни недели, время, предмет, тип, аудиторию и преподавателя.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = """Пример:
Понедельник
10:00-11:30 Физика, лекция, ауд 204, Иванов А.П.
14:00-15:30 История, семинар, ауд 312""",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = state.inputText,
                    onValueChange = viewModel::onInputChange,
                    label = { Text("Текст расписания") },
                    minLines = 6,
                    maxLines = 12,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = viewModel::parse,
                    enabled = !state.isParsing && state.inputText.isNotBlank()
                ) {
                    if (state.isParsing) {
                        CircularProgressIndicator(modifier = Modifier.height(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Text("Распознать")
                }
            }
        }

        state.message?.let {
            Text(text = it, style = MaterialTheme.typography.bodyMedium)
        }

        if (state.lessons.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Распознано занятий: ${state.lessons.size}",
                        style = MaterialTheme.typography.titleSmall
                    )

                    Text(
                        text = "Проверьте поля и подтвердите импорт.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                state.lessons.forEachIndexed { index, lesson ->
                    LessonRow(
                        lesson = lesson,
                        onToggle = { viewModel.toggleLesson(index) },
                        onUpdate = { newLesson -> viewModel.updateLesson(index, newLesson) }
                    )
                }
            }

            Button(
                onClick = viewModel::import,
                enabled = !state.isImporting && state.lessons.any { it.isChecked },
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isImporting) {
                    CircularProgressIndicator(modifier = Modifier.height(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Text("Импортировать выбранные")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LessonRow(
    lesson: ImportLessonState,
    onToggle: () -> Unit,
    onUpdate: (ImportLessonState) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = lesson.isChecked,
                    onCheckedChange = { onToggle() }
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = when (lesson.parsed.confidence) {
                        ParsedLesson.Confidence.HIGH -> "Высокая уверенность"
                        ParsedLesson.Confidence.MEDIUM -> "Средняя уверенность"
                        ParsedLesson.Confidence.LOW -> "Низкая уверенность — проверьте"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = when (lesson.parsed.confidence) {
                        ParsedLesson.Confidence.HIGH -> MaterialTheme.colorScheme.primary
                        ParsedLesson.Confidence.MEDIUM -> MaterialTheme.colorScheme.tertiary
                        ParsedLesson.Confidence.LOW -> MaterialTheme.colorScheme.error
                    }
                )
            }

            OutlinedTextField(
                value = lesson.subjectName,
                onValueChange = { onUpdate(lesson.copy(subjectName = it)) },
                label = { Text("Предмет") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = lesson.lessonType,
                onValueChange = { onUpdate(lesson.copy(lessonType = it)) },
                label = { Text("Тип (лекция/семинар/лабораторная)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text(text = "День недели", style = MaterialTheme.typography.labelLarge)

            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                weekdays.forEach { (value, label) ->
                    if (lesson.dayOfWeek == value) {
                        Button(onClick = { onUpdate(lesson.copy(dayOfWeek = value)) }) {
                            Text(label, maxLines = 1)
                        }
                    } else {
                        OutlinedButton(onClick = { onUpdate(lesson.copy(dayOfWeek = value)) }) {
                            Text(label, maxLines = 1)
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = lesson.startTime,
                    onValueChange = { onUpdate(lesson.copy(startTime = it)) },
                    label = { Text("Начало") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(0.47f)
                )

                OutlinedTextField(
                    value = lesson.endTime,
                    onValueChange = { onUpdate(lesson.copy(endTime = it)) },
                    label = { Text("Конец") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(0.47f)
                )
            }

            OutlinedTextField(
                value = lesson.room,
                onValueChange = { onUpdate(lesson.copy(room = it)) },
                label = { Text("Аудитория") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = lesson.teacher,
                onValueChange = { onUpdate(lesson.copy(teacher = it)) },
                label = { Text("Преподаватель") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text(text = "Чётность", style = MaterialTheme.typography.labelLarge)

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                parities.forEach { (value, label) ->
                    if (lesson.parity == value) {
                        Button(onClick = { onUpdate(lesson.copy(parity = value)) }) {
                            Text(label, maxLines = 1)
                        }
                    } else {
                        OutlinedButton(onClick = { onUpdate(lesson.copy(parity = value)) }) {
                            Text(label, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}