package com.wenwentome.reader.feature.apihub

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun ApiHubOverviewScreen(
    state: ApiHubUiState,
    onOpenProviders: () -> Unit,
    onOpenBindings: () -> Unit,
    onOpenBudgets: () -> Unit,
    onOpenPrices: () -> Unit,
    onOpenUsageLogs: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("api-hub-overview-screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "API 中心",
            style = MaterialTheme.typography.headlineSmall,
        )
        OverviewMetricCard(label = "已启用 Provider", value = state.enabledProviderCount.toString())
        OverviewMetricCard(label = "能力绑定", value = state.boundCapabilityCount.toString())
        OverviewMetricCard(label = "今日调用", value = state.todayCallCount.toString())
        OverviewMetricCard(
            label = "预算使用",
            value = "${(state.budgetUsageRatio * 100).roundToInt()}%",
        )
        state.latestError?.let { message ->
            OverviewMetricCard(label = "最近错误", value = message)
        }
        Button(
            onClick = onOpenProviders,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("api-hub-open-providers"),
        ) {
            Text("Provider 管理")
        }
        Button(
            onClick = onOpenBindings,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("api-hub-open-bindings"),
        ) {
            Text("模型绑定")
        }
        Button(
            onClick = onOpenBudgets,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("api-hub-open-budgets"),
        ) {
            Text("预算与回退")
        }
        Button(
            onClick = onOpenPrices,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("api-hub-open-prices"),
        ) {
            Text("价格目录")
        }
        Button(
            onClick = onOpenUsageLogs,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("api-hub-open-usage-logs"),
        ) {
            Text("调用记录")
        }
    }
}

@Composable
private fun OverviewMetricCard(
    label: String,
    value: String,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}
