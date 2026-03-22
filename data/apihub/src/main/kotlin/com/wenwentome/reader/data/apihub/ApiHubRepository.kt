package com.wenwentome.reader.data.apihub

import com.wenwentome.reader.core.database.dao.ApiAbilityCacheDao
import com.wenwentome.reader.core.database.dao.ApiBudgetPolicyDao
import com.wenwentome.reader.core.database.dao.ApiCapabilityBindingDao
import com.wenwentome.reader.core.database.dao.ApiModelDao
import com.wenwentome.reader.core.database.dao.ApiPriceOverrideDao
import com.wenwentome.reader.core.database.dao.ApiProviderDao
import com.wenwentome.reader.core.database.dao.ApiUsageLogDao
import com.wenwentome.reader.core.database.toEntity
import com.wenwentome.reader.core.database.toModel
import com.wenwentome.reader.core.model.ApiAbilityCacheEntry
import com.wenwentome.reader.core.model.ApiBudgetPolicy
import com.wenwentome.reader.core.model.ApiCapabilityBinding
import com.wenwentome.reader.core.model.ApiModelProfile
import com.wenwentome.reader.core.model.ApiPriceOverride
import com.wenwentome.reader.core.model.ApiProviderProfile
import com.wenwentome.reader.core.model.ApiUsageLog
import com.wenwentome.reader.core.model.DEFAULT_API_BUDGET_POLICY_ID
import com.wenwentome.reader.data.apihub.local.ApiSecretLocalStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ApiHubRepository(
    private val providerDao: ApiProviderDao,
    private val modelDao: ApiModelDao,
    private val capabilityBindingDao: ApiCapabilityBindingDao,
    private val budgetPolicyDao: ApiBudgetPolicyDao,
    private val usageLogDao: ApiUsageLogDao,
    private val priceOverrideDao: ApiPriceOverrideDao,
    private val abilityCacheDao: ApiAbilityCacheDao,
    private val secretLocalStore: ApiSecretLocalStore,
) : ApiHubStore {
    // Task 2 约定：provider 自身的本地 secret 以 providerId 作为 secret key 存取。
    private fun providerSecretId(providerId: String): String = providerId

    fun observeProviders(): Flow<List<ApiProviderProfile>> =
        providerDao.observeAll().map { items -> items.map { it.toModel() } }

    override suspend fun getProviders(): List<ApiProviderProfile> = providerDao.getAll().map { item -> item.toModel() }

    override suspend fun getProvider(providerId: String): ApiProviderProfile? =
        getProviders().firstOrNull { it.providerId == providerId }

    override suspend fun upsertProvider(profile: ApiProviderProfile) {
        providerDao.upsert(profile.toEntity())
    }

    suspend fun deleteProvider(providerId: String) {
        capabilityBindingDao.getAll().forEach { binding ->
            when {
                binding.primaryProviderId == providerId -> capabilityBindingDao.deleteById(binding.capabilityId)
                binding.fallbackProviderId == providerId ->
                    capabilityBindingDao.upsert(
                        binding.copy(
                            fallbackProviderId = null,
                            fallbackModelId = null,
                            updatedAt = System.currentTimeMillis(),
                        ),
                    )
            }
        }
        modelDao.deleteByProviderId(providerId)
        priceOverrideDao.deleteByProviderId(providerId)
        secretLocalStore.delete(providerSecretId(providerId))
        providerDao.deleteById(providerId)
    }

    fun observeModels(providerId: String): Flow<List<ApiModelProfile>> =
        modelDao.observeByProviderId(providerId).map { items -> items.map { it.toModel() } }

    override suspend fun getModels(providerId: String): List<ApiModelProfile> =
        modelDao.getByProviderId(providerId).map { item -> item.toModel() }

    suspend fun upsertModel(profile: ApiModelProfile) {
        modelDao.upsert(profile.toEntity())
    }

    suspend fun replaceModels(providerId: String, profiles: List<ApiModelProfile>) {
        require(profiles.all { it.providerId == providerId }) {
            "All model profiles must belong to providerId=$providerId"
        }
        modelDao.replaceForProvider(providerId, profiles.map { it.toEntity() })
    }

    suspend fun deleteModel(providerId: String, modelId: String) {
        modelDao.deleteById(providerId, modelId)
    }

    fun observeCapabilityBindings(): Flow<List<ApiCapabilityBinding>> =
        capabilityBindingDao.observeAll().map { items -> items.map { it.toModel() } }

    override suspend fun getCapabilityBindings(): List<ApiCapabilityBinding> =
        capabilityBindingDao.getAll().map { item -> item.toModel() }

    override suspend fun getCapabilityBinding(capabilityId: String): ApiCapabilityBinding? =
        getCapabilityBindings().firstOrNull { it.capabilityId == capabilityId }

    override suspend fun upsertCapabilityBinding(binding: ApiCapabilityBinding) {
        capabilityBindingDao.upsert(binding.toEntity())
    }

    suspend fun deleteCapabilityBinding(capabilityId: String) {
        capabilityBindingDao.deleteById(capabilityId)
    }

    fun observeBudgetPolicy(): Flow<ApiBudgetPolicy?> =
        budgetPolicyDao.observeById(DEFAULT_API_BUDGET_POLICY_ID).map { it?.toModel() }

    suspend fun getBudgetPolicy(policyId: String = DEFAULT_API_BUDGET_POLICY_ID): ApiBudgetPolicy? =
        budgetPolicyDao.getById(policyId)?.toModel()

    suspend fun upsertBudgetPolicy(policy: ApiBudgetPolicy) {
        budgetPolicyDao.upsert(policy.toEntity())
    }

    suspend fun clearBudgetPolicy(policyId: String = DEFAULT_API_BUDGET_POLICY_ID) {
        budgetPolicyDao.deleteById(policyId)
    }

    fun observeUsageLogs(): Flow<List<ApiUsageLog>> =
        usageLogDao.observeLatest().map { items -> items.map { it.toModel() } }

    override suspend fun getUsageLogs(): List<ApiUsageLog> = observeUsageLogs().first()

    suspend fun appendUsageLog(log: ApiUsageLog) {
        usageLogDao.upsert(log.toEntity())
    }

    suspend fun deleteUsageLogsOlderThan(cutoffAt: Long) {
        usageLogDao.deleteOlderThan(cutoffAt)
    }

    fun observePriceOverrides(): Flow<List<ApiPriceOverride>> =
        priceOverrideDao.observeAll().map { items -> items.map { it.toModel() } }

    suspend fun getPriceOverrides(): List<ApiPriceOverride> = observePriceOverrides().first()

    suspend fun findPriceOverride(providerId: String, modelId: String): ApiPriceOverride? =
        priceOverrideDao.findById(providerId, modelId)?.toModel()

    suspend fun upsertPriceOverride(override: ApiPriceOverride) {
        priceOverrideDao.upsert(override.toEntity())
    }

    suspend fun deletePriceOverride(providerId: String, modelId: String) {
        priceOverrideDao.deleteById(providerId, modelId)
    }

    fun observeAbilityCache(): Flow<List<ApiAbilityCacheEntry>> =
        abilityCacheDao.observeAll().map { items -> items.map { it.toModel() } }

    suspend fun findAbilityCache(cacheKey: String): ApiAbilityCacheEntry? =
        abilityCacheDao.findByKey(cacheKey)?.toModel()

    suspend fun upsertAbilityCache(entry: ApiAbilityCacheEntry) {
        abilityCacheDao.upsert(entry.toEntity())
    }

    suspend fun deleteAbilityCache(cacheKey: String) {
        abilityCacheDao.deleteByKey(cacheKey)
    }

    suspend fun deleteExpiredAbilityCache(expiredAt: Long) {
        abilityCacheDao.deleteExpired(expiredAt)
    }

    suspend fun saveSecret(secretId: String, plainText: String) {
        secretLocalStore.save(secretId, plainText)
    }

    suspend fun readSecret(secretId: String): String? = secretLocalStore.read(secretId)

    suspend fun deleteSecret(secretId: String) {
        secretLocalStore.delete(secretId)
    }
}
