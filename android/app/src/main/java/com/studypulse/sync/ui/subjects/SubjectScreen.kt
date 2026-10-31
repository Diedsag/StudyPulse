package com.studypulse.sync.ui.subjects

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.studypulse.sync.data.local.entity.SubjectEntity

@Composable
fun SubjectScreen(
    viewModel: SubjectViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(onClick = viewModel::startAdd) {
            Text("Добавить предмет")
        }

        state.message?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        if (state.isFormOpen) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (state.editingId == null) "Новый предмет" else "Редактирование предмета",
                        style = MaterialTheme.typography.titleMedium
                    )

                    OutlinedTextField(
                        value = state.name,
                        onValueChange = viewModel::onNameChange,
                        label = { Text("Название") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = state.teacher,
                        onValueChange = viewModel::onTeacherChange,
                        label = { Text("Преподаватель") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "Цвет предмета",
                        style = MaterialTheme.typography.labelLarge
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SubjectPalette.colors.forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(
                                        color = Color(color),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.selectColor(color) },
                                    modifier = Modifier.size(34.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                                ) {
                                    Text(
                                        text = if (state.colorArgb == color) "✓" else "",
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(onClick = viewModel::save) {
                            Text("Сохранить")
                        }

                        OutlinedButton(onClick = viewModel::closeForm) {
                            Text("Отмена")
                        }
                    }
                }
            }
        }

        if (state.subjects.isEmpty() && !state.isFormOpen) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Пока нет предметов",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = "Создай первый предмет, чтобы добавлять занятия, задания и материалы.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = state.subjects,
                key = { it.id }
            ) { subject ->
                SubjectCard(
                    subject = subject,
                    onEdit = { viewModel.startEdit(subject) },
                    onDelete = { viewModel.delete(subject) }
                )
            }
        }
    }
}

@Composable
private fun SubjectCard(
    subject: SubjectEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .background(
                            color = Color(subject.colorArgb),
                            shape = CircleShape
                        )
                )

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = subject.name,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            subject.teacher?.let { teacher ->
                Text(
                    text = "Преподаватель: $teacher",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Text(
                text = "Источник: ${sourceLabel(subject.source)}",
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = onEdit) {
                    Text("Редактировать")
                }

                OutlinedButton(onClick = onDelete) {
                    Text("Удалить")
                }
            }
        }
    }
}

private fun sourceLabel(source: String): String {
    return when (source) {
        "LOCAL" -> "локальный"
        "PACKAGE" -> "из пакета"
        else -> source
    }
}