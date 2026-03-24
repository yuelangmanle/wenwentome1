package com.wenwentome.reader.feature.apihub

import com.wenwentome.reader.data.apihub.sync.PendingSyncConflict

data class ApiHubUiState(
    val providerStatusLabel: String = "还没有可用接口",
    val bindingStatusLabel: String = "还没有能力绑定",
    val usageStatusLabel: String = "今天还没有调用记录",
    val enabledProviderCount: Int = 0,
    val boundCapabilityCount: Int = 0,
    val todayCallCount: Int = 0,
    val budgetUsageRatio: Float = 0f,
    val latestError: String? = null,
    val budgetSummary: BudgetSummaryUiState = BudgetSummaryUiState(),
    val priceCatalogEntries: List<PriceCatalogEntryUiState> = emptyList(),
    val usageLogEntries: List<UsageLogEntryUiState> = emptyList(),
    val providerCards: List<ProviderCardUiState> = emptyList(),
    val capabilityBindings: List<CapabilityBindingCardUiState> = emptyList(),
    val selectedProviderId: String? = null,
    val selectedProviderModels: List<ModelOptionUiState> = emptyList(),
    val bindingValidationMessage: String? = null,
    val pendingConflict: PendingSyncConflict? = null,
)

data class BudgetSummaryUiState(
    val dailyLimitLabel: String = "未配置",
    val monthlyLimitLabel: String = "未配置",
    val todaySpentLabel: String = "0 元",
    val projectedUsageLabel: String = "0%",
    val overBudgetActionLabel: String = "按需确认",
    val fallbackEnabled: Boolean = true,
)

data class PriceCatalogEntryUiState(
    val providerId: String,
    val providerLabel: String,
    val modelId: String,
    val modelLabel: String,
    val inputPriceLabel: String,
    val outputPriceLabel: String,
    val requestPriceLabel: String,
)

data class UsageLogEntryUiState(
    val callId: String,
    val title: String,
    val detail: String,
    val costLabel: String,
    val statusLabel: String,
    val usedFallback: Boolean,
)

data class ProviderCardUiState(
    val providerId: String,
    val displayName: String,
    val templateLabel: String,
    val modelSourceLabel: String,
    val validationLabel: String,
    val enabled: Boolean,
    val availableModelCount: Int,
)

data class ModelOptionUiState(
    val modelId: String,
    val label: String,
    val sourceLabel: String,
    val validationLabel: String,
)

data class CapabilityBindingCardUiState(
    val capabilityId: String,
    val primaryLabel: String,
    val fallbackLabel: String? = null,
    val enabled: Boolean = true,
)
