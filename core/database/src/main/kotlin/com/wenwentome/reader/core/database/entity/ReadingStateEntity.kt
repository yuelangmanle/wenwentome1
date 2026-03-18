package com.wenwentome.reader.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.wenwentome.reader.core.model.ReadingBookmark

@Entity(tableName = "reading_states")
data class ReadingStateEntity(
    @PrimaryKey val bookId: String,
    val locator: String? = null,
    val chapterRef: String? = null,
    val progressPercent: Float = 0f,
    val bookmarks: List<ReadingBookmark> = emptyList(),
    val notes: List<String> = emptyList(),
    val updatedAt: Long = System.currentTimeMillis(),
)

