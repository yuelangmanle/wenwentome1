package com.wenwentome.reader.data.apihub.runtime

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.wenwentome.reader.core.database.ReaderDatabase
import com.wenwentome.reader.core.model.ApiBudgetPolicy
import com.wenwentome.reader.core.model.ApiCapabilityBinding
import com.wenwentome.reader.core.model.ApiModelProfile
import com.wenwentome.reader.core.model.ApiProviderProfile
import com.wenwentome.reader.core.model.ProviderKind
import com.wenwentome.reader.data.apihub.ApiHubRepository
import com.wenwentome.reader.data.apihub.local.ApiSecretLocalStore
import com.wenwentome.reader.data.apihub.pricing.ApiPricingRepository
import com.wenwentome.reader.data.apihub.pricing.PriceEstimator
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ApiAbilityDispatcherTest {

    @Test
    fun dispatch_fallsBackToSecondaryModelOn429_andWritesUsageLog() = runTest {
        val now = 1_710_000_000_000L
        val repository = createRepository(testDatabase())
        repository.upsertProvider(
            ApiProviderProfile(
                providerId = "primary-provider",
                displayName = "Primary",
                providerKind = ProviderKind.OPENAI_COMPATIBLE,
            ),
        )
        repository.upsertProvider(
            ApiProviderProfile(
                providerId = "fallback-provider",
                displayName = "Fallback",
                providerKind = ProviderKind.OPENAI_COMPATIBLE,
            ),
        )
        repository.upsertModel(
            ApiModelProfile(
                providerId = "primary-provider",
                modelId = "primary-model",
                label = "Primary Model",
                inputPricePer1kMicros = 1_000L,
                outputPricePer1kMicros = 2_000L,
            ),
        )
        repository.upsertModel(
            ApiModelProfile(
                providerId = "fallback-provider",
                modelId = "fallback-model",
                label = "Fallback Model",
                inputPricePer1kMicros = 900L,
                outputPricePer1kMicros = 1_500L,
            ),
        )
        repository.upsertCapabilityBinding(
            ApiCapabilityBinding(
                capabilityId = "reader.summary",
                primaryProviderId = "primary-provider",
                primaryModelId = "primary-model",
                fallbackProviderId = "fallback-provider",
                fallbackModelId = "fallback-model",
                updatedAt = now,
            ),
        )
        repository.upsertBudgetPolicy(
            ApiBudgetPolicy(
                dailyLimitMicros = 10_000_000L,
                monthlyLimitMicros = 200_000_000L,
                updatedAt = now,
            ),
        )
        val dispatcher = ApiAbilityDispatcher(
            repository = repository,
            priceEstimator = PriceEstimator(ApiPricingRepository(repository)),
            budgetGuard = ApiBudgetGuard(repository, nowProvider = { now }),
            fallbackExecutor =
                ApiFallbackExecutor(
                    abilityExecutor =
                        FakeApiAbilityExecutor(
                            outcomes =
                                mapOf(
                                    "primary-provider/primary-model" to ApiAbilityExecutionResult.Failure(429, "rate limited"),
                                    "fallback-provider/fallback-model" to ApiAbilityExecutionResult.Success("fallback answer"),
                                ),
                        ),
                ),
            callLogger = ApiCallLogger(repository, nowProvider = { now }),
        )

        val result =
            dispatcher.invoke(
                capabilityId = "reader.summary",
                request =
                    ApiAbilityRequest(
                        body = "第三章正文",
                        bookId = "book-1",
                        chapterRef = "chapter-3",
                        estimatedInputTokens = 120,
                        estimatedOutputTokens = 48,
                    ),
            )

        assertEquals("fallback-provider", result.providerId)
        assertTrue(result.usedFallback)

        val latestLog = repository.getUsageLogs().first()
        assertEquals("fallback-provider", latestLog.providerId)
        assertTrue(latestLog.usedFallback)
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

    private class FakeApiAbilityExecutor(
        private val outcomes: Map<String, ApiAbilityExecutionResult>,
    ) : ApiAbilityExecutor {
        override suspend fun execute(
            providerId: String,
            modelId: String,
            request: ApiAbilityRequest,
        ): ApiAbilityExecutionResult = outcomes.getValue("$providerId/$modelId")
    }
}
