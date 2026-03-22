package com.wenwentome.reader.feature.library

import com.wenwentome.reader.core.model.OriginType

enum class LibraryFilter {
    DEFAULT,
    LOCAL_ONLY,
    WEB_ONLY,
    ;

    fun apply(items: List<LibraryBookItem>): List<LibraryBookItem> =
        when (this) {
            DEFAULT -> items
            LOCAL_ONLY -> items.filter { it.book.originType == OriginType.LOCAL }
            WEB_ONLY -> items.filter { it.book.originType == OriginType.WEB || it.book.originType == OriginType.MIXED }
        }
}
