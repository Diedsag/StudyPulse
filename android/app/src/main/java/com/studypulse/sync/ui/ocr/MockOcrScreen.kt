package com.studypulse.sync.ui.ocr

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.studypulse.sync.data.local.entity.SubjectEntity

@Composable
fun MockOcrScreen(
    onExit: () -> Unit,
    viewModel: MockOcrViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(onClick = onExit) { Text("Назад") }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Mock OCR — обработка фото конспекта",
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = "Это демо-экран, имитирующий реальный OCR/ASR пайплайн. Вместо реального распознавания генерируется правдоподобный конспект по названию и предмету.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (state.subjects.isNotEmpty()) {
                    Text(text = "Предмет", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { viewModel.selectSubject(null) }) { Text("Без предмета") }
                        state.subjects.take(4).forEach { subject ->
                            OutlinedButton(onClick = { viewModel.selectSubject(subject) }) { Text(subject.name) }
                        }
                    }
                }

                OutlinedTextField(
                    value = state.title,
                    onValueChange = viewModel::onTitleChange,
                    label = { Text("Название (например: Физика, лекция 5)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = state.sourceLabel,
                    onValueChange = viewModel::onSourceLabelChange,
                    label = { Text("Источник") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = viewModel::process,
                    enabled = !state.isProcessing && state.title.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state.isProcessing) {
                        CircularProgressIndicator(modifier = Modifier.height(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Обработать фото")
                }
            }
        }

        state.message?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = if (state.saved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        }

        if (state.isProcessing || state.processingSteps.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "Процесс обработки:", style = MaterialTheme.typography.titleSmall)
                    state.processingSteps.forEach { step ->
                        Text(text = "• $step", style = MaterialTheme.typography.bodyMedium)
                    }
                    if (state.isProcessing) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator(modifier = Modifier.height(16.dp))
                            Text("Обработка...")
                        }
                    }
                }
            }
        }

        state.result?.let { result ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Саммари", style = MaterialTheme.typography.titleMedium)
                    Text(text = result.summary, style = MaterialTheme.typography.bodyMedium)

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(text = "Ключевые факты:", style = MaterialTheme.typography.labelLarge)
                    result.keyFacts.forEach { fact ->
                        Text(text = "• $fact", style = MaterialTheme.typography.bodyMedium)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(text = "Распознанный текст:", style = MaterialTheme.typography.labelLarge)
                    Text(
                        text = result.rawText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = viewModel::save,
                        enabled = !state.isSaving && !state.saved,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(modifier = Modifier.height(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(if (state.saved) "Сохранено" else "Сохранить материал")
                    }
                }
            }
        }
    }
}