package com.wenwentome.reader.core.model

data class ApiUsageLog(
    val callId: String,
    val capabilityId: String,
    val providerId: String,
    val modelId: String,
    val success: Boolean,
    val usedFallback: Boolean = false,
    val bookId: String? = null,
    val chapterRef: String? = null,
    val durationMs: Long? = null,
    val estimatedCostMicros: Long = 0L,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
