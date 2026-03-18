package com.wenwentome.reader.feature.library

import com.wenwentome.reader.core.model.BookRecord
import com.wenwentome.reader.core.model.OriginType

enum class LibraryFilter {
    DEFAULT,
    LOCAL_ONLY,
    WEB_ONLY,
    ;

    fun apply(books: List<BookRecord>): List<BookRecord> =
        when (this) {
            DEFAULT -> books
            LOCAL_ONLY -> books.filter { it.originType == OriginType.LOCAL }
            WEB_ONLY -> books.filter { it.originType == OriginType.WEB }
        }
}

