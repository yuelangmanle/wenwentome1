package com.wenwentome.reader.feature.discover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SourceManagementScreen(
    state: SourceManagementUiState,
    onImportJson: () -> Unit,
    onToggleSource: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(onClick = onImportJson) {
                Text("导入")
            }
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(
                top = paddingValues.calculateTopPadding() + 16.dp,
                bottom = paddingValues.calculateBottomPadding() + 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text(
                    text = "书源管理",
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
            if (state.sources.isEmpty()) {
                item {
                    Text(
                        text = "暂无书源，点击右下角导入",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else {
                items(state.sources, key = { it.sourceId }) { source ->
                    ListItem(
                        modifier = Modifier.fillMaxWidth(),
                        headlineContent = { Text(source.sourceName) },
                        supportingContent = { Text(source.group ?: "未分组") },
                        trailingContent = {
                            Switch(
                                checked = source.enabled,
                                onCheckedChange = { onToggleSource(source.sourceId) },
                            )
                        },
                    )
                }
            }
        }
    }
}
