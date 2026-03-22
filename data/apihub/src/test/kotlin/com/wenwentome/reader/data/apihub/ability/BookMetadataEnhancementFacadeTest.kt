package com.wenwentome.reader.data.apihub.ability

import com.wenwentome.reader.core.model.ApiAbilityCacheEntry
import com.wenwentome.reader.core.model.BookFormat
import com.wenwentome.reader.core.model.BookRecord
import com.wenwentome.reader.core.model.OriginType
import com.wenwentome.reader.data.apihub.cache.AbilityCacheStore
import com.wenwentome.reader.data.apihub.cache.AbilityResultCacheRepository
import com.wenwentome.reader.data.apihub.runtime.ApiDispatchResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BookMetadataEnhancementFacadeTest {

    @Test
    fun enhance_reusesCacheByBookSnapshot() = runTest {
        var remoteCalls = 0
        val facade: BookMetadataEnhancementFacade =
            DefaultBookMetadataEnhancementFacade(
                invoker = { capabilityId, request ->
                    remoteCalls += 1
                    assertEquals("book.metadata.enhance", capabilityId)
                    assertTrue(request.body.contains("悉达多"))
                    ApiDispatchResult(
                        providerId = "provider-a",
                        modelId = "model-a",
                        outputText =
                            """
                            {
                              "improvedSummary":"AI 优化后的简介",
                              "suggestedCoverUri":"https://example.com/ai-cover.jpg",
                              "authorIntroduction":"黑塞，德国作家。",
                              "tags":["成长","哲思"]
                            }
                            """.trimIndent(),
                        estimatedCostMicros = 2_400L,
                        usedFallback = false,
                    )
                },
                cacheRepository = AbilityResultCacheRepository(
                    store = InMemoryAbilityCacheStore(),
                    nowProvider = { 100L },
                ),
            )

        val book =
            BookRecord(
                id = "book-1",
                title = "悉达多",
                author = "黑塞",
                originType = OriginType.LOCAL,
                primaryFormat = BookFormat.EPUB,
                summary = "原始简介",
            )

        val first = facade.enhance(book)
        val second = facade.enhance(book)

        assertEquals("AI 优化后的简介", first.improvedSummary)
        assertEquals("https://example.com/ai-cover.jpg", first.suggestedCoverUri)
        assertEquals(listOf("成长", "哲思"), second.tags)
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
