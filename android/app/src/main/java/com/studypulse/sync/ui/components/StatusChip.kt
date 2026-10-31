package com.studypulse.sync.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

enum class StatusTone { Neutral, Info, Success, Warning, Danger }

@Composable
fun StatusChip(text: String, tone: StatusTone, modifier: Modifier = Modifier) {
    val colorScheme = MaterialTheme.colorScheme
    val (container, content) = when (tone) {
        StatusTone.Neutral -> colorScheme.surfaceVariant to colorScheme.onSurfaceVariant
        StatusTone.Info -> colorScheme.primaryContainer to colorScheme.onPrimaryContainer
        StatusTone.Success -> colorScheme.secondaryContainer to colorScheme.onSecondaryContainer
        StatusTone.Warning -> colorScheme.tertiaryContainer to colorScheme.onTertiaryContainer
        StatusTone.Danger -> colorScheme.errorContainer to colorScheme.onErrorContainer
    }
    Surface(modifier = modifier, shape = MaterialTheme.shapes.small, color = container) {
        Text(
            text = text,
            color = content,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}