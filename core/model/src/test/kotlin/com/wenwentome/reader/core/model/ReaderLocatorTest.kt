package com.wenwentome.reader.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderLocatorTest {
    @Test
    fun buildReaderChapterLocator_webEncodesChapterRefAndStartsAtParagraphZero() {
        val chapterRef = "https://example.com/books/1/chapter-9?from=shelf"

        val locator = buildReaderChapterLocator(BookFormat.WEB, chapterRef)

        assertTrue(locator?.startsWith("web:chapter=") == true)
        assertTrue(locator?.endsWith("#paragraph:0") == true)
        assertEquals(chapterRef, resolveReaderChapterRef(BookFormat.WEB, locator))
        assertEquals(0, resolveReaderParagraphIndex(BookFormat.WEB, locator))
    }

    @Test
    fun resolveReaderChapterRef_webSupportsLegacyPlainLocator() {
        val legacyLocator = "https://example.com/books/1/chapter-2"

        assertEquals(legacyLocator, resolveReaderChapterRef(BookFormat.WEB, legacyLocator))
        assertEquals(0, resolveReaderParagraphIndex(BookFormat.WEB, legacyLocator))
    }

    @Test
    fun buildReaderParagraphLocator_epubRetainsStructuredParagraphAnchor() {
        val locator = buildReaderParagraphLocator(
            format = BookFormat.EPUB,
            chapterRef = "OEBPS/chapter1.xhtml",
            paragraphIndex = 8,
        )

        assertEquals("chapter:OEBPS/chapter1.xhtml#paragraph:8", locator)
        assertEquals(8, resolveReaderParagraphIndex(BookFormat.EPUB, locator))
        assertEquals("OEBPS/chapter1.xhtml", resolveReaderChapterRef(BookFormat.EPUB, locator))
    }
}
