package com.studypulse.sync.ui.materials

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.studypulse.sync.core.ux.UserLanguageMapper
import com.studypulse.sync.data.local.entity.MaterialEntity
import com.studypulse.sync.data.local.entity.SubjectEntity
import com.studypulse.sync.ui.review.ReviewRequest
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

@Composable
fun MaterialsScreen(
    onStartReview: (ReviewRequest) -> Unit = {},
    onAsk: (String) -> Unit = {},
    onOpenMockOcr: () -> Unit = {},
    viewModel: MaterialsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = viewModel::startAdd, modifier = Modifier.weight(1f)) { Text("Добавить") }
                OutlinedButton(onClick = onOpenMockOcr, modifier = Modifier.weight(1f)) { Text("Сканировать конспект") }
            }
            OutlinedButton(onClick = { onStartReview(ReviewRequest()) }, modifier = Modifier.fillMaxWidth()) {
                Text("Повторить все due-карточки")
            }
        }

        state.message?.let { Text(text = it, style = MaterialTheme.typography.bodyMedium) }

        if (state.isFormOpen) {
            MaterialForm(state = state, viewModel = viewModel)
        }

        if (state.materials.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Нет материалов", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Добавь материал вручную, обработай фото конспекта или скачай учебный пакет.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            state.materials.forEach { material ->
                MaterialCard(
                    material = material,
                    onStartReview = {
                        onStartReview(ReviewRequest(materialExternalId = material.externalId))
                    },
                    onAsk = { onAsk(material.externalId) },
                    onEdit = { viewModel.startEdit(material) },
                    onDelete = { viewModel.delete(material) }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MaterialForm(state: MaterialsUiState, viewModel: MaterialsViewModel) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (state.editingExternalId == null) "Новый материал" else "Редактирование материала",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(onClick = viewModel::toggleForm) { Text("Скрыть") }
            }

            val selectedLabel = when {
                state.selectedSubjectId == null -> "Без предмета"
                else -> state.subjects.find { it.id == state.selectedSubjectId }?.name
                    ?: "Предмет #${state.selectedSubjectId}"
            }
            Text(
                text = "Выбранный предмет: $selectedLabel",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )

            if (state.subjects.isNotEmpty()) {
                Text(text = "Предметы", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state.selectedSubjectId == null) {
                        Button(onClick = { viewModel.selectSubject(null) }) { Text("Без предмета") }
                    } else {
                        OutlinedButton(onClick = { viewModel.selectSubject(null) }) { Text("Без предмета") }
                    }
                    state.subjects.forEach { subject ->
                        if (state.selectedSubjectId == subject.id) {
                            Button(onClick = { viewModel.selectSubject(subject) }) { Text(subject.name) }
                        } else {
                            OutlinedButton(onClick = { viewModel.selectSubject(subject) }) { Text(subject.name) }
                        }
                    }
                }
            }

            OutlinedTextField(value = state.title, onValueChange = viewModel::onTitleChange, label = { Text("Название") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = state.sourceLabel, onValueChange = viewModel::onSourceLabelChange, label = { Text("Источник") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = state.text, onValueChange = viewModel::onTextChange, label = { Text("Текст материала") }, minLines = 6, maxLines = 12, modifier = Modifier.fillMaxWidth())

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = viewModel::save) { Text("Сохранить") }
                OutlinedButton(onClick = viewModel::toggleForm) { Text("Скрыть форму") }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MaterialCard(
    material: MaterialEntity,
    onStartReview: () -> Unit,
    onAsk: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val keyFacts = parseKeyFacts(material.keyFactsJson)
    val body = material.rawText?.takeIf { it.isNotBlank() } ?: material.summaryText
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = material.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            material.sourceLabel?.let { source ->
                Text(text = "Источник: $source", style = MaterialTheme.typography.bodySmall)
            }
            Text(text = "Источник данных: ${UserLanguageMapper.toUserSource(material.source)}", style = MaterialTheme.typography.bodySmall)
            Text(text = body, style = MaterialTheme.typography.bodyMedium, maxLines = 8, overflow = TextOverflow.Ellipsis)
            if (keyFacts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = "Ключевые факты:", style = MaterialTheme.typography.labelLarge)
                keyFacts.take(4).forEach { fact ->
                    Text(text = "• $fact", style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onStartReview, modifier = Modifier.fillMaxWidth()) { Text("Повторить карточки") }
                OutlinedButton(onClick = onAsk, modifier = Modifier.fillMaxWidth()) { Text("Спросить по материалу") }
                if (material.source == "LOCAL") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onEdit) { Text("Редактировать") }
                        OutlinedButton(onClick = onDelete) { Text("Удалить") }
                    }
                }
            }
        }
    }
}

private fun parseKeyFacts(raw: String?): List<String> {
    if (raw.isNullOrBlank()) return emptyList()
    return try {
        json.decodeFromString<List<String>>(raw)
    } catch (_: Throwable) {
        emptyList()
    }
}