package com.wenwentome.reader.data.apihub.cache

import com.wenwentome.reader.core.model.ApiAbilityCacheEntry
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AbilityResultCacheRepositoryTest {

    @Test
    fun getOrPut_returnsCachedValueOnSecondCall() = runTest {
        var producerCalls = 0
        val repository = AbilityResultCacheRepository(
            store = InMemoryAbilityCacheStore(),
            nowProvider = { 10_000L },
        )

        val first =
            repository.getOrPut(
                cacheKey = "reader.summary:book-1:chapter-3",
                serializer = SamplePayload.serializer(),
                ttlMs = 60_000L,
            ) {
                producerCalls += 1
                SamplePayload(text = "第三章总结")
            }
        val second =
            repository.getOrPut(
                cacheKey = "reader.summary:book-1:chapter-3",
                serializer = SamplePayload.serializer(),
                ttlMs = 60_000L,
            ) {
                producerCalls += 1
                SamplePayload(text = "不会被调用")
            }

        assertEquals("第三章总结", first.value.text)
        assertEquals("第三章总结", second.value.text)
        assertTrue(second.hit)
        assertEquals(1, producerCalls)
    }

    @Serializable
    private data class SamplePayload(
        val text: String,
    )

    private class InMemoryAbilityCacheStore : AbilityCacheStore {
        private val entries = mutableMapOf<String, ApiAbilityCacheEntry>()

        override suspend fun find(cacheKey: String): ApiAbilityCacheEntry? = entries[cacheKey]

        override suspend fun upsert(entry: ApiAbilityCacheEntry) {
            entries[entry.cacheKey] = entry
        }

        override suspend fun deleteExpired(expiredAt: Long) {
            entries.entries.removeAll { (_, value) -> value.expiresAt <= expiredAt }
        }
    }
}
