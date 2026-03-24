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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import com.wenwentome.reader.bridge.source.model.RemoteSearchResult

@Composable
fun DiscoverScreen(
    state: DiscoverUiState,
    onQueryChange: (String) -> Unit,
    onSubmitSearch: () -> Unit,
    onPreview: (String) -> Unit,
    onAddToShelf: (RemoteSearchResult) -> Unit,
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = state.draftQuery,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .testTag("discover-search-input"),
                label = { Text("搜索网文") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSubmitSearch() }),
            )
            Button(
                onClick = onSubmitSearch,
                modifier = Modifier.testTag("discover-search-submit"),
            ) {
                Text("搜索")
            }
        }
        state.enhancementHint?.let { hint ->
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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
                    supportingContent = {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(result.author ?: "未知作者")
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SourceHealthBadge(
                                    score = result.healthScore,
                                    label = result.healthLabel,
                                    modifier = Modifier.testTag("source-health-badge-${result.id}"),
                                )
                                result.boostReason?.let { reason ->
                                    Text(
                                        text = reason,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                    },
                    overlineContent = {
                        if (state.selectedResultId == result.id) {
                            Text("预览中")
                        }
                    },
                    trailingContent = {
                        val isAdding = result.id in state.addingResultIds
                        Button(
                            onClick = { onAddToShelf(result.result) },
                            enabled = !isAdding,
                            modifier = Modifier.testTag("discover-result-add-${result.id}"),
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
    onAddToShelf: (RemoteSearchResult) -> Unit,
    onRefreshSelected: () -> Unit,
    onReadLatest: () -> Unit,
) {
    val selectedResult = state.selectedResult ?: return
    val selectedId = selectedResult.id
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
                text = preview?.title ?: selectedResult.title,
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = preview?.author ?: selectedResult.author ?: "未知作者",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SourceHealthBadge(
                    score = selectedResult.healthScore,
                    label = selectedResult.healthLabel,
                    modifier = Modifier.testTag("source-health-badge-preview"),
                )
                selectedResult.boostReason?.let { reason ->
                    Text(
                        text = reason,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            preview?.lastChapter?.takeIf { it.isNotBlank() }?.let { lastChapter ->
                Text(
                    text = "最新章节：$lastChapter",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            state.lastRefreshHint?.let { hint ->
                Text(
                    text = hint,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary,
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
                    onClick = { onAddToShelf(selectedResult.result) },
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
