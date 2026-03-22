package com.wenwentome.reader.data.apihub.ability

import com.wenwentome.reader.core.model.ApiAbilityCacheEntry
import com.wenwentome.reader.data.apihub.cache.AbilityCacheStore
import com.wenwentome.reader.data.apihub.cache.AbilityResultCacheRepository
import com.wenwentome.reader.data.apihub.runtime.ApiAbilityRequest
import com.wenwentome.reader.data.apihub.runtime.ApiDispatchResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderAbilityFacadeTest {

    @Test
    fun summarizeChapter_usesCacheAndDispatcher() = runTest {
        var remoteCalls = 0
        var lastCapabilityId: String? = null
        var lastRequest: ApiAbilityRequest? = null
        val facade: ReaderAbilityFacade =
            DefaultReaderAbilityFacade(
                invoker = { capabilityId, request ->
                    remoteCalls += 1
                    lastCapabilityId = capabilityId
                    lastRequest = request
                    ApiDispatchResult(
                        providerId = "provider-a",
                        modelId = "model-a",
                        outputText = "这是第三章总结",
                        estimatedCostMicros = 1_200L,
                        usedFallback = false,
                    )
                },
                cacheRepository = AbilityResultCacheRepository(
                    store = InMemoryAbilityCacheStore(),
                    nowProvider = { 100L },
                ),
            )

        val input = ReaderAbilityInput(
            bookId = "book-1",
            chapterRef = "chapter-3",
            chapterTitle = "第三章",
            paragraphs = listOf("第一段", "第二段"),
        )

        val first = facade.summarizeChapter(input)
        val second = facade.summarizeChapter(input)

        assertEquals("reader.summary", lastCapabilityId)
        assertTrue(lastRequest?.body.orEmpty().contains("第三章"))
        assertEquals("这是第三章总结", first.text)
        assertEquals("这是第三章总结", second.text)
        assertTrue(second.cached)
        assertEquals(1, remoteCalls)
    }

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
