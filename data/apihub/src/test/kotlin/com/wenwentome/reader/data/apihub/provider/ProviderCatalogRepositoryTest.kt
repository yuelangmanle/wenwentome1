package com.wenwentome.reader.data.apihub.provider

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.wenwentome.reader.core.database.ReaderDatabase
import com.wenwentome.reader.core.model.ApiModelProfile
import com.wenwentome.reader.core.model.ApiProviderProfile
import com.wenwentome.reader.core.model.ProviderKind
import com.wenwentome.reader.core.model.ProviderModelSource
import com.wenwentome.reader.data.apihub.ApiHubRepository
import com.wenwentome.reader.data.apihub.local.ApiSecretLocalStore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ProviderCatalogRepositoryTest {

    @Test
    fun discoverModels_mergesRemotePresetAndManualEntries() = runTest {
        val database = testDatabase()
        val apiHubRepository = createRepository(database)
        apiHubRepository.upsertProvider(
            ApiProviderProfile(
                providerId = "deepseek-main",
                displayName = "DeepSeek 主线",
                providerKind = ProviderKind.DOMESTIC_PRESET,
                baseUrl = "https://api.deepseek.com",
                modelSource = ProviderModelSource.REMOTE_DISCOVERY,
            ),
        )
        apiHubRepository.upsertModel(
            ApiModelProfile(
                providerId = "deepseek-main",
                modelId = "custom-manual-model",
                label = "自定义模型",
                source = ProviderModelSource.MANUAL,
            ),
        )
        val repository = ProviderCatalogRepository(
            apiHubRepository = apiHubRepository,
            modelDiscoveryService =
                FakeModelDiscoveryService(
                    discovered =
                        mapOf(
                            "deepseek-main" to
                                listOf(
                                    ApiModelProfile(
                                        providerId = "deepseek-main",
                                        modelId = "deepseek-chat",
                                        label = "DeepSeek Chat",
                                        source = ProviderModelSource.REMOTE_DISCOVERY,
                                    ),
                                ),
                        ),
                ),
        )

        val models = repository.modelsFor("deepseek-main")

        assertEquals(
            listOf("deepseek-chat", "deepseek-reasoner", "custom-manual-model"),
            models.map { it.modelId },
        )
        assertEquals(ProviderModelSource.REMOTE_DISCOVERY, models[0].source)
        assertEquals(ProviderModelSource.PRESET_CATALOG, models[1].source)
        assertEquals(ProviderModelSource.MANUAL, models[2].source)
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
        private val values = linkedMapOf<String, String>()

        override suspend fun save(secretId: String, plainText: String) {
            values[secretId] = plainText
        }

        override suspend fun read(secretId: String): String? = values[secretId]

        override suspend fun delete(secretId: String) {
            values.remove(secretId)
        }
    }

    private class FakeModelDiscoveryService(
        private val discovered: Map<String, List<ApiModelProfile>>,
    ) : ModelDiscoveryService {
        override suspend fun discover(provider: ApiProviderProfile): List<ApiModelProfile> =
            discovered[provider.providerId].orEmpty()
    }
}
