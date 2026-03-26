package io.legado.app.ui.wenwen

import org.junit.Assert.assertEquals
import org.junit.Test

class WenwenUiBridgeTest {

    @Test
    fun resolveBookLookup_prefersBookUrlWhenProvided() {
        val lookup = WenwenUiBridge.resolveBookLookup(
            bookUrl = "https://example.com/book/1",
            name = "雪中悍刀行",
            author = "烽火戏诸侯",
        )

        assertEquals(
            WenwenBookLookup.ByBookUrl("https://example.com/book/1"),
            lookup,
        )
    }

    @Test
    fun resolveBookLookup_fallsBackToNameAndAuthor() {
        val lookup = WenwenUiBridge.resolveBookLookup(
            bookUrl = null,
            name = "雪中悍刀行",
            author = "烽火戏诸侯",
        )

        assertEquals(
            WenwenBookLookup.ByNameAuthor(
                name = "雪中悍刀行",
                author = "烽火戏诸侯",
            ),
            lookup,
        )
    }

    @Test
    fun resolveBookLookup_returnsMissingWhenInsufficientArguments() {
        val lookup = WenwenUiBridge.resolveBookLookup(
            bookUrl = null,
            name = "雪中悍刀行",
            author = "",
        )

        assertEquals(WenwenBookLookup.Missing, lookup)
    }
}
