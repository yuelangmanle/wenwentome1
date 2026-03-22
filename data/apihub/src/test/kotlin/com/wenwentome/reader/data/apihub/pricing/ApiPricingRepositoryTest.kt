package com.wenwentome.reader.data.apihub.pricing

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.wenwentome.reader.core.database.ReaderDatabase
import com.wenwentome.reader.core.model.ApiModelProfile
import com.wenwentome.reader.core.model.ApiPriceOverride
import com.wenwentome.reader.core.model.ApiProviderProfile
import com.wenwentome.reader.core.model.ProviderKind
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
class ApiPricingRepositoryTest {

    @Test
    fun priceRepository_prefersManualOverrideOverCatalogPrice() = runTest {
        val repository = createRepository(testDatabase())
        repository.upsertProvider(
            ApiProviderProfile(
                providerId = "openai-main",
                displayName = "OpenAI 主线",
                providerKind = ProviderKind.OPENAI_COMPATIBLE,
            ),
        )
        repository.upsertModel(
            ApiModelProfile(
                providerId = "openai-main",
                modelId = "gpt-5-mini",
                label = "GPT-5 mini",
                inputPricePer1kMicros = 1_000L,
                outputPricePer1kMicros = 2_000L,
            ),
        )
        repository.upsertPriceOverride(
            ApiPriceOverride(
                providerId = "openai-main",
                modelId = "gpt-5-mini",
                inputPricePer1kMicros = 1_230L,
                outputPricePer1kMicros = 4_560L,
            ),
        )
        val pricingRepository = ApiPricingRepository(repository)

        val price = pricingRepository.resolve(providerId = "openai-main", modelId = "gpt-5-mini")

        assertEquals(1_230L, price?.inputPricePer1kMicros)
        assertEquals(4_560L, price?.outputPricePer1kMicros)
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
