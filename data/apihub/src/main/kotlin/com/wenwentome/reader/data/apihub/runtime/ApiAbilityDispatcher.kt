package com.wenwentome.reader.data.apihub.runtime

import com.wenwentome.reader.data.apihub.ApiHubRepository
import com.wenwentome.reader.data.apihub.pricing.PriceEstimator

data class ApiDispatchResult(
    val providerId: String,
    val modelId: String,
    val outputText: String,
    val estimatedCostMicros: Long,
    val usedFallback: Boolean,
)

class ApiAbilityInvocationException(
    val statusCode: Int? = null,
    override val message: String,
) : IllegalStateException(message)

class ApiAbilityDispatcher(
    private val repository: ApiHubRepository,
    private val priceEstimator: PriceEstimator,
    private val budgetGuard: ApiBudgetGuard,
    private val fallbackExecutor: ApiFallbackExecutor,
    private val callLogger: ApiCallLogger,
) {
    suspend fun invoke(
        capabilityId: String,
        request: ApiAbilityRequest,
    ): ApiDispatchResult {
        val binding =
            repository.getCapabilityBinding(capabilityId)
                ?: throw ApiAbilityInvocationException(message = "未找到能力绑定: $capabilityId")
        val estimatedPrimaryCost =
            priceEstimator.estimate(
                providerId = binding.primaryProviderId,
                modelId = binding.primaryModelId,
                inputTokens = request.estimatedInputTokens,
                outputTokens = request.estimatedOutputTokens,
            )
        budgetGuard.ensureAllowed(estimatedPrimaryCost)
        val outcome =
            fallbackExecutor.execute(
                route =
                    ApiAbilityRoute(
                        capabilityId = capabilityId,
                        primaryProviderId = binding.primaryProviderId,
                        primaryModelId = binding.primaryModelId,
                        fallbackProviderId = binding.fallbackProviderId,
                        fallbackModelId = binding.fallbackModelId,
                    ),
                request = request,
            )
        val actualCost =
            priceEstimator.estimate(
                providerId = outcome.providerId,
                modelId = outcome.modelId,
                inputTokens = request.estimatedInputTokens,
                outputTokens = request.estimatedOutputTokens,
            )
        callLogger.record(
            capabilityId = capabilityId,
            request = request,
            outcome = outcome,
            estimatedCostMicros = actualCost,
        )
        if (!outcome.success || outcome.outputText == null) {
            throw ApiAbilityInvocationException(
                statusCode = outcome.statusCode,
                message = outcome.errorMessage ?: "调用失败",
            )
        }
        return ApiDispatchResult(
            providerId = outcome.providerId,
            modelId = outcome.modelId,
            outputText = outcome.outputText,
            estimatedCostMicros = actualCost,
            usedFallback = outcome.usedFallback,
        )
    }
}
