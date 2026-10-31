package com.studypulse.sync.ui.qa

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

@Composable
fun QaScreen(
    materialExternalId: String,
    onExit: () -> Unit,
    viewModel: QaViewModel = viewModel()
) {
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
                MessageBubble(message = message)
            }

            if (state.isSending) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.height(16.dp))
                        Text("Думаю локально...")
                    }
                }
            }
        }

        OutlinedTextField(
            value = state.input,
            onValueChange = viewModel::onInputChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Например: кратко о чём материал?") },
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
    message: com.studypulse.sync.ui.qa.QaMessage
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = if (message.isUser) "Ты" else "StudyPulse",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}