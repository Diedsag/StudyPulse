package com.studypulse.sync.ui.packages

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.studypulse.sync.data.local.entity.StudyPackageEntity

@Composable
fun PackagesScreen(
    viewModel: PackagesViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = viewModel::refresh,
                enabled = !state.isRefreshing
            ) {
                Text("Обновить")
            }

            if (state.isRefreshing) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp))
            }
        }

        state.errorMessage?.let { message ->
            Text(
                text = "Ошибка: $message",
                color = MaterialTheme.colorScheme.error
            )
        }

        if (state.packages.isEmpty() && !state.isRefreshing) {
            Text(
                text = "Нет пакетов. Нажми «Обновить», если сервер доступен.",
                style = MaterialTheme.typography.bodyLarge
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = state.packages,
                key = { it.externalId }
            ) { packageEntity ->
                PackageCard(
                    packageEntity = packageEntity,
                    isDownloading = state.downloadingPackageId == packageEntity.externalId,
                    anyDownloading = state.downloadingPackageId != null,
                    onDownload = {
                        viewModel.downloadPackage(packageEntity.externalId)
                    }
                )
            }
        }
    }
}

@Composable
private fun PackageCard(
    packageEntity: StudyPackageEntity,
    isDownloading: Boolean,
    anyDownloading: Boolean,
    onDownload: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = packageEntity.title,
                style = MaterialTheme.typography.titleMedium
            )

            packageEntity.subject?.let { subject ->
                Text(
                    text = "Предмет: $subject",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            packageEntity.description?.let { description ->
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Text(
                text = if (packageEntity.isDownloaded) {
                    "Статус: доступен офлайн"
                } else {
                    "Статус: не скачан"
                },
                color = if (packageEntity.isDownloaded) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onDownload,
                    enabled = !anyDownloading
                ) {
                    Text(
                        text = if (packageEntity.isDownloaded) {
                            "Обновить пакет"
                        } else {
                            "Скачать пакет"
                        }
                    )
                }

                if (isDownloading) {
                    Spacer(modifier = Modifier.width(8.dp))
                    CircularProgressIndicator(modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}