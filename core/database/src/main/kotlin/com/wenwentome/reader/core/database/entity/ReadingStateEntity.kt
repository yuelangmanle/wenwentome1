package com.wenwentome.reader.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.wenwentome.reader.core.model.ReadingBookmark

/**
 * 只持久化正文逻辑锚点，不保存视觉页码，避免和阅读模式强绑定。
 */
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
