package com.wenwentome.reader.feature.discover

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun DiscoverScreen(
    state: DiscoverUiState,
    onSearch: (String) -> Unit,
    onPreview: (String) -> Unit,
    onAddToShelf: (String) -> Unit,
    onRefreshSelected: () -> Unit,
    onReadLatest: () -> Unit,
    onManageSources: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "发现",
            modifier = Modifier.testTag("screen"),
            style = MaterialTheme.typography.headlineSmall,
        )
        OutlinedButton(onClick = onManageSources) {
            Text("书源管理")
        }
        OutlinedTextField(
            value = state.query,
            onValueChange = onSearch,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("discover-search-input"),
            label = { Text("搜索网文") },
        )
        state.lastAddedTitle?.let { title ->
            Text(
                text = "已加入书库：$title",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        SelectedPreviewCard(
            state = state,
            onAddToShelf = onAddToShelf,
            onRefreshSelected = onRefreshSelected,
            onReadLatest = onReadLatest,
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.results, key = { it.id }) { result ->
                ListItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPreview(result.id) }
                        .testTag("discover-result-${result.id}"),
                    headlineContent = { Text(result.title) },
                    supportingContent = { Text(result.author ?: "未知作者") },
                    overlineContent = {
                        if (state.selectedResultId == result.id) {
                            Text("预览中")
                        }
                    },
                    trailingContent = {
                        val isAdding = result.id in state.addingResultIds
                        Button(
                            onClick = { onAddToShelf(result.id) },
                            enabled = !isAdding,
                        ) {
                            Text(if (isAdding) "加入中" else "加入书库")
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun SelectedPreviewCard(
    state: DiscoverUiState,
    onAddToShelf: (String) -> Unit,
    onRefreshSelected: () -> Unit,
    onReadLatest: () -> Unit,
) {
    val selectedId = state.selectedResultId ?: return
    val preview = state.selectedPreview
    val isAdding = selectedId in state.addingResultIds
    val isRefreshing = selectedId in state.refreshingResultIds

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("discover-selected-preview"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = preview?.title ?: "预览加载中",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = preview?.author ?: "未知作者",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            preview?.lastChapter?.takeIf { it.isNotBlank() }?.let { lastChapter ->
                Text(
                    text = "最新章节：$lastChapter",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            preview?.summary?.takeIf { it.isNotBlank() }?.let { summary ->
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = { onAddToShelf(selectedId) },
                    enabled = !isAdding,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("discover-preview-add-button"),
                ) {
                    Text(if (isAdding) "加入中" else "加入书库")
                }
                OutlinedButton(
                    onClick = onRefreshSelected,
                    enabled = !isRefreshing,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (isRefreshing) "刷新中" else "刷新目录")
                }
            }
            Button(
                onClick = onReadLatest,
                enabled = !isRefreshing,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("discover-read-latest-button"),
            ) {
                Text("阅读最新")
            }
        }
    }
}
