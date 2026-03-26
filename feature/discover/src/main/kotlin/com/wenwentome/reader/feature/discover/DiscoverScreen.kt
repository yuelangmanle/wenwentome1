package com.wenwentome.reader.feature.discover

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import com.wenwentome.reader.bridge.source.model.RemoteSearchResult

private enum class DiscoverEntrance {
    SOURCE_SEARCH,
    BROWSER_FIND,
}

@Composable
fun DiscoverScreen(
    state: DiscoverUiState,
    onQueryChange: (String) -> Unit,
    onBrowserQueryChange: (String) -> Unit,
    onSubmitSearch: () -> Unit,
    onOpenBrowserSearch: () -> Unit,
    onPreview: (String) -> Unit,
    onAddToShelf: (RemoteSearchResult) -> Unit,
    onRefreshSelected: () -> Unit,
    onReadLatest: () -> Unit,
    onManageSources: () -> Unit,
    onManageSearchEngines: () -> Unit,
    onOpenBrowserSettings: () -> Unit,
    onQuickSwitchEngine: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var activeEntrance by remember { mutableStateOf(DiscoverEntrance.SOURCE_SEARCH) }
    var overflowExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "发现",
                    modifier = Modifier.testTag("screen"),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = "书源搜索和浏览器找书共用一个入口层",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            DiscoverOverflowMenu(
                expanded = overflowExpanded,
                onExpandedChange = { overflowExpanded = it },
                activeEntrance = activeEntrance,
                onManageSources = onManageSources,
                onManageSearchEngines = onManageSearchEngines,
                onOpenBrowserSettings = onOpenBrowserSettings,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = activeEntrance == DiscoverEntrance.SOURCE_SEARCH,
                onClick = { activeEntrance = DiscoverEntrance.SOURCE_SEARCH },
                label = { Text("书源搜索") },
            )
            FilterChip(
                selected = activeEntrance == DiscoverEntrance.BROWSER_FIND,
                onClick = { activeEntrance = DiscoverEntrance.BROWSER_FIND },
                label = { Text("浏览器找书") },
            )
        }
        when (activeEntrance) {
            DiscoverEntrance.SOURCE_SEARCH -> SourceSearchPane(
                state = state,
                onQueryChange = onQueryChange,
                onSubmitSearch = onSubmitSearch,
                onPreview = onPreview,
                onAddToShelf = onAddToShelf,
                onRefreshSelected = onRefreshSelected,
                onReadLatest = onReadLatest,
            )
            DiscoverEntrance.BROWSER_FIND -> BrowserFindPane(
                state = state,
                onBrowserQueryChange = onBrowserQueryChange,
                onOpenBrowserSearch = onOpenBrowserSearch,
                onManageSearchEngines = onManageSearchEngines,
                onOpenBrowserSettings = onOpenBrowserSettings,
                onQuickSwitchEngine = onQuickSwitchEngine,
            )
        }
    }
}

@Composable
private fun ColumnScope.SourceSearchPane(
    state: DiscoverUiState,
    onQueryChange: (String) -> Unit,
    onSubmitSearch: () -> Unit,
    onPreview: (String) -> Unit,
    onAddToShelf: (RemoteSearchResult) -> Unit,
    onRefreshSelected: () -> Unit,
    onReadLatest: () -> Unit,
) {
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

@Composable
private fun BrowserFindPane(
    state: DiscoverUiState,
    onBrowserQueryChange: (String) -> Unit,
    onOpenBrowserSearch: () -> Unit,
    onManageSearchEngines: () -> Unit,
    onOpenBrowserSettings: () -> Unit,
    onQuickSwitchEngine: (String) -> Unit,
) {
    var engineMenuExpanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    FilterChip(
                        selected = true,
                        onClick = { engineMenuExpanded = true },
                        label = { Text(state.browserSearchEngineLabel) },
                    )
                    DropdownMenu(
                        expanded = engineMenuExpanded,
                        onDismissRequest = { engineMenuExpanded = false },
                    ) {
                        state.browserAvailableEngines.forEach { engine ->
                            DropdownMenuItem(
                                text = { Text(engine.label) },
                                onClick = {
                                    engineMenuExpanded = false
                                    onQuickSwitchEngine(engine.id)
                                },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("更多搜索引擎…") },
                            onClick = {
                                engineMenuExpanded = false
                                onManageSearchEngines()
                            },
                        )
                    }
                }
                OutlinedTextField(
                    value = state.browserDraftQuery,
                    onValueChange = onBrowserQueryChange,
                    modifier = Modifier.weight(1f),
                    label = { Text("浏览器找书") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onOpenBrowserSearch() }),
                )
                Button(
                    onClick = onOpenBrowserSearch,
                    modifier = Modifier.testTag("discover-browser-search-submit"),
                ) {
                    Text("前往")
                )
            }
            Text(
                text = "进入网页后可识别正文区域，并切换为优化阅读视图。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "右上角菜单可进入搜索引擎管理、浏览器模式与自动优化阅读设置。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("网页识别预览", style = MaterialTheme.typography.titleMedium)
                    Text("当前搜索引擎：${state.browserSearchEngineLabel}", style = MaterialTheme.typography.bodyMedium)
                    Text("浏览器模式：${state.browserModeLabel}", style = MaterialTheme.typography.bodyMedium)
                    Text("自动优化阅读：${state.autoOptimizeReadingLabel}", style = MaterialTheme.typography.bodyMedium)
                    Text("手动优化入口：悬浮按钮", style = MaterialTheme.typography.bodyMedium)
                    OutlinedButton(
                        onClick = onOpenBrowserSettings,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("打开浏览器找书设置")
                    }
                }
            }
        }
    }
}

@Composable
private fun DiscoverOverflowMenu(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    activeEntrance: DiscoverEntrance,
    onManageSources: () -> Unit,
    onManageSearchEngines: () -> Unit,
    onOpenBrowserSettings: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.End) {
        Text(
            text = "...",
            modifier = Modifier
                .testTag("discover-overflow-menu")
                .clickable { onExpandedChange(true) }
                .padding(top = 4.dp),
            style = MaterialTheme.typography.headlineMedium,
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
        ) {
            if (activeEntrance == DiscoverEntrance.SOURCE_SEARCH) {
                DropdownMenuItem(
                    text = { Text("书源管理") },
                    onClick = {
                        onExpandedChange(false)
                        onManageSources()
                    },
                )
                DropdownMenuItem(
                    text = { Text("导入书源") },
                    onClick = {
                        onExpandedChange(false)
                        onManageSources()
                    },
                )
                DropdownMenuItem(
                    text = { Text("启用 / 禁用书源") },
                    onClick = {
                        onExpandedChange(false)
                        onManageSources()
                    },
                )
            } else {
                DropdownMenuItem(
                    text = { Text("搜索引擎管理") },
                    onClick = {
                        onExpandedChange(false)
                        onManageSearchEngines()
                    },
                )
                DropdownMenuItem(
                    text = { Text("自定义搜索引擎") },
                    onClick = {
                        onExpandedChange(false)
                        onManageSearchEngines()
                    },
                )
                DropdownMenuItem(
                    text = { Text("浏览器模式") },
                    onClick = {
                        onExpandedChange(false)
                        onOpenBrowserSettings()
                    },
                )
                DropdownMenuItem(
                    text = { Text("优化阅读设置") },
                    onClick = {
                        onExpandedChange(false)
                        onOpenBrowserSettings()
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
