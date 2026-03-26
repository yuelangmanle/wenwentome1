package io.legado.app.ui.wenwen

import android.content.Context
import android.content.Intent
import io.legado.app.data.entities.Book
import io.legado.app.help.book.isAudio
import io.legado.app.help.book.isEpub
import io.legado.app.help.book.isImage
import io.legado.app.help.book.isLocal
import io.legado.app.help.book.isLocalTxt
import io.legado.app.help.config.AppConfig
import io.legado.app.ui.book.audio.AudioPlayActivity
import io.legado.app.ui.book.manga.ReadMangaActivity
import io.legado.app.ui.book.read.ReadBookActivity

enum class WenwenReaderDestination {
    WenwenReader,
    AudioPlayer,
    MangaReader,
    LegacyReader,
}

object WenwenReaderRouting {

    const val EXTRA_BOOK_URL = "bookUrl"

    fun resolveDestination(
        book: Book,
        showMangaUi: Boolean = AppConfig.showMangaUi,
    ): WenwenReaderDestination = when {
        book.isAudio -> WenwenReaderDestination.AudioPlayer
        !book.isLocal && book.isImage && showMangaUi -> WenwenReaderDestination.MangaReader
        book.isLocalTxt || book.isEpub || (!book.isLocal && !book.isImage) ->
            WenwenReaderDestination.WenwenReader
        else -> WenwenReaderDestination.LegacyReader
    }

    fun createIntent(
        context: Context,
        book: Book,
        showMangaUi: Boolean = AppConfig.showMangaUi,
        configIntent: Intent.() -> Unit = {},
    ): Intent {
        val targetClass = when (resolveDestination(book, showMangaUi)) {
            WenwenReaderDestination.WenwenReader -> WenwenReaderActivity::class.java
            WenwenReaderDestination.AudioPlayer -> AudioPlayActivity::class.java
            WenwenReaderDestination.MangaReader -> ReadMangaActivity::class.java
            WenwenReaderDestination.LegacyReader -> ReadBookActivity::class.java
        }
        return Intent(context, targetClass).apply {
            putExtra(EXTRA_BOOK_URL, book.bookUrl)
            configIntent()
        }
    }
}
