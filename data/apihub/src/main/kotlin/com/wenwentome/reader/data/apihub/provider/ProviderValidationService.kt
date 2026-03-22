package com.wenwentome.reader.data.apihub.provider

import com.wenwentome.reader.core.model.ApiProviderProfile
import com.wenwentome.reader.core.model.ProviderModelSource

enum class ProviderValidationState { UNCHECKED, VALID, FAILED }

data class ProviderBindingProbeResult(
    val success: Boolean,
    val message: String = "",
)

data class ProviderValidationResult(
    val allowed: Boolean,
    val reason: String,
    val state: ProviderValidationState,
)

class ProviderValidationService(
    private val providerCatalogRepository: ProviderCatalogRepository,
    private val bindingProbe: suspend (ApiProviderProfile, String) -> ProviderBindingProbeResult =
        { _, modelId ->
            ProviderBindingProbeResult(
                success = false,
                message = "probe 未通过: $modelId",
            )
        },
) {
    suspend fun validateBinding(
        providerId: String,
        modelId: String,
    ): ProviderValidationResult {
        val provider =
            providerCatalogRepository.provider(providerId)
                ?: return ProviderValidationResult(
                    allowed = false,
                    reason = "Provider 不存在",
                    state = ProviderValidationState.FAILED,
                )
        val matchedModel =
            providerCatalogRepository.modelsFor(providerId).firstOrNull { model -> model.modelId == modelId }
        if (matchedModel != null && matchedModel.source != ProviderModelSource.MANUAL) {
            return ProviderValidationResult(
                allowed = true,
                reason = "已命中预设或远端发现模型",
                state = ProviderValidationState.VALID,
            )
        }
        val probeResult = bindingProbe(provider, modelId)
        return if (probeResult.success) {
            ProviderValidationResult(
                allowed = true,
                reason = probeResult.message.ifBlank { "probe 成功" },
                state = ProviderValidationState.VALID,
            )
        } else {
            ProviderValidationResult(
                allowed = false,
                reason = probeResult.message.ifBlank { "需要 probe 成功或命中预设模型" },
                state = ProviderValidationState.FAILED,
            )
        }
    }

    suspend fun validateProvider(providerId: String): ProviderValidationResult {
        val models = providerCatalogRepository.modelsFor(providerId)
        return if (models.isNotEmpty()) {
            ProviderValidationResult(
                allowed = true,
                reason = "已发现 ${models.size} 个模型",
                state = ProviderValidationState.VALID,
            )
        } else {
            ProviderValidationResult(
                allowed = false,
                reason = "暂未发现可用模型，请检查地址、密钥或手动录入模型",
                state = ProviderValidationState.FAILED,
            )
        }
    }
}
