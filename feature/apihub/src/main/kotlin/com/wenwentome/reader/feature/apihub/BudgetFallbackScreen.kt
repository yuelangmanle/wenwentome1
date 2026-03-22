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
fun BudgetFallbackScreen(
    summary: BudgetSummaryUiState,
) {
    val summaryItems =
        listOf(
            "日预算上限" to summary.dailyLimitLabel,
            "月预算上限" to summary.monthlyLimitLabel,
            "今日已用" to summary.todaySpentLabel,
            "预计用量" to summary.projectedUsageLabel,
            "超限动作" to summary.overBudgetActionLabel,
            "失败回退" to if (summary.fallbackEnabled) "已开启" else "已关闭",
        )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("api-hub-budget-placeholder-screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "预算与回退",
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = "这里集中展示预算保护和失败回退策略，方便后续接入真实调度器时直接复用。",
            style = MaterialTheme.typography.bodyMedium,
        )
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(summaryItems, key = { item -> item.first }) { item ->
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = item.first,
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            text = item.second,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        }
    }
}
