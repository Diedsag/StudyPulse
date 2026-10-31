package com.studypulse.sync.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun EmptyState(
    title: String,
    message: String,
    primaryActionText: String? = null,
    onPrimaryAction: (() -> Unit)? = null,
    secondaryActionText: String? = null,
    onSecondaryAction: (() -> Unit)? = null
) {
    StudyPulseCard {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Text(text = message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (primaryActionText != null && onPrimaryAction != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Button(onClick = onPrimaryAction) { Text(primaryActionText) }
        }
        if (secondaryActionText != null && onSecondaryAction != null) {
            OutlinedButton(onClick = onSecondaryAction) { Text(secondaryActionText) }
        }
    }
}