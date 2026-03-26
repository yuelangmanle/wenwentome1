package com.wenwentome.reader.feature.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
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
    onFontSizeChange: (Float) -> Unit,
    onAutoFitFontSizeChange: (Boolean) -> Unit = {},
    onLineHeightChange: (Float) -> Unit,
    onLetterSpacingChange: (Float) -> Unit = {},
    onParagraphSpacingChange: (Float) -> Unit = {},
    onSidePaddingChange: (Int) -> Unit = {},
    onBackgroundPaletteChange: (String) -> Unit = {},
    onImportFontClick: () -> Unit = {},
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
                value = presentation.fontSizeSp,
                onValueChange = onFontSizeChange,
                valueRange = 14f..30f,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("自动适配字号", style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = { onAutoFitFontSizeChange(!presentation.autoFitFontSize) }) {
                    Text(
                        if (presentation.autoFitFontSize) "已开启" else "已关闭",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Text("行距", style = MaterialTheme.typography.labelLarge)
            Slider(
                value = presentation.lineHeightMultiplier,
                onValueChange = onLineHeightChange,
                valueRange = 1.2f..2.2f,
                modifier = Modifier.fillMaxWidth(),
            )
            Text("字距", style = MaterialTheme.typography.labelLarge)
            Slider(
                value = presentation.letterSpacingEm,
                onValueChange = onLetterSpacingChange,
                valueRange = -0.05f..0.2f,
                modifier = Modifier.fillMaxWidth(),
            )
            Text("段落间距", style = MaterialTheme.typography.labelLarge)
            Slider(
                value = presentation.paragraphSpacingEm,
                onValueChange = onParagraphSpacingChange,
                valueRange = 0f..1f,
                modifier = Modifier.fillMaxWidth(),
            )
            Text("页边距", style = MaterialTheme.typography.labelLarge)
            Slider(
                value = presentation.sidePaddingDp.toFloat(),
                onValueChange = { onSidePaddingChange(it.toInt()) },
                valueRange = 8f..40f,
                modifier = Modifier.fillMaxWidth(),
            )
            Text("阅读背景调色盘", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = presentation.backgroundPaletteKey == "paper",
                    onClick = { onBackgroundPaletteChange("paper") },
                    label = { Text("纸白") },
                )
                FilterChip(
                    selected = presentation.backgroundPaletteKey == "paper-green",
                    onClick = { onBackgroundPaletteChange("paper-green") },
                    label = { Text("青纸") },
                )
                FilterChip(
                    selected = presentation.backgroundPaletteKey == "night-paper",
                    onClick = { onBackgroundPaletteChange("night-paper") },
                    label = { Text("夜墨") },
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("当前字体", style = MaterialTheme.typography.bodyMedium)
                Text(
                    presentation.fontFamilyKey,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            TextButton(onClick = onImportFontClick) {
                Text("导入字体")
            }
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
