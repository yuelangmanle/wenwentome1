package com.wenwentome.reader.core.database.entity

import androidx.room.Entity
import com.wenwentome.reader.core.model.ApiModelCostLevel
import com.wenwentome.reader.core.model.ApiModelValidationState
import com.wenwentome.reader.core.model.ProviderModelSource

@Entity(
    tableName = "api_models",
    primaryKeys = ["providerId", "modelId"],
)
data class ApiModelEntity(
    val providerId: String,
    val modelId: String,
    val label: String,
    val capabilities: List<String> = emptyList(),
    val costLevel: ApiModelCostLevel = ApiModelCostLevel.MEDIUM,
    val contextWindow: Int? = null,
    val supportsStreaming: Boolean = true,
    val inputPricePer1kMicros: Long? = null,
    val outputPricePer1kMicros: Long? = null,
    val requestPricePerCallMicros: Long? = null,
    val voiceOptions: List<String> = emptyList(),
    val maxRequestsPerMinute: Int? = null,
    val source: ProviderModelSource = ProviderModelSource.MANUAL,
    val validationState: ApiModelValidationState = ApiModelValidationState.UNVERIFIED,
    val updatedAt: Long = System.currentTimeMillis(),
)
