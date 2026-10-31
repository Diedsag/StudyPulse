package com.studypulse.sync.ui.qa
import androidx.compose.runtime.remember

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.studypulse.sync.data.qa.Citation

@Composable
fun QaScreen(
    materialExternalId: String,
    onExit: () -> Unit,
    viewModel: QaViewModel = viewModel()
) {
        QaDemoNotice()
        QaDynamicStatus(state)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(materialExternalId) {
        viewModel.start(materialExternalId)
    }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = onExit) {
                Text("Назад")
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = if (state.materialTitle.isBlank()) {
                    "Вопрос по материалу"
                } else {
                    state.materialTitle
                },
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1
            )
        }

        if (state.isLoading) {
            CircularProgressIndicator()
            Text("Загружаем материал...")
            return@Column
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(state.messages) { message ->
                MessageBubble(message = message, onAskGeneral = viewModel::sendGeneral)
            }

            if (state.isSending) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.height(16.dp))
                        Text("Ищу в материалах...")
                    }
                }
            }
        }

        OutlinedTextField(
            value = state.input,
            onValueChange = viewModel::onInputChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Например: что такое первое начало термодинамики?") },
            minLines = 1,
            maxLines = 4
        )

        Button(
            onClick = viewModel::send,
            enabled = state.input.isNotBlank() && !state.isSending && state.materialFound,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Отправить")
        }
    }
}

@Composable
private fun MessageBubble(
    message: QaMessage,
    onAskGeneral: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = if (message.isUser) "Ты" else if (message.isGeneral) "StudyPulse (общий)" else "StudyPulse",
                style = MaterialTheme.typography.labelMedium,
                color = if (message.isGeneral) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
            )

            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyMedium
            )

            if (message.citations.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Источники:",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                message.citations.forEachIndexed { index, citation ->
                    Column(
                        modifier = Modifier.padding(start = 8.dp, top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "[${index + 1}] ${citation.materialTitle}" +
                                (citation.pageNumber?.let { ", стр. $it" } ?: "") +
                                (citation.timecode?.let { ", $it" } ?: ""),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = citation.quotedText.take(150) + if (citation.quotedText.length > 150) "..." else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (!message.isUser && !message.isGeneral && message.citations.isEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(onClick = onAskGeneral) {
                    Text("Ответить общо")
                }
            }
        }
    }
}


private data class QaDescription(
    val isLoading: Boolean,
    val hasError: Boolean,
    val errorText: String?,
    val hasAnswer: Boolean,
    val answerText: String?
)

private object QaStateInspector {
    fun describe(state: Any?): QaDescription {
        if (state == null) {
            return QaDescription(false, false, null, false, null)
        }

        var isLoading = false
        var errorText: String? = null
        var answerText: String? = null

        val fields = runCatching { state.javaClass.declaredFields }.getOrNull()
            ?: return QaDescription(false, false, null, false, null)

        for (field in fields) {
            runCatching {
                field.isAccessible = true
                val name = field.name.lowercase()
                val value = field.get(state)

                if (name.contains("loading") || name.contains("busy") || name.contains("progress")) {
                    isLoading = (value as? Boolean) ?: isLoading
                }

                if (name.contains("error") || name.contains("message")) {
                    val text = value as? String
                    if (!text.isNullOrBlank()) {
                        errorText = text
                    }
                }

                if (
                    name.contains("answer") ||
                    name.contains("response") ||
                    name.contains("result") ||
                    name.contains("text")
                ) {
                    val text = value as? String

                    if (!text.isNullOrBlank()) {
                        answerText = text
                    } else if (value != null) {
                        answerText = extractNestedString(
                            obj = value,
                            keys = arrayOf("answer", "text", "content", "message")
                        ) ?: answerText
                    }
                }
            }
        }

        return QaDescription(
            isLoading = isLoading,
            hasError = !errorText.isNullOrBlank(),
            errorText = errorText,
            hasAnswer = !answerText.isNullOrBlank(),
            answerText = answerText
        )
    }

    private fun extractNestedString(obj: Any, keys: Array<String>): String? {
        val fields = runCatching { obj.javaClass.declaredFields }.getOrNull() ?: return null

        for (field in fields) {
            val result = runCatching {
                field.isAccessible = true
                val name = field.name.lowercase()
                val value = field.get(obj)

                if (keys.any { name.contains(it.lowercase()) }) {
                    value as? String
                } else {
                    null
                }
            }.getOrNull()

            if (!result.isNullOrBlank()) {
                return result
            }
        }

        return null
    }
}

@Composable
private fun QaDemoNotice() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Q&A: демо-поиск по материалам",
                style = MaterialTheme.typography.titleSmall
            )

            Text(
                text = "Сейчас ответы ищутся по ключевым словам в тексте материала. Если вопрос сформулирован другими словами или материал ещё не разбит на фрагменты, ответ может не найтись.",
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                text = "Попробуй добавить больше текста в конспект или переформулировать вопрос ближе к терминам из материала.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun QaDynamicStatus(state: Any?) {
    val description = remember(state) { QaStateInspector.describe(state) }

    when {
        description.isLoading -> {
            Text(
                text = "Ищу ответ по материалу...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        description.hasError -> {
            Text(
                text = "Не получилось ответить: ${description.errorText ?: "неизвестная ошибка"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }

        !description.hasAnswer -> {
            Text(
                text = "Прямой ответ пока не найден. Это не обязательно ошибка: возможно, в материале нет точных совпадений по ключевым словам.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}