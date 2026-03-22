package com.wenwentome.reader.core.model

enum class ApiModelCostLevel { LOW, MEDIUM, HIGH }

enum class ApiModelValidationState { UNVERIFIED, VALID, FAILED }

data class ApiModelProfile(
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
