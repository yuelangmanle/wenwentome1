package io.legado.app.ui.wenwen

import android.content.Context
import android.content.Intent
import io.legado.app.data.entities.Book

sealed interface WenwenBookLookup {
    data class ByBookUrl(
        val bookUrl: String,
    ) : WenwenBookLookup

    data class ByNameAuthor(
        val name: String,
        val author: String,
    ) : WenwenBookLookup

    data object Missing : WenwenBookLookup
}

object WenwenUiBridge {
    const val EXTRA_BOOK_URL = "bookUrl"
    const val EXTRA_NAME = "name"
    const val EXTRA_AUTHOR = "author"

    fun resolveBookLookup(
        bookUrl: String?,
        name: String?,
        author: String?,
    ): WenwenBookLookup {
        val normalizedBookUrl = bookUrl?.trim().orEmpty()
        if (normalizedBookUrl.isNotEmpty()) {
            return WenwenBookLookup.ByBookUrl(normalizedBookUrl)
        }

        val normalizedName = name?.trim().orEmpty()
        val normalizedAuthor = author?.trim().orEmpty()
        return if (normalizedName.isNotEmpty() && normalizedAuthor.isNotEmpty()) {
            WenwenBookLookup.ByNameAuthor(
                name = normalizedName,
                author = normalizedAuthor,
            )
        } else {
            WenwenBookLookup.Missing
        }
    }

    fun detailIntent(
        context: Context,
        bookUrl: String? = null,
        name: String? = null,
        author: String? = null,
    ): Intent = Intent(context, WenwenBookDetailActivity::class.java).apply {
        putExtra(EXTRA_BOOK_URL, bookUrl)
        putExtra(EXTRA_NAME, name)
        putExtra(EXTRA_AUTHOR, author)
    }
}

fun Context.startWenwenBookDetailActivity(
    bookUrl: String? = null,
    name: String? = null,
    author: String? = null,
) {
    startActivity(
        WenwenUiBridge.detailIntent(
            context = this,
            bookUrl = bookUrl,
            name = name,
            author = author,
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}

fun Context.startWenwenBookDetailActivity(book: Book) {
    startWenwenBookDetailActivity(
        bookUrl = book.bookUrl,
        name = book.name,
        author = book.author,
    )
}
