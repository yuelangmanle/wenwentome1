package com.wenwentome.reader.feature.reader

import com.wenwentome.reader.core.model.BookFormat
import com.wenwentome.reader.core.model.BookRecord
import com.wenwentome.reader.core.model.OriginType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReaderUiStateLocatorTest {
    @Test
    fun locatorForSave_prefersExistingLocator() {
        val state = ReaderUiState(
            book = book(primaryFormat = BookFormat.EPUB),
            locator = "chapter:OEBPS/chapter2.xhtml#paragraph:8",
            chapterRef = "OEBPS/chapter1.xhtml",
        )

        assertEquals("chapter:OEBPS/chapter2.xhtml#paragraph:8", state.locatorForSave())
    }

    @Test
    fun locatorForSave_buildsStructuredLocatorForEpub() {
        val state = ReaderUiState(
            book = book(primaryFormat = BookFormat.EPUB),
            chapterRef = "OEBPS/chapter1.xhtml",
        )

        assertEquals("chapter:OEBPS/chapter1.xhtml#paragraph:0", state.locatorForSave())
    }

    @Test
    fun locatorForSave_usesChapterRefForWeb() {
        val state = ReaderUiState(
            book = book(primaryFormat = BookFormat.WEB),
            chapterRef = "https://example.com/chapter-9",
        )

        assertEquals("https://example.com/chapter-9", state.locatorForSave())
    }

    @Test
    fun locatorForSave_returnsNullWhenNoLogicalAnchorExists() {
        val state = ReaderUiState(
            book = book(primaryFormat = BookFormat.WEB),
        )

        assertNull(state.locatorForSave())
    }

    private fun book(primaryFormat: BookFormat) = BookRecord(
        id = "book-1",
        title = "测试书",
        originType = OriginType.LOCAL,
        primaryFormat = primaryFormat,
    )
}
