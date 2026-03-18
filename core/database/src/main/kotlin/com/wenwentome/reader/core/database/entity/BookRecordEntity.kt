package com.wenwentome.reader.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.wenwentome.reader.core.model.BookFormat
import com.wenwentome.reader.core.model.BookshelfState
import com.wenwentome.reader.core.model.OriginType

@Entity(tableName = "book_records")
data class BookRecordEntity(
    @PrimaryKey val id: String,
    val title: String,
    val author: String? = null,
    val originType: OriginType = OriginType.LOCAL,
    val primaryFormat: BookFormat = BookFormat.TXT,
    val cover: String? = null,
    val summary: String? = null,
    val bookshelfState: BookshelfState = BookshelfState.IN_SHELF,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
)

