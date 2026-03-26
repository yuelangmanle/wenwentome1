package io.legado.app.ui.wenwen

import io.legado.app.constant.BookType
import io.legado.app.data.entities.Book
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WenwenBookDetailFetchPlanTest {

    @Test
    fun resolve_refreshesCurrentSourceForOnlineBook() {
        val onlineBook =
            Book(
                bookUrl = "https://example.com/book/1",
                origin = "https://source.example",
                originName = "示例源",
                name = "雪中悍刀行",
                author = "烽火戏诸侯",
                type = BookType.text,
            )

        val plan =
            WenwenBookDetailFetchPlan.resolve(
                existingBook = onlineBook,
                searchCandidate = WenwenBookSearchCandidate("雪中悍刀行", "烽火戏诸侯"),
            )

        assertTrue(plan.refreshCurrentSource)
        assertFalse(plan.tryPreciseSearch)
    }

    @Test
    fun resolve_usesPreciseSearchForLocalBookWhenNameAuthorAvailable() {
        val localBook =
            Book(
                bookUrl = "file:///books/demo.txt",
                origin = BookType.localTag,
                originName = "demo.txt",
                name = "雪中悍刀行",
                author = "烽火戏诸侯",
                type = BookType.text or BookType.local,
            )

        val plan =
            WenwenBookDetailFetchPlan.resolve(
                existingBook = localBook,
                searchCandidate = WenwenBookSearchCandidate("雪中悍刀行", "烽火戏诸侯"),
            )

        assertFalse(plan.refreshCurrentSource)
        assertTrue(plan.tryPreciseSearch)
    }

    @Test
    fun resolve_usesPreciseSearchWhenBookMissingButNameAuthorAvailable() {
        val plan =
            WenwenBookDetailFetchPlan.resolve(
                existingBook = null,
                searchCandidate = WenwenBookSearchCandidate("雪中悍刀行", "烽火戏诸侯"),
            )

        assertFalse(plan.refreshCurrentSource)
        assertTrue(plan.tryPreciseSearch)
    }

    @Test
    fun resolve_skipsNetworkWhenSearchCandidateMissing() {
        val plan =
            WenwenBookDetailFetchPlan.resolve(
                existingBook = null,
                searchCandidate = null,
            )

        assertFalse(plan.refreshCurrentSource)
        assertFalse(plan.tryPreciseSearch)
    }
}
