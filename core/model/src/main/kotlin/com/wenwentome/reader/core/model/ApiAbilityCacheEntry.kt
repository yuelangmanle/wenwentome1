package com.wenwentome.reader.core.model

data class ApiAbilityCacheEntry(
    val cacheKey: String,
    val payloadJson: String,
    val expiresAt: Long,
    val updatedAt: Long = System.currentTimeMillis(),
)
