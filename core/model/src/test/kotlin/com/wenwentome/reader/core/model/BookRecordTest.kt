package com.wenwentome.reader.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class BookRecordTest {
    @Test
    fun newLocalBook_defaultsToInShelfAndLocalOrigin() {
        val book = BookRecord.newLocal(
            title = "三体",
            author = "刘慈欣",
            format = BookFormat.EPUB,
        )

        assertEquals(OriginType.LOCAL, book.originType)
        assertEquals(BookshelfState.IN_SHELF, book.bookshelfState)
        assertEquals(BookFormat.EPUB, book.primaryFormat)
    }
}

