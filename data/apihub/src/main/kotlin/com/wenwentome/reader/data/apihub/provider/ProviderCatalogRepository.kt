package com.wenwentome.reader.data.apihub.provider

import com.wenwentome.reader.data.apihub.ApiHubStore
import com.wenwentome.reader.core.model.ApiModelProfile
import com.wenwentome.reader.core.model.ApiProviderProfile
import com.wenwentome.reader.core.model.ProviderModelSource

class ProviderCatalogRepository(
    private val apiHubRepository: ApiHubStore,
    private val modelDiscoveryService: ModelDiscoveryService,
    private val templates: List<ProviderTemplate> = ProviderTemplate.defaults(),
) {
    private val templateById = templates.associateBy { it.templateId }

    fun templates(): List<ProviderTemplate> = templates

    fun templateFor(providerId: String): ProviderTemplate? = templateById[providerId]

    fun templateLabelFor(provider: ApiProviderProfile): String =
        templateFor(provider.providerId)?.label ?: provider.providerKind.name.replace('_', ' ')

    suspend fun provider(providerId: String): ApiProviderProfile? = apiHubRepository.getProvider(providerId)

    suspend fun modelsFor(providerId: String): List<ApiModelProfile> {
        val provider = provider(providerId) ?: return emptyList()
        val presetModels = templateFor(providerId).orEmptyModels(providerId)
        val discoveredModels =
            if (provider.modelSource == ProviderModelSource.REMOTE_DISCOVERY) {
                modelDiscoveryService.discover(provider).map { model ->
                    model.copy(
                        providerId = providerId,
                        source = ProviderModelSource.REMOTE_DISCOVERY,
                    )
                }
            } else {
                emptyList()
            }
        val manualModels =
            apiHubRepository.getModels(providerId)
                .filter { model -> model.source == ProviderModelSource.MANUAL }
        val merged = linkedMapOf<String, ApiModelProfile>()
        presetModels.forEach { model -> merged[model.modelId] = model }
        discoveredModels.forEach { model -> merged[model.modelId] = model }
        manualModels.forEach { model -> merged[model.modelId] = model }
        return merged.values.toList()
    }

    fun createProviderFromTemplate(
        templateId: String,
        providerId: String = templateId,
        displayName: String = templateFor(templateId)?.label ?: templateId,
    ): ApiProviderProfile? =
        templateById[templateId]?.toProviderProfile(
            providerId = providerId,
            displayName = displayName,
        )

    private fun ProviderTemplate?.orEmptyModels(providerId: String): List<ApiModelProfile> =
        this?.presetModelProfiles(providerId).orEmpty()
}
