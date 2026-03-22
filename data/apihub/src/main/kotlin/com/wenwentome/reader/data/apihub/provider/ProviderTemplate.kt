package com.wenwentome.reader.data.apihub.provider

import com.wenwentome.reader.core.model.ApiModelProfile
import com.wenwentome.reader.core.model.ApiModelValidationState
import com.wenwentome.reader.core.model.ApiProviderProfile
import com.wenwentome.reader.core.model.ProviderAuthScheme
import com.wenwentome.reader.core.model.ProviderKind
import com.wenwentome.reader.core.model.ProviderModelSource
import com.wenwentome.reader.core.model.ProviderTransportStyle

data class ProviderTemplate(
    val templateId: String,
    val label: String,
    val providerKind: ProviderKind,
    val baseUrl: String,
    val apiStyle: ProviderTransportStyle,
    val authScheme: ProviderAuthScheme,
    val defaultModelSource: ProviderModelSource = ProviderModelSource.REMOTE_DISCOVERY,
    val presetModels: List<ProviderTemplateModel> = emptyList(),
    val supportsChat: Boolean = true,
    val supportsTts: Boolean = false,
    val supportsEmbeddings: Boolean = false,
    val supportsImage: Boolean = false,
) {
    fun toProviderProfile(
        providerId: String = templateId,
        displayName: String = label,
    ): ApiProviderProfile =
        ApiProviderProfile(
            providerId = providerId,
            displayName = displayName,
            providerKind = providerKind,
            baseUrl = baseUrl,
            apiStyle = apiStyle,
            authScheme = authScheme,
            modelSource = defaultModelSource,
            supportsChat = supportsChat,
            supportsTts = supportsTts,
            supportsEmbeddings = supportsEmbeddings,
            supportsImage = supportsImage,
        )

    fun presetModelProfiles(providerId: String): List<ApiModelProfile> =
        presetModels.map { model -> model.toApiModelProfile(providerId) }

    companion object {
        fun defaults(): List<ProviderTemplate> =
            listOf(
                ProviderTemplate(
                    templateId = "openai-main",
                    label = "OpenAI Compatible",
                    providerKind = ProviderKind.OPENAI_COMPATIBLE,
                    baseUrl = "https://api.openai.com/v1",
                    apiStyle = ProviderTransportStyle.RESPONSES,
                    authScheme = ProviderAuthScheme.BEARER,
                    presetModels =
                        listOf(
                            ProviderTemplateModel(
                                modelId = "gpt-4.1-mini",
                                label = "GPT-4.1 mini",
                                capabilities = listOf("reader.summary", "reader.translate"),
                            ),
                            ProviderTemplateModel(
                                modelId = "gpt-4.1",
                                label = "GPT-4.1",
                                capabilities = listOf("reader.summary", "reader.translate", "reader.tts"),
                            ),
                        ),
                    supportsTts = true,
                ),
                ProviderTemplate(
                    templateId = "deepseek-main",
                    label = "DeepSeek",
                    providerKind = ProviderKind.DOMESTIC_PRESET,
                    baseUrl = "https://api.deepseek.com",
                    apiStyle = ProviderTransportStyle.CHAT_COMPLETIONS,
                    authScheme = ProviderAuthScheme.BEARER,
                    presetModels =
                        listOf(
                            ProviderTemplateModel(
                                modelId = "deepseek-chat",
                                label = "DeepSeek Chat",
                                capabilities = listOf("reader.summary", "reader.translate"),
                            ),
                            ProviderTemplateModel(
                                modelId = "deepseek-reasoner",
                                label = "DeepSeek Reasoner",
                                capabilities = listOf("reader.summary"),
                            ),
                        ),
                ),
                ProviderTemplate(
                    templateId = "openrouter-main",
                    label = "OpenRouter",
                    providerKind = ProviderKind.OPENAI_COMPATIBLE,
                    baseUrl = "https://openrouter.ai/api/v1",
                    apiStyle = ProviderTransportStyle.CHAT_COMPLETIONS,
                    authScheme = ProviderAuthScheme.BEARER,
                ),
                ProviderTemplate(
                    templateId = "moonshot-main",
                    label = "Moonshot",
                    providerKind = ProviderKind.DOMESTIC_PRESET,
                    baseUrl = "https://api.moonshot.cn/v1",
                    apiStyle = ProviderTransportStyle.CHAT_COMPLETIONS,
                    authScheme = ProviderAuthScheme.BEARER,
                ),
            )
    }
}

data class ProviderTemplateModel(
    val modelId: String,
    val label: String,
    val capabilities: List<String> = emptyList(),
) {
    fun toApiModelProfile(providerId: String): ApiModelProfile =
        ApiModelProfile(
            providerId = providerId,
            modelId = modelId,
            label = label,
            capabilities = capabilities,
            source = ProviderModelSource.PRESET_CATALOG,
            validationState = ApiModelValidationState.VALID,
        )
}
