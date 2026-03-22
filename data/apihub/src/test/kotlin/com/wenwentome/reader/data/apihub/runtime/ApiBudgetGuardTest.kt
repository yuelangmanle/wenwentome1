package com.wenwentome.reader.data.apihub.runtime

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.wenwentome.reader.core.database.ReaderDatabase
import com.wenwentome.reader.core.model.ApiBudgetPolicy
import com.wenwentome.reader.core.model.ApiUsageLog
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
class ApiBudgetGuardTest {

    @Test
    fun budgetGuard_blocksHighCostRequestWhenDailyBudgetExceeded() = runTest {
        val now = 1_710_000_000_000L
        val repository = createRepository(testDatabase())
        repository.upsertBudgetPolicy(
            ApiBudgetPolicy(
                dailyLimitMicros = 1_500_000L,
                monthlyLimitMicros = 50_000_000L,
                updatedAt = now,
            ),
        )
        repository.appendUsageLog(
            ApiUsageLog(
                callId = "call-1",
                capabilityId = "reader.summary",
                providerId = "provider-a",
                modelId = "model-a",
                success = true,
                estimatedCostMicros = 900_000L,
                createdAt = now,
            ),
        )
        val guard = ApiBudgetGuard(
            repository = repository,
            nowProvider = { now },
        )

        val decision = guard.evaluate(estimatedCostMicros = 800_000L)

        assertEquals(BudgetDecision.Blocked("daily_limit"), decision)
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
