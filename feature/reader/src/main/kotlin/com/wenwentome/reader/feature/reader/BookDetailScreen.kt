package com.wenwentome.reader.feature.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.wenwentome.reader.core.model.BookRecord

@Composable
fun BookDetailScreen(
    book: BookRecord,
    onReadClick: () -> Unit,
    onSyncClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("book-detail"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = book.title,
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = book.author ?: "未知作者",
            style = MaterialTheme.typography.bodyMedium,
        )
        book.summary?.takeIf { it.isNotBlank() }?.let { summary ->
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onReadClick) {
                Text("开始阅读")
            }
            OutlinedButton(onClick = onSyncClick) {
                Text("立即同步")
            }
        }
    }
}
