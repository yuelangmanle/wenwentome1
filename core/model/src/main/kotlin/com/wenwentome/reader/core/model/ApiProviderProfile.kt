package com.wenwentome.reader.core.model

enum class ProviderKind { OPENAI_COMPATIBLE, DOMESTIC_PRESET, CUSTOM }

enum class ProviderTransportStyle { CHAT_COMPLETIONS, RESPONSES, DOMESTIC_CUSTOM }

enum class ProviderAuthScheme { BEARER, API_KEY_HEADER, ACCESS_KEY_SECRET, CUSTOM_HEADERS }

enum class ProviderSecretSyncMode { LOCAL_ONLY, SYNC_ENCRYPTED }

enum class ProviderModelSource { REMOTE_DISCOVERY, PRESET_CATALOG, MANUAL }

data class ApiProviderProfile(
    val providerId: String,
    val displayName: String,
    val providerKind: ProviderKind = ProviderKind.CUSTOM,
    val baseUrl: String? = null,
    val apiStyle: ProviderTransportStyle = ProviderTransportStyle.RESPONSES,
    val authScheme: ProviderAuthScheme = ProviderAuthScheme.BEARER,
    val enabled: Boolean = true,
    val region: String? = null,
    val organizationId: String? = null,
    val defaultHeadersTemplate: String? = null,
    val secretSyncMode: ProviderSecretSyncMode = ProviderSecretSyncMode.LOCAL_ONLY,
    val modelSource: ProviderModelSource = ProviderModelSource.MANUAL,
    val supportsChat: Boolean = true,
    val supportsTts: Boolean = false,
    val supportsEmbeddings: Boolean = false,
    val supportsImage: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
)
