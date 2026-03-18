package com.wenwentome.reader.feature.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.wenwentome.reader.core.model.BookRecord

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
) {
    val uiState by viewModel.uiState.collectAsState(initial = LibraryUiState())

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .testTag("library-screen"),
    ) {
        Text(
            text = "书库",
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (uiState.visibleBooks.isEmpty()) {
            Text(
                text = "暂无书籍",
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            uiState.visibleBooks.forEach { book ->
                BookCard(
                    book = book,
                    modifier = Modifier
                        .padding(vertical = 6.dp)
                        .testTag("book-${book.id}"),
                )
            }
        }
    }
}

@Composable
private fun BookCard(
    book: BookRecord,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = book.title,
                style = MaterialTheme.typography.titleMedium,
            )
            book.author?.takeIf { it.isNotBlank() }?.let { author ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = author,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

