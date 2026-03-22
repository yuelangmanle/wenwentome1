package com.wenwentome.reader.data.apihub

import com.wenwentome.reader.data.apihub.provider.ProviderCatalogRepository
import com.wenwentome.reader.data.apihub.provider.ProviderValidationService
import com.wenwentome.reader.data.apihub.sync.ApiHubMergeResolver
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.time.ZoneId

data class ApiHubOverviewSnapshot(
    val enabledProviderCount: Int = 0,
    val boundCapabilityCount: Int = 0,
    val todayCallCount: Int = 0,
    val budgetUsageRatio: Float = 0f,
    val latestError: String? = null,
)

class ApiHubModule(
    val repository: ApiHubStore,
    val providerCatalogRepository: ProviderCatalogRepository,
    val providerValidationService: ProviderValidationService,
    val mergeResolver: ApiHubMergeResolver,
) {
    fun loadOverviewSnapshot(): ApiHubOverviewSnapshot =
        runBlocking {
            val providers = repository.getProviders()
            val bindings = repository.getCapabilityBindings()
            val usageLogs = repository.getUsageLogs()
            val todayStart = startOfDay(System.currentTimeMillis())
            val todayLogs = usageLogs.filter { log -> log.createdAt >= todayStart }
            val todaySpent = todayLogs.sumOf { log -> log.estimatedCostMicros }
            val dailyLimit =
                (repository as? ApiHubRepository)
                    ?.getBudgetPolicy()
                    ?.dailyLimitMicros
            ApiHubOverviewSnapshot(
                enabledProviderCount = providers.count { provider -> provider.enabled },
                boundCapabilityCount = bindings.count { binding -> binding.enabled },
                todayCallCount = todayLogs.size,
                budgetUsageRatio =
                    dailyLimit
                        ?.takeIf { limit -> limit > 0L }
                        ?.let { limit -> (todaySpent.toFloat() / limit.toFloat()).coerceIn(0f, 1f) }
                        ?: 0f,
                latestError = usageLogs.firstOrNull { log -> !log.success }?.errorMessage,
            )
        }

    private fun startOfDay(timestamp: Long): Long {
        val zone = ZoneId.systemDefault()
        return Instant.ofEpochMilli(timestamp)
            .atZone(zone)
            .toLocalDate()
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()
    }
}
