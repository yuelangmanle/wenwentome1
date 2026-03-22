package com.wenwentome.reader.data.apihub.pricing

class PriceEstimator(
    private val pricingRepository: ApiPricingRepository,
) {
    suspend fun estimate(
        providerId: String,
        modelId: String,
        inputTokens: Int,
        outputTokens: Int,
    ): Long {
        val price = pricingRepository.resolve(providerId, modelId) ?: return 0L
        return estimate(price, inputTokens, outputTokens)
    }

    fun estimate(
        price: ApiResolvedPrice,
        inputTokens: Int,
        outputTokens: Int,
    ): Long =
        scaledTokenCost(inputTokens, price.inputPricePer1kMicros) +
            scaledTokenCost(outputTokens, price.outputPricePer1kMicros) +
            (price.requestPricePerCallMicros ?: 0L)

    private fun scaledTokenCost(tokens: Int, pricePer1kMicros: Long?): Long {
        val unitPrice = pricePer1kMicros ?: return 0L
        if (tokens <= 0) return 0L
        return ((tokens.toLong() * unitPrice) + 999L) / 1_000L
    }
}
