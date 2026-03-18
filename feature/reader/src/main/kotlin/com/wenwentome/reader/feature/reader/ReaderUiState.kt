package com.wenwentome.reader.feature.reader

import com.wenwentome.reader.core.model.BookRecord
import com.wenwentome.reader.core.model.ReadingBookmark

data class ReaderUiState(
    val book: BookRecord? = null,
    val locator: String? = null,
    val progressPercent: Float = 0f,
    val chapterTitle: String? = null,
    val paragraphs: List<String> = emptyList(),
    val bookmarks: List<ReadingBookmark> = emptyList(),
)
