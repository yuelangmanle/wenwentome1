package com.wenwentome.reader.feature.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun LibraryScreen(
    state: LibraryUiState,
    onImportClick: () -> Unit,
    onContinueReadingClick: (String) -> Unit,
    onBookClick: (String) -> Unit,
    onRefreshCatalog: (String) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
) {
    var actionTarget by remember { mutableStateOf<LibraryBookItem?>(null) }

    Scaffold(
        modifier = modifier.testTag("library-screen"),
        floatingActionButton = {
            FloatingActionButton(onClick = onImportClick) {
                Text("导入")
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("library-hero-section")
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "书库",
                    modifier = Modifier.testTag("screen"),
                    style = MaterialTheme.typography.headlineSmall,
                )
                state.continueReading?.let { item ->
                    ContinueReadingCard(
                        item = item,
                        onClick = { onContinueReadingClick(item.book.id) },
                        modifier = Modifier.testTag("continue-reading-card"),
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 132.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("library-grid-section"),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = 8.dp,
                    end = 16.dp,
                    bottom = contentPadding.calculateBottomPadding(),
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (state.visibleBooks.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = "暂无书籍",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                } else {
                    items(
                        items = state.visibleBooks,
                        key = { it.book.id },
                    ) { item ->
                        BookCoverCard(
                            item = item,
                            onClick = { onBookClick(item.book.id) },
                            onLongClick = { actionTarget = item },
                            modifier = Modifier.testTag("book-cover-card-${item.book.id}"),
                        )
                    }
                }
            }
        }

        BookActionsMenu(
            visible = actionTarget != null,
            onDismiss = { actionTarget = null },
            onOpenDetail = { actionTarget?.let { onBookClick(it.book.id) } },
            onRefreshCatalog = actionTarget?.takeIf { it.book.originType != com.wenwentome.reader.core.model.OriginType.LOCAL }?.let { item ->
                { onRefreshCatalog(item.book.id) }
            },
        )
    }
}
