package io.legado.app.ui.wenwen

import io.legado.app.constant.BookType
import io.legado.app.data.entities.Book
import org.junit.Assert.assertEquals
import org.junit.Test

class WenwenReaderRoutingTest {

    @Test
    fun resolveDestination_usesNewReaderForLocalTxt() {
        val book = Book(
            bookUrl = "file:///books/demo.txt",
            origin = BookType.localTag,
            originName = "demo.txt",
            type = BookType.text,
        )

        assertEquals(
            WenwenReaderDestination.WenwenReader,
            WenwenReaderRouting.resolveDestination(book, showMangaUi = true),
        )
    }

    @Test
    fun resolveDestination_usesNewReaderForLocalEpub() {
        val book = Book(
            bookUrl = "file:///books/demo.epub",
            origin = BookType.localTag,
            originName = "demo.epub",
            type = BookType.text,
        )

        assertEquals(
            WenwenReaderDestination.WenwenReader,
            WenwenReaderRouting.resolveDestination(book, showMangaUi = true),
        )
    }

    @Test
    fun resolveDestination_keepsAudioOnLegacyPlayer() {
        val book = Book(
            bookUrl = "https://example.com/audio/1",
            origin = "https://source.example",
            originName = "demo.mp3",
            type = BookType.audio,
        )

        assertEquals(
            WenwenReaderDestination.AudioPlayer,
            WenwenReaderRouting.resolveDestination(book, showMangaUi = true),
        )
    }

    @Test
    fun resolveDestination_keepsOnlineImageOnMangaReaderWhenEnabled() {
        val book = Book(
            bookUrl = "https://example.com/manga/1",
            origin = "https://source.example",
            originName = "demo",
            type = BookType.image,
        )

        assertEquals(
            WenwenReaderDestination.MangaReader,
            WenwenReaderRouting.resolveDestination(book, showMangaUi = true),
        )
    }

    @Test
    fun resolveDestination_usesNewReaderForOnlineText() {
        val book = Book(
            bookUrl = "https://example.com/book/1",
            origin = "https://source.example",
            originName = "demo",
            type = BookType.text,
        )

        assertEquals(
            WenwenReaderDestination.WenwenReader,
            WenwenReaderRouting.resolveDestination(book, showMangaUi = true),
        )
    }
}
