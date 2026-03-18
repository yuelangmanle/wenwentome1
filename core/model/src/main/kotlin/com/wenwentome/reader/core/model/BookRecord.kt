package com.wenwentome.reader.core.model

import java.util.UUID

enum class OriginType { LOCAL, WEB, MIXED }
enum class BookFormat { TXT, EPUB, WEB }
enum class BookshelfState { IN_SHELF, ARCHIVED, FAVORITE }

data class BookRecord(
    val id: String,
    val title: String,
    val author: String? = null,
    val originType: OriginType,
    val primaryFormat: BookFormat,
    val cover: String? = null,
    val summary: String? = null,
    val bookshelfState: BookshelfState = BookshelfState.IN_SHELF,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
) {
    companion object {
        fun newLocal(title: String, author: String?, format: BookFormat) = BookRecord(
            id = UUID.randomUUID().toString(),
            title = title,
            author = author,
            originType = OriginType.LOCAL,
            primaryFormat = format,
        )
    }
}

