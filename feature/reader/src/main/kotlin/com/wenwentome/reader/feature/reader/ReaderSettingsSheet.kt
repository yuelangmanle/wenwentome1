package com.wenwentome.reader.feature.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.wenwentome.reader.core.model.ReaderPresentationPrefs
import com.wenwentome.reader.core.model.ReaderTheme

@Composable
fun ReaderSettingsSheet(
    presentation: ReaderPresentationPrefs,
    progressLabel: String,
    onThemeChange: (ReaderTheme) -> Unit,
    onFontSizeChange: (Int) -> Unit,
    onLineHeightChange: (Float) -> Unit,
    onBrightnessChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.testTag("reader-settings-sheet"),
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("阅读设置", style = MaterialTheme.typography.titleMedium)
            Text("当前进度 $progressLabel", style = MaterialTheme.typography.bodyMedium)
            Text("主题", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReaderTheme.entries.forEach { theme ->
                    TextButton(onClick = { onThemeChange(theme) }) {
                        Text(
                            text = theme.label(),
                            color = if (theme == presentation.theme) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                    }
                }
            }
            Text("字体大小", style = MaterialTheme.typography.labelLarge)
            Slider(
                value = presentation.fontSizeSp.toFloat(),
                onValueChange = { onFontSizeChange(it.toInt()) },
                valueRange = 14f..30f,
                modifier = Modifier.fillMaxWidth(),
            )
            Text("行距", style = MaterialTheme.typography.labelLarge)
            Slider(
                value = presentation.lineHeightMultiplier,
                onValueChange = onLineHeightChange,
                valueRange = 1.2f..2.2f,
                modifier = Modifier.fillMaxWidth(),
            )
            Text("亮度", style = MaterialTheme.typography.labelLarge)
            Slider(
                value = presentation.brightnessPercent.toFloat(),
                onValueChange = { onBrightnessChange(it.toInt()) },
                valueRange = 0f..100f,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private fun ReaderTheme.label(): String =
    when (this) {
        ReaderTheme.PAPER -> "纸张"
        ReaderTheme.SEPIA -> "暖黄"
        ReaderTheme.NIGHT -> "夜间"
    }
