package com.wenwentome.reader.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.wenwentome.reader.core.model.ProviderAuthScheme
import com.wenwentome.reader.core.model.ProviderKind
import com.wenwentome.reader.core.model.ProviderModelSource
import com.wenwentome.reader.core.model.ProviderSecretSyncMode
import com.wenwentome.reader.core.model.ProviderTransportStyle

@Entity(tableName = "api_providers")
data class ApiProviderEntity(
    @PrimaryKey val providerId: String,
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
