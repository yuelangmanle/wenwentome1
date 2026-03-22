package com.wenwentome.reader.feature.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ReaderAssistantSheet(
    state: ReaderAssistantUiState,
    onSummarizeChapter: () -> Unit,
    onExplainParagraph: () -> Unit,
    onTranslateParagraph: () -> Unit,
    onSpeakChapter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "AI 阅读助手",
                style = MaterialTheme.typography.titleMedium,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = false,
                    onClick = onSummarizeChapter,
                    label = { Text("章节总结") },
                )
                FilterChip(
                    selected = false,
                    onClick = onExplainParagraph,
                    label = { Text("段落解释") },
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = false,
                    onClick = onTranslateParagraph,
                    label = { Text("AI 翻译") },
                )
                FilterChip(
                    selected = false,
                    onClick = onSpeakChapter,
                    label = { Text("TTS 朗读") },
                )
            }
            if (state.isLoading) {
                Text(
                    text = "AI 正在整理中…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            state.errorMessage?.takeIf { it.isNotBlank() }?.let { message ->
                ReaderAssistantResultCard(title = "调用状态", content = message)
            }
            state.summary?.takeIf { it.isNotBlank() }?.let { content ->
                ReaderAssistantResultCard(title = "章节总结", content = content)
            }
            state.explanation?.takeIf { it.isNotBlank() }?.let { content ->
                ReaderAssistantResultCard(title = "段落解释", content = content)
            }
            state.translation?.takeIf { it.isNotBlank() }?.let { content ->
                ReaderAssistantResultCard(title = "AI 翻译", content = content)
            }
            state.ttsScript?.takeIf { it.isNotBlank() }?.let { content ->
                ReaderAssistantResultCard(title = "TTS 文稿", content = content)
            }
        }
    }
}

@Composable
private fun ReaderAssistantResultCard(
    title: String,
    content: String,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
