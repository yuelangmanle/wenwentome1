package com.wenwentome.reader.data.apihub

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.wenwentome.reader.core.database.ReaderDatabase
import com.wenwentome.reader.core.database.entity.ApiCapabilityBindingEntity
import com.wenwentome.reader.core.model.ApiModelCostLevel
import com.wenwentome.reader.core.model.ApiModelProfile
import com.wenwentome.reader.core.model.ApiPriceOverride
import com.wenwentome.reader.core.model.ApiProviderProfile
import com.wenwentome.reader.core.model.ProviderKind
import com.wenwentome.reader.core.model.ProviderModelSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ApiHubRepositoryTest {
    @Test
    fun deleteProvider_cascadesLocalData_andDowngradesFallbackBindings() = runTest {
        val database = testDatabase()
        val secretStore = FakeApiSecretLocalStore()
        val repository = createRepository(database, secretStore)
        repository.upsertProvider(
            ApiProviderProfile(
                providerId = "provider-a",
                displayName = "Provider A",
                providerKind = ProviderKind.OPENAI_COMPATIBLE,
            ),
        )
        repository.upsertProvider(
            ApiProviderProfile(
                providerId = "provider-b",
                displayName = "Provider B",
                providerKind = ProviderKind.OPENAI_COMPATIBLE,
            ),
        )
        repository.upsertModel(
            ApiModelProfile(
                providerId = "provider-a",
                modelId = "model-a",
                label = "Model A",
                capabilities = listOf("reader.summary"),
                costLevel = ApiModelCostLevel.LOW,
                source = ProviderModelSource.MANUAL,
            ),
        )
        repository.upsertPriceOverride(
            ApiPriceOverride(
                providerId = "provider-a",
                modelId = "model-a",
                inputPricePer1kMicros = 100L,
            ),
        )
        repository.upsertCapabilityBinding(
            com.wenwentome.reader.core.model.ApiCapabilityBinding(
                capabilityId = "reader.summary",
                primaryProviderId = "provider-a",
                primaryModelId = "model-a",
                updatedAt = 1L,
            ),
        )
        repository.upsertCapabilityBinding(
            com.wenwentome.reader.core.model.ApiCapabilityBinding(
                capabilityId = "reader.translate",
                primaryProviderId = "provider-b",
                primaryModelId = "model-b",
                fallbackProviderId = "provider-a",
                fallbackModelId = "model-a",
                updatedAt = 1L,
            ),
        )
        repository.saveSecret("provider-a", "sk-provider-a")

        repository.deleteProvider("provider-a")

        assertEquals(listOf("provider-b"), repository.observeProviders().first().map { it.providerId })
        assertEquals(emptyList<ApiModelProfile>(), repository.observeModels("provider-a").first())
        assertEquals(emptyList<ApiPriceOverride>(), repository.observePriceOverrides().first())
        assertNull(repository.readSecret("provider-a"))

        val bindings = database.apiCapabilityBindingDao().getAll().associateBy(ApiCapabilityBindingEntity::capabilityId)
        assertNull(bindings["reader.summary"])
        val fallbackBinding = bindings["reader.translate"]
        assertEquals("provider-b", fallbackBinding?.primaryProviderId)
        assertNull(fallbackBinding?.fallbackProviderId)
        assertNull(fallbackBinding?.fallbackModelId)
        assertEquals(true, (fallbackBinding?.updatedAt ?: 0L) > 1L)
    }

    @Test
    fun replaceModels_rejectsProfilesForOtherProviders() = runTest {
        val repository = createRepository(testDatabase(), FakeApiSecretLocalStore())

        try {
            repository.replaceModels(
                providerId = "provider-a",
                profiles =
                    listOf(
                        ApiModelProfile(
                            providerId = "provider-b",
                            modelId = "model-a",
                            label = "Wrong Provider",
                        ),
                    ),
            )
            fail("Expected replaceModels to reject mismatched providerId")
        } catch (expected: IllegalArgumentException) {
            assertEquals(
                "All model profiles must belong to providerId=provider-a",
                expected.message,
            )
        }
    }

    private fun createRepository(
        database: ReaderDatabase,
        secretStore: FakeApiSecretLocalStore,
    ): ApiHubRepository =
        ApiHubRepository(
            providerDao = database.apiProviderDao(),
            modelDao = database.apiModelDao(),
            capabilityBindingDao = database.apiCapabilityBindingDao(),
            budgetPolicyDao = database.apiBudgetPolicyDao(),
            usageLogDao = database.apiUsageLogDao(),
            priceOverrideDao = database.apiPriceOverrideDao(),
            abilityCacheDao = database.apiAbilityCacheDao(),
            secretLocalStore = secretStore,
        )

    private fun testDatabase(): ReaderDatabase =
        Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ReaderDatabase::class.java,
        )
            .allowMainThreadQueries()
            .build()

    private class FakeApiSecretLocalStore : com.wenwentome.reader.data.apihub.local.ApiSecretLocalStore {
        private val secrets = linkedMapOf<String, String>()

        override suspend fun save(secretId: String, plainText: String) {
            secrets[secretId] = plainText
        }

        override suspend fun read(secretId: String): String? = secrets[secretId]

        override suspend fun delete(secretId: String) {
            secrets.remove(secretId)
        }
    }
}
