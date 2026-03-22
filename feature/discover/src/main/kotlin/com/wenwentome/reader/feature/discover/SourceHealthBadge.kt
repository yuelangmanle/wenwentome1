package com.wenwentome.reader.feature.discover

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun SourceHealthBadge(
    score: Float,
    label: String,
    modifier: Modifier = Modifier,
) {
    val clamped = score.coerceIn(0f, 1f)
    val percent = (clamped * 100).roundToInt()
    val containerColor = when {
        clamped >= 0.85f -> MaterialTheme.colorScheme.primaryContainer
        clamped >= 0.7f -> MaterialTheme.colorScheme.secondaryContainer
        clamped >= 0.5f -> MaterialTheme.colorScheme.tertiaryContainer
        clamped >= 0.3f -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.errorContainer
    }
    val contentColor = when {
        clamped >= 0.85f -> MaterialTheme.colorScheme.onPrimaryContainer
        clamped >= 0.7f -> MaterialTheme.colorScheme.onSecondaryContainer
        clamped >= 0.5f -> MaterialTheme.colorScheme.onTertiaryContainer
        clamped >= 0.3f -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onErrorContainer
    }

    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.small,
        modifier = modifier,
    ) {
        Text(
            text = "$label $percent%",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}
