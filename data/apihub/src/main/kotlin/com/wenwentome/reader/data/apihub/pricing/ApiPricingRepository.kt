package com.wenwentome.reader.data.apihub.pricing

import com.wenwentome.reader.core.model.ApiModelProfile
import com.wenwentome.reader.core.model.ApiPriceOverride
import com.wenwentome.reader.data.apihub.ApiHubRepository

data class ApiResolvedPrice(
    val providerId: String,
    val modelId: String,
    val inputPricePer1kMicros: Long? = null,
    val outputPricePer1kMicros: Long? = null,
    val requestPricePerCallMicros: Long? = null,
)

class ApiPricingRepository(
    private val repository: ApiHubRepository,
) {
    suspend fun resolve(
        providerId: String,
        modelId: String,
    ): ApiResolvedPrice? {
        val model =
            repository.getModels(providerId)
                .firstOrNull { candidate -> candidate.modelId == modelId }
                ?: return null
        val override = repository.findPriceOverride(providerId, modelId)
        return model.toResolvedPrice(override)
    }

    suspend fun catalog(): List<ApiResolvedPrice> =
        repository.getProviders()
            .flatMap { provider ->
                repository.getModels(provider.providerId).map { model ->
                    val override = repository.findPriceOverride(provider.providerId, model.modelId)
                    model.toResolvedPrice(override)
                }
            }
            .sortedWith(compareBy(ApiResolvedPrice::providerId, ApiResolvedPrice::modelId))
}

private fun ApiModelProfile.toResolvedPrice(override: ApiPriceOverride?): ApiResolvedPrice =
    ApiResolvedPrice(
        providerId = providerId,
        modelId = modelId,
        inputPricePer1kMicros = override?.inputPricePer1kMicros ?: inputPricePer1kMicros,
        outputPricePer1kMicros = override?.outputPricePer1kMicros ?: outputPricePer1kMicros,
        requestPricePerCallMicros = override?.requestPricePerCallMicros ?: requestPricePerCallMicros,
    )
