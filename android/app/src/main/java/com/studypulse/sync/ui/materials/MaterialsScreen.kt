package com.studypulse.sync.ui.materials

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.studypulse.sync.data.local.entity.MaterialEntity
import kotlinx.serialization.json.Json

private val json = Json {
    ignoreUnknownKeys = true
}

@Composable
fun MaterialsScreen(
    onStartReview: (String?) -> Unit = {},
    viewModel: MaterialsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(onClick = { onStartReview(null) }) {
                Text("Повторить все due-карточки")
            }
        }

        if (state.materials.isEmpty()) {
            Text(
                text = "Нет материалов. Скачай учебный пакет во вкладке Packages.",
                style = MaterialTheme.typography.bodyLarge
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = state.materials,
                key = { it.externalId }
            ) { material ->
                MaterialCard(
                    material = material,
                    onStartReview = {
                        onStartReview(material.externalId)
                    }
                )
            }
        }
    }
}

@Composable
private fun MaterialCard(
    material: MaterialEntity,
    onStartReview: () -> Unit
) {
    val keyFacts = parseKeyFacts(material.keyFactsJson)

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = material.title,
                style = MaterialTheme.typography.titleMedium
            )

            material.sourceLabel?.let { source ->
                Text(
                    text = "Источник: $source",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Text(
                text = material.summaryText,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 8,
                overflow = TextOverflow.Ellipsis
            )

            if (keyFacts.isNotEmpty()) {
                Text(
                    text = "Ключевые факты:",
                    style = MaterialTheme.typography.labelLarge
                )

                keyFacts.forEach { fact ->
                    Text(
                        text = "• $fact",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Button(onClick = onStartReview) {
                Text("Повторить карточки материала")
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