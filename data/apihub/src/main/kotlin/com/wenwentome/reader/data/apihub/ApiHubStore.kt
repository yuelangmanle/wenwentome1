package com.wenwentome.reader.data.apihub

import com.wenwentome.reader.core.model.ApiCapabilityBinding
import com.wenwentome.reader.core.model.ApiModelProfile
import com.wenwentome.reader.core.model.ApiProviderProfile
import com.wenwentome.reader.core.model.ApiUsageLog

interface ApiHubStore {
    suspend fun getProviders(): List<ApiProviderProfile>

    suspend fun getProvider(providerId: String): ApiProviderProfile?

    suspend fun upsertProvider(profile: ApiProviderProfile)

    suspend fun getModels(providerId: String): List<ApiModelProfile>

    suspend fun getCapabilityBindings(): List<ApiCapabilityBinding>

    suspend fun getCapabilityBinding(capabilityId: String): ApiCapabilityBinding?

    suspend fun upsertCapabilityBinding(binding: ApiCapabilityBinding)

    suspend fun getUsageLogs(): List<ApiUsageLog>
}
