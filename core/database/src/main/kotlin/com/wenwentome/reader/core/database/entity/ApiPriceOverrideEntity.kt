package com.wenwentome.reader.core.database.entity

import androidx.room.Entity

@Entity(
    tableName = "api_price_overrides",
    primaryKeys = ["providerId", "modelId"],
)
data class ApiPriceOverrideEntity(
    val providerId: String,
    val modelId: String,
    val inputPricePer1kMicros: Long? = null,
    val outputPricePer1kMicros: Long? = null,
    val requestPricePerCallMicros: Long? = null,
    val updatedAt: Long = System.currentTimeMillis(),
)
