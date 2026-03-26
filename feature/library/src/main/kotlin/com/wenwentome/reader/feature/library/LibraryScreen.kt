package com.wenwentome.reader.feature.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun LibraryScreen(
    state: LibraryUiState,
    onImportClick: () -> Unit,
    onOpenCacheManager: () -> Unit,
    onOpenBatchManage: () -> Unit,
    onContinueReadingClick: (String) -> Unit,
    onBookClick: (String) -> Unit,
    onRefreshCatalog: (String) -> Unit,
    onRefreshCover: (String) -> Unit,
    onImportPhoto: (String) -> Unit,
    onRestoreAutomaticCover: (String) -> Unit,
    onFilterChange: (LibraryFilter) -> Unit,
    onSortChange: (LibrarySort) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
) {
    var actionTarget by remember { mutableStateOf<LibraryBookItem?>(null) }
    var overflowExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("library-screen"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("library-hero-section")
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "书架",
                        modifier = Modifier.testTag("screen"),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        text = "本地书与缓存下载统一管理",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                LibraryOverflowMenu(
                    expanded = overflowExpanded,
                    onExpandedChange = { overflowExpanded = it },
                    onImportClick = onImportClick,
                    onOpenCacheManager = onOpenCacheManager,
                    onOpenBatchManage = onOpenBatchManage,
                )
            }
            if (state.isImporting) {
                Text(
                    text = "正在导入书籍…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            state.importErrorMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            state.continueReading?.let { item ->
                ContinueReadingCard(
                    item = item,
                    onClick = { onContinueReadingClick(item.book.id) },
                    modifier = Modifier.testTag("continue-reading-card"),
                )
            }
            LibraryShelfControls(
                selectedFilter = state.filter,
                selectedSort = state.sort,
                onFilterChange = onFilterChange,
                onSortChange = onSortChange,
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 156.dp),
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
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

        BookActionsMenu(
            visible = actionTarget != null,
            onDismiss = { actionTarget = null },
            onOpenDetail = { actionTarget?.let { onBookClick(it.book.id) } },
            onRefreshCatalog = actionTarget?.takeIf { it.book.originType != com.wenwentome.reader.core.model.OriginType.LOCAL }?.let { item ->
                { onRefreshCatalog(item.book.id) }
            },
            onRefreshCover = actionTarget?.let { item ->
                { onRefreshCover(item.book.id) }
            },
            onImportPhoto = actionTarget?.let { item ->
                { onImportPhoto(item.book.id) }
            },
            onRestoreAutomaticCover = actionTarget?.takeIf { it.canRestoreAutomaticCover }?.let { item ->
                { onRestoreAutomaticCover(item.book.id) }
            },
        )
    }
}

@Composable
private fun LibraryOverflowMenu(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onImportClick: () -> Unit,
    onOpenCacheManager: () -> Unit,
    onOpenBatchManage: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.End) {
        Text(
            text = "...",
            modifier = Modifier
                .testTag("library-overflow-menu")
                .padding(top = 4.dp)
                .clickable { onExpandedChange(true) },
            style = MaterialTheme.typography.headlineMedium,
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
        ) {
            DropdownMenuItem(
                text = { Text("导入书籍") },
                onClick = {
                    onExpandedChange(false)
                    onImportClick()
                },
            )
            DropdownMenuItem(
                text = { Text("下载缓存管理") },
                onClick = {
                    onExpandedChange(false)
                    onOpenCacheManager()
                },
            )
            DropdownMenuItem(
                text = { Text("批量管理") },
                onClick = {
                    onExpandedChange(false)
                    onOpenBatchManage()
                },
            )
            DropdownMenuItem(
                text = { Text("排序与筛选") },
                onClick = { onExpandedChange(false) },
            )
        }
    }
}

@Composable
private fun LibraryShelfControls(
    selectedFilter: LibraryFilter,
    selectedSort: LibrarySort,
    onFilterChange: (LibraryFilter) -> Unit,
    onSortChange: (LibrarySort) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = selectedFilter == LibraryFilter.DEFAULT,
                onClick = { onFilterChange(LibraryFilter.DEFAULT) },
                label = { Text("全部") },
            )
            FilterChip(
                selected = selectedFilter == LibraryFilter.LOCAL_ONLY,
                onClick = { onFilterChange(LibraryFilter.LOCAL_ONLY) },
                label = { Text("本地") },
            )
            FilterChip(
                selected = selectedFilter == LibraryFilter.WEB_ONLY,
                onClick = { onFilterChange(LibraryFilter.WEB_ONLY) },
                label = { Text("网文") },
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LibrarySort.entries.forEach { sort ->
                FilterChip(
                    selected = selectedSort == sort,
                    onClick = { onSortChange(sort) },
                    label = { Text(sort.label) },
                )
            }
        }
    }
}
