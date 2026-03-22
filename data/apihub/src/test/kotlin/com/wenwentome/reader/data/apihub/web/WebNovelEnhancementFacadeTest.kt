package com.wenwentome.reader.data.apihub.web

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WebNovelEnhancementFacadeTest {
    @Test
    fun enhanceRanksResultsByHealthScoreAndBoost() {
        val facade = WebNovelEnhancementFacade(nowProvider = { 1_000L })
        repeat(10) {
            facade.recordSourceResult(sourceId = "source-fast", success = true, latencyMs = 300)
        }
        repeat(2) {
            facade.recordSourceResult(sourceId = "source-slow", success = true, latencyMs = 2_500)
        }
        val results =
            listOf(
                WebNovelSearchResult(
                    id = "slow-1",
                    sourceId = "source-slow",
                    title = "雪中悍刀行",
                ),
                WebNovelSearchResult(
                    id = "fast-1",
                    sourceId = "source-fast",
                    title = "雪中悍刀行",
                ),
            )

        val enhanced = facade.enhanceSearchResults(results)

        assertEquals("source-fast", enhanced.first().sourceId)
        assertTrue(enhanced.first().healthScore > 0.8f)
    }

    @Test
    fun recordFailureDropsHealthScoreQuickly() {
        val facade = WebNovelEnhancementFacade(nowProvider = { 1_000L })
        repeat(6) {
            facade.recordSourceResult(sourceId = "source-unstable", success = true, latencyMs = 1_200)
        }
        val before = facade.healthScore("source-unstable")

        facade.recordSourceResult(sourceId = "source-unstable", success = false, latencyMs = 1_200)

        val after = facade.healthScore("source-unstable")
        assertTrue(after < before)
    }

    @Test
    fun enhanceSearchResults_prefersTitleAndAuthorMatchBeforeSameHealthTier() {
        val facade = WebNovelEnhancementFacade(nowProvider = { 1_000L })
        repeat(5) {
            facade.recordSourceResult(sourceId = "source-a", success = true, latencyMs = 900)
            facade.recordSourceResult(sourceId = "source-b", success = true, latencyMs = 900)
        }
        val results =
            listOf(
                WebNovelSearchResult(
                    id = "other-1",
                    sourceId = "source-a",
                    title = "雪中外传",
                    author = "佚名",
                ),
                WebNovelSearchResult(
                    id = "match-1",
                    sourceId = "source-b",
                    title = "雪中悍刀行",
                    author = "烽火戏诸侯",
                ),
            )

        val enhanced =
            facade.enhanceSearchResults(
                results = results,
                preferredTitle = "雪中悍刀行",
                preferredAuthor = "烽火戏诸侯",
            )

        assertEquals("match-1", enhanced.first().id)
        assertEquals("题名直达 · 作者命中", enhanced.first().boostReason)
    }
}
