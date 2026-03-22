package com.wenwentome.reader.data.apihub.provider

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.wenwentome.reader.core.database.ReaderDatabase
import com.wenwentome.reader.core.model.ApiModelProfile
import com.wenwentome.reader.core.model.ApiProviderProfile
import com.wenwentome.reader.core.model.ProviderModelSource
import com.wenwentome.reader.data.apihub.ApiHubRepository
import com.wenwentome.reader.data.apihub.local.ApiSecretLocalStore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ProviderValidationServiceTest {

    @Test
    fun bindCapability_requiresSuccessfulProbeOrPresetMatch() = runTest {
        val database = testDatabase()
        val apiHubRepository = createRepository(database)
        apiHubRepository.upsertProvider(
            ApiProviderProfile(
                providerId = "custom",
                displayName = "自定义 Provider",
                modelSource = ProviderModelSource.MANUAL,
            ),
        )
        apiHubRepository.upsertModel(
            ApiModelProfile(
                providerId = "custom",
                modelId = "bad-model",
                label = "Bad Model",
                source = ProviderModelSource.MANUAL,
            ),
        )
        val catalogRepository = ProviderCatalogRepository(
            apiHubRepository = apiHubRepository,
            modelDiscoveryService = object : ModelDiscoveryService {
                override suspend fun discover(provider: ApiProviderProfile): List<ApiModelProfile> = emptyList()
            },
        )
        val validator = ProviderValidationService(
            providerCatalogRepository = catalogRepository,
            bindingProbe = { _, _ ->
                ProviderBindingProbeResult(
                    success = false,
                    message = "probe failed for bad-model",
                )
            },
        )

        val result = validator.validateBinding(providerId = "custom", modelId = "bad-model")

        assertFalse(result.allowed)
        assertTrue(result.reason.contains("probe"))
    }

    private fun createRepository(database: ReaderDatabase): ApiHubRepository =
        ApiHubRepository(
            providerDao = database.apiProviderDao(),
            modelDao = database.apiModelDao(),
            capabilityBindingDao = database.apiCapabilityBindingDao(),
            budgetPolicyDao = database.apiBudgetPolicyDao(),
            usageLogDao = database.apiUsageLogDao(),
            priceOverrideDao = database.apiPriceOverrideDao(),
            abilityCacheDao = database.apiAbilityCacheDao(),
            secretLocalStore = InMemorySecretStore(),
        )

    private fun testDatabase(): ReaderDatabase =
        Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ReaderDatabase::class.java,
        )
            .allowMainThreadQueries()
            .build()

    private class InMemorySecretStore : ApiSecretLocalStore {
        override suspend fun save(secretId: String, plainText: String) = Unit

        override suspend fun read(secretId: String): String? = null

        override suspend fun delete(secretId: String) = Unit
    }
}
