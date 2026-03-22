package com.wenwentome.reader.feature.apihub

data class ApiHubUiState(
    val enabledProviderCount: Int = 0,
    val boundCapabilityCount: Int = 0,
    val todayCallCount: Int = 0,
    val budgetUsageRatio: Float = 0f,
    val latestError: String? = null,
)
