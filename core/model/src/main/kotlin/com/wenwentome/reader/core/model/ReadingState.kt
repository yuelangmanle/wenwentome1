package com.wenwentome.reader.core.model

data class ReadingBookmark(
    val chapterRef: String?,
    val locator: String,
    val label: String,
)

data class ReadingState(
    val bookId: String,
    val locator: String? = null,
    val chapterRef: String? = null,
    val progressPercent: Float = 0f,
    val bookmarks: List<ReadingBookmark> = emptyList(),
    val notes: List<String> = emptyList(),
    val updatedAt: Long = System.currentTimeMillis(),
) {
    companion object {
        fun initial(bookId: String) = ReadingState(bookId = bookId)
    }
}

