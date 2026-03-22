package com.wenwentome.reader.core.model

data class ApiPriceOverride(
    val providerId: String,
    val modelId: String,
    val inputPricePer1kMicros: Long? = null,
    val outputPricePer1kMicros: Long? = null,
    val requestPricePerCallMicros: Long? = null,
    val updatedAt: Long = System.currentTimeMillis(),
)
