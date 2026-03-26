package com.wenwentome.reader.feature.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.wenwentome.reader.core.model.ReaderChapter

@Composable
fun ReaderTocSheet(
    chapters: List<ReaderChapter>,
    currentChapterRef: String?,
    latestChapterRef: String?,
    initialScrollChapterRef: String?,
    progressLabel: String,
    onChapterClick: (ReaderChapter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(chapters, initialScrollChapterRef) {
        val targetIndex = chapters.indexOfFirst { it.chapterRef == initialScrollChapterRef }
        if (targetIndex >= 0) {
            listState.scrollToItem(targetIndex)
        }
    }

    Surface(
        modifier = modifier,
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("目录", style = MaterialTheme.typography.titleMedium)
            Text(
                text = progressLabel,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.testTag("reader-progress-label"),
            )
            if (!initialScrollChapterRef.isNullOrBlank()) {
                Text(
                    text = "定位 ${chapters.firstOrNull { it.chapterRef == initialScrollChapterRef }?.title ?: "当前章节"}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (chapters.isEmpty()) {
                Text("暂无目录", style = MaterialTheme.typography.bodyMedium)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    items(chapters, key = { it.chapterRef }) { chapter ->
                        val tag =
                            when (chapter.chapterRef) {
                                currentChapterRef -> "toc-current-chapter"
                                latestChapterRef -> "toc-latest-chapter"
                                else -> null
                            }
                        TextButton(
                            onClick = { onChapterClick(chapter) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (tag != null) Modifier.testTag(tag) else Modifier
                                ),
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = chapter.title,
                                    color = if (chapter.chapterRef == currentChapterRef) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                Text(
                                    text = "第 ${chapter.orderIndex + 1} 节",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
