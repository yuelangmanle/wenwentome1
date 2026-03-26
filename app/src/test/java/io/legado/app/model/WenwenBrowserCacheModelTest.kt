package io.legado.app.model

import io.legado.app.data.entities.Book
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WenwenBrowserCacheModelTest {

    @Test
    fun addDownload_keepsDownloadToEndStateWhenMergingRanges() {
        val model = WenwenBrowserCache.BrowserCacheModel(
            Book(bookUrl = "wenwentome-browser://novel.example.org/demo")
        )

        model.addDownload(start = 5, end = -1)
        model.addDownload(start = 2, end = 4)

        assertEquals(2, model.startIndex)
        assertTrue(model.containsIndex(2))
        assertTrue(model.containsIndex(999))
        assertFalse(model.isStop())
    }

    @Test
    fun waitCount_excludesCurrentDownloadingChapterForFiniteRange() {
        val model = WenwenBrowserCache.BrowserCacheModel(
            Book(bookUrl = "wenwentome-browser://novel.example.org/demo")
        )

        model.addDownload(start = 2, end = 4)
        assertEquals(3, model.waitCount)

        model.begin(2)
        assertEquals(2, model.waitCount)

        model.completeError()
        assertEquals(3, model.waitCount)
    }
}
