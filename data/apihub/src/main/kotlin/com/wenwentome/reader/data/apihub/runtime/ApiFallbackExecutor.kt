package com.wenwentome.reader.data.apihub.runtime

data class ApiAbilityRoute(
    val capabilityId: String,
    val primaryProviderId: String,
    val primaryModelId: String,
    val fallbackProviderId: String? = null,
    val fallbackModelId: String? = null,
)

data class ApiAbilityRequest(
    val body: String,
    val bookId: String? = null,
    val chapterRef: String? = null,
    val estimatedInputTokens: Int = 0,
    val estimatedOutputTokens: Int = 0,
)

sealed interface ApiAbilityExecutionResult {
    data class Success(
        val outputText: String,
        val durationMs: Long? = null,
    ) : ApiAbilityExecutionResult

    data class Failure(
        val statusCode: Int? = null,
        val message: String,
        val durationMs: Long? = null,
    ) : ApiAbilityExecutionResult
}

interface ApiAbilityExecutor {
    suspend fun execute(
        providerId: String,
        modelId: String,
        request: ApiAbilityRequest,
    ): ApiAbilityExecutionResult
}

data class ApiExecutionOutcome(
    val providerId: String,
    val modelId: String,
    val success: Boolean,
    val outputText: String? = null,
    val usedFallback: Boolean = false,
    val errorMessage: String? = null,
    val statusCode: Int? = null,
    val durationMs: Long? = null,
)

class NoOpApiAbilityExecutor : ApiAbilityExecutor {
    override suspend fun execute(
        providerId: String,
        modelId: String,
        request: ApiAbilityRequest,
    ): ApiAbilityExecutionResult =
        ApiAbilityExecutionResult.Failure(
            statusCode = 501,
            message = "尚未接入远端执行器",
        )
}

class ApiFallbackExecutor(
    private val abilityExecutor: ApiAbilityExecutor,
) {
    suspend fun execute(
        route: ApiAbilityRoute,
        request: ApiAbilityRequest,
    ): ApiExecutionOutcome {
        val primary =
            abilityExecutor.execute(
                providerId = route.primaryProviderId,
                modelId = route.primaryModelId,
                request = request,
            )
        if (primary is ApiAbilityExecutionResult.Success) {
            return ApiExecutionOutcome(
                providerId = route.primaryProviderId,
                modelId = route.primaryModelId,
                success = true,
                outputText = primary.outputText,
                durationMs = primary.durationMs,
            )
        }

        val primaryFailure = primary as ApiAbilityExecutionResult.Failure
        val fallbackProviderId = route.fallbackProviderId
        val fallbackModelId = route.fallbackModelId
        if (fallbackProviderId == null || fallbackModelId == null || !primaryFailure.isRetryable()) {
            return ApiExecutionOutcome(
                providerId = route.primaryProviderId,
                modelId = route.primaryModelId,
                success = false,
                usedFallback = false,
                errorMessage = primaryFailure.message,
                statusCode = primaryFailure.statusCode,
                durationMs = primaryFailure.durationMs,
            )
        }

        return when (
            val fallback =
                abilityExecutor.execute(
                    providerId = fallbackProviderId,
                    modelId = fallbackModelId,
                    request = request,
                )
        ) {
            is ApiAbilityExecutionResult.Success ->
                ApiExecutionOutcome(
                    providerId = fallbackProviderId,
                    modelId = fallbackModelId,
                    success = true,
                    outputText = fallback.outputText,
                    usedFallback = true,
                    durationMs = fallback.durationMs,
                )

            is ApiAbilityExecutionResult.Failure ->
                ApiExecutionOutcome(
                    providerId = fallbackProviderId,
                    modelId = fallbackModelId,
                    success = false,
                    usedFallback = true,
                    errorMessage = fallback.message,
                    statusCode = fallback.statusCode,
                    durationMs = fallback.durationMs,
                )
        }
    }
}

private fun ApiAbilityExecutionResult.Failure.isRetryable(): Boolean =
    statusCode == 429 || (statusCode != null && statusCode >= 500)
