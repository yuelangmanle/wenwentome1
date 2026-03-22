package com.wenwentome.reader.data.apihub.runtime

import com.wenwentome.reader.core.model.ApiUsageLog
import com.wenwentome.reader.data.apihub.ApiHubRepository
import java.util.UUID

class ApiCallLogger(
    private val repository: ApiHubRepository,
    private val nowProvider: () -> Long = System::currentTimeMillis,
) {
    suspend fun record(
        capabilityId: String,
        request: ApiAbilityRequest,
        outcome: ApiExecutionOutcome,
        estimatedCostMicros: Long,
    ) {
        repository.appendUsageLog(
            ApiUsageLog(
                callId = UUID.randomUUID().toString(),
                capabilityId = capabilityId,
                providerId = outcome.providerId,
                modelId = outcome.modelId,
                success = outcome.success,
                usedFallback = outcome.usedFallback,
                bookId = request.bookId,
                chapterRef = request.chapterRef,
                durationMs = outcome.durationMs,
                estimatedCostMicros = estimatedCostMicros,
                errorMessage = outcome.errorMessage,
                createdAt = nowProvider(),
            ),
        )
    }
}
