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
fun PriceCatalogScreen(
    entries: List<PriceCatalogEntryUiState>,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("api-hub-price-catalog-screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "价格目录",
            style = MaterialTheme.typography.headlineSmall,
        )
        if (entries.isEmpty()) {
            Text(
                text = "暂时还没有可展示的模型价格。",
                style = MaterialTheme.typography.bodyLarge,
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(entries, key = { entry -> "${entry.providerId}:${entry.modelId}" }) { entry ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = entry.modelLabel,
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = "${entry.providerLabel} · ${entry.modelId}",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                text = "输入单价：${entry.inputPriceLabel}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                text = "输出单价：${entry.outputPriceLabel}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                text = "单次请求：${entry.requestPriceLabel}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }
    }
}
