package io.legado.app.ui.wenwen

import io.legado.app.constant.BookType
import io.legado.app.data.entities.Book
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WenwenReaderActionAvailabilityTest {

    @Test
    fun fromBook_hidesOnlineActionsForLocalBook() {
        val localBook =
            Book(
                bookUrl = "file:///books/demo.txt",
                origin = BookType.localTag,
                originName = "demo.txt",
                type = BookType.text or BookType.local,
            )

        val availability = WenwenReaderActionAvailability.fromBook(localBook)

        assertFalse(availability.showDownloadShortcut)
        assertFalse(availability.showDownloadEntry)
        assertFalse(availability.showChangeSourceEntry)
    }

    @Test
    fun fromBook_enablesOnlineActionsForWebBook() {
        val onlineBook =
            Book(
                bookUrl = "https://example.com/book/1",
                origin = "https://source.example",
                originName = "示例源",
                type = BookType.text,
            )

        val availability = WenwenReaderActionAvailability.fromBook(onlineBook)

        assertTrue(availability.showDownloadShortcut)
        assertTrue(availability.showDownloadEntry)
        assertTrue(availability.showChangeSourceEntry)
    }

    @Test
    fun fromBook_enablesDownloadButHidesChangeSourceForBrowserCapturedBook() {
        val browserBook =
            Book(
                bookUrl = "wenwentome-browser://novel.example.org/demo",
                origin = WENWEN_BROWSER_BOOK_ORIGIN,
                originName = "novel.example.org",
                type = BookType.text,
            )

        val availability = WenwenReaderActionAvailability.fromBook(browserBook)

        assertTrue(availability.showDownloadShortcut)
        assertTrue(availability.showDownloadEntry)
        assertFalse(availability.showChangeSourceEntry)
    }
}
