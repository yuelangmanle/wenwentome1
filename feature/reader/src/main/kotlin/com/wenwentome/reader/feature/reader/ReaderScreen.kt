package com.wenwentome.reader.feature.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun ReaderScreen(
    state: ReaderUiState,
    onLocatorChanged: (String, Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("reader-screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text(
                text = state.chapterTitle ?: state.book?.title.orEmpty(),
                style = MaterialTheme.typography.titleLarge,
            )
        }
        item {
            LinearProgressIndicator(
                progress = { state.progressPercent.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (state.paragraphs.isEmpty()) {
            item {
                Text(
                    text = "暂无正文内容",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            items(state.paragraphs) { paragraph ->
                SelectionContainer {
                    Text(
                        text = paragraph,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
        item {
            Button(
                onClick = {
                    onLocatorChanged(
                        state.locator ?: state.chapterTitle ?: "chapter-1",
                        state.progressPercent,
                    )
                },
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text("保存进度")
            }
        }
    }
}
