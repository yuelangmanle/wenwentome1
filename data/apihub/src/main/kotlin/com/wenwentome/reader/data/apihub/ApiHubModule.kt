package com.wenwentome.reader.data.apihub

data class ApiHubOverviewSnapshot(
    val enabledProviderCount: Int = 0,
    val boundCapabilityCount: Int = 0,
    val todayCallCount: Int = 0,
    val budgetUsageRatio: Float = 0f,
    val latestError: String? = null,
)

class ApiHubModule {
    fun loadOverviewSnapshot(): ApiHubOverviewSnapshot = ApiHubOverviewSnapshot()
}
