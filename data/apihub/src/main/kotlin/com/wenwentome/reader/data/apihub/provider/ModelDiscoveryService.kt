package com.wenwentome.reader.data.apihub.provider

import com.wenwentome.reader.core.model.ApiModelProfile
import com.wenwentome.reader.core.model.ApiProviderProfile

fun interface ModelDiscoveryService {
    suspend fun discover(provider: ApiProviderProfile): List<ApiModelProfile>
}

object NoOpModelDiscoveryService : ModelDiscoveryService {
    override suspend fun discover(provider: ApiProviderProfile): List<ApiModelProfile> = emptyList()
}
