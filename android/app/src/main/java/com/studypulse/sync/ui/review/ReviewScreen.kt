package com.studypulse.sync.ui.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
fun ReviewScreen(
    materialExternalId: String?,
    onExit: () -> Unit,
    viewModel: ReviewViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(materialExternalId) {
        viewModel.start(materialExternalId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = onExit) {
                Text("Назад")
            }

            if (!state.finished && state.cards.isNotEmpty()) {
                Text(
                    text = "Карточка ${state.index + 1} из ${state.cards.size}",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        if (state.isLoading) {
            CircularProgressIndicator()
            Text("Загружаем карточки...")
            return@Column
        }

        state.message?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (state.finished) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Повторение завершено",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = "Повторено карточек: ${state.reviewedCount}",
                        style = MaterialTheme.typography.bodyLarge
                    )

                    state.nextDueLabel?.let { due ->
                        Text(
                            text = "Следующее повторение: $due",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Button(onClick = onExit) {
                        Text("Готово")
                    }
                }
            }

            return@Column
        }

        val card = state.cards.getOrNull(state.index)

        if (card == null) {
            Text("Нет активной карточки.")
            return@Column
        }

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Вопрос",
                    style = MaterialTheme.typography.labelLarge
                )

                Text(
                    text = card.front,
                    style = MaterialTheme.typography.titleMedium
                )

                card.hint?.let { hint ->
                    Text(
                        text = "Подсказка: $hint",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (!state.showAnswer) {
                    Button(
                        onClick = viewModel::revealAnswer,
                        enabled = !state.isReviewing
                    ) {
                        Text("Показать ответ")
                    }
                } else {
                    Text(
                        text = "Ответ",
                        style = MaterialTheme.typography.labelLarge
                    )

                    Text(
                        text = card.back,
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.rate(1) },
                            enabled = !state.isReviewing,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Again")
                        }

                        Button(
                            onClick = { viewModel.rate(2) },
                            enabled = !state.isReviewing,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Hard")
                        }

                        Button(
                            onClick = { viewModel.rate(3) },
                            enabled = !state.isReviewing,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Good")
                        }

                        Button(
                            onClick = { viewModel.rate(4) },
                            enabled = !state.isReviewing,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Easy")
                        }
                    }

                    state.nextDueLabel?.let { due ->
                        Text(
                            text = "Следующее повторение: $due",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                if (state.isReviewing) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        Text("Сохраняем...")
                    }
                }
            }
        }
    }
}