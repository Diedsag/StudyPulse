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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.mergeDescendants
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selectableGroup
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.studypulse.sync.core.ux.UserLanguageMapper
import com.studypulse.sync.data.local.entity.MaterialEntity
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

        // 1.4: сообщение экрана озвучивается TalkBack как polite live-region.
        state.message?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
            )
        }

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
                    onStartReview = { onStartReview(ReviewRequest(materialExternalId = material.externalId)) },
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
    // 1.5: фокус на поле названия при открытии формы, с защитой от гонки attach.
    val titleFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { titleFocus.requestFocus() } }

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
                // 1.2: группа выбора без смены компонента на FilterChip.
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.selectableGroup()
                ) {
                    val noneSelected = state.selectedSubjectId == null
                    if (noneSelected) {
                        Button(
                            onClick = { viewModel.selectSubject(null) },
                            modifier = Modifier.semantics { role = Role.RadioButton; selected = true }
                        ) { Text("Без предмета", maxLines = 1) }
                    } else {
                        OutlinedButton(
                            onClick = { viewModel.selectSubject(null) },
                            modifier = Modifier.semantics { role = Role.RadioButton; selected = false }
                        ) { Text("Без предмета", maxLines = 1) }
                    }
                    state.subjects.forEach { subject ->
                        val sel = state.selectedSubjectId == subject.id
                        if (sel) {
                            Button(
                                onClick = { viewModel.selectSubject(subject) },
                                modifier = Modifier.semantics { role = Role.RadioButton; selected = true }
                            ) { Text(subject.name, maxLines = 1) }
                        } else {
                            OutlinedButton(
                                onClick = { viewModel.selectSubject(subject) },
                                modifier = Modifier.semantics { role = Role.RadioButton; selected = false }
                            ) { Text(subject.name, maxLines = 1) }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = state.title,
                onValueChange = viewModel::onTitleChange,
                label = { Text("Название") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().focusRequester(titleFocus)
            )
            OutlinedTextField(
                value = state.sourceLabel,
                onValueChange = viewModel::onSourceLabelChange,
                label = { Text("Источник") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.text,
                onValueChange = viewModel::onTextChange,
                label = { Text("Текст материала") },
                minLines = 6,
                maxLines = 12,
                modifier = Modifier.fillMaxWidth()
            )

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
    val title = material.title

    Card(modifier = Modifier.fillMaxWidth()) {
        // Внешний Column НЕ mergeDescendants: иначе кнопки слились бы в один фокус.
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {

            // 1.3: описательный под-блок объединяется в один узел доступности.
            Column(modifier = Modifier.mergeDescendants()) {
                // 3.2: заголовок материала помечен как heading.
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.heading()
                )
                material.sourceLabel?.let { source ->
                    Text(text = "Источник: $source", style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    text = "Источник данных: ${UserLanguageMapper.toUserSource(material.source)}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(text = body, style = MaterialTheme.typography.bodyMedium, maxLines = 8, overflow = TextOverflow.Ellipsis)
                if (keyFacts.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = "Ключевые факты:", style = MaterialTheme.typography.labelLarge)
                    keyFacts.take(4).forEach { fact ->
                        Text(text = "• $fact", style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 1.1: contentDescription содержит и глагол, и контекст материала,
            // поэтому TalkBack не теряет смысл действия.
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onStartReview,
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Повторить карточки для материала $title" }
                ) { Text("Повторить карточки") }

                OutlinedButton(
                    onClick = onAsk,
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Задать вопрос по материалу $title" }
                ) { Text("Спросить по материалу") }

                if (material.source == "LOCAL") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onEdit,
                            modifier = Modifier.semantics { contentDescription = "Редактировать материал $title" }
                        ) { Text("Редактировать", maxLines = 1) }
                        OutlinedButton(
                            onClick = onDelete,
                            modifier = Modifier.semantics { contentDescription = "Удалить материал $title" }
                        ) { Text("Удалить", maxLines = 1) }
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