package com.wenwentome.reader.feature.apihub

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun UsageLogScreen(
    entries: List<UsageLogEntryUiState>,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("api-hub-usage-log-screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "调用记录",
            style = MaterialTheme.typography.headlineSmall,
        )
        if (entries.isEmpty()) {
            Text(
                text = "还没有调用记录，接入真实能力后会展示最近 20 次调用。",
                style = MaterialTheme.typography.bodyLarge,
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(entries, key = { entry -> entry.callId }) { entry ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = entry.title,
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = entry.statusLabel,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            if (entry.detail.isNotBlank()) {
                                Text(
                                    text = entry.detail,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            Text(
                                text = "估算成本：${entry.costLabel}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                text = if (entry.usedFallback) "已触发回退模型" else "未触发回退",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }
    }
}
