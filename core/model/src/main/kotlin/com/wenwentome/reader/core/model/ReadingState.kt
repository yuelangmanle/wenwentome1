package com.wenwentome.reader.core.model

data class ReadingBookmark(
    val chapterRef: String?,
    val locator: String,
    val label: String,
)

/**
 * `chapterRef` + `locator` 共同表示正文逻辑锚点。
 * 这里不保存与阅读呈现模式绑定的视觉页码。
 */
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
