package com.wenwentome.reader.feature.discover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
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
    onAddToShelf: (String) -> Unit,
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
            modifier = Modifier.fillMaxWidth(),
            label = { Text("搜索网文") },
        )
        state.lastAddedTitle?.let { title ->
            Text(
                text = "已加入书库：$title",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.results, key = { it.id }) { result ->
                ListItem(
                    headlineContent = { Text(result.title) },
                    supportingContent = { Text(result.author ?: "未知作者") },
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
