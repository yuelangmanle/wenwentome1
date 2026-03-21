package com.wenwentome.reader.feature.reader

import com.wenwentome.reader.core.model.BookRecord
import com.wenwentome.reader.core.model.ReaderChapter

data class BookDetailUiState(
    val book: BookRecord? = null,
    val effectiveCover: String? = null,
    val readActionLabel: String = "开始阅读",
    val progressPercent: Float = 0f,
    val progressLabel: String = "0%",
    val currentChapterRef: String? = null,
    val currentChapterTitle: String? = null,
    val lastReadLabel: String? = null,
    val showTocAction: Boolean = true,
    val showRefreshCatalogAction: Boolean = false,
    val showJumpToLatestAction: Boolean = false,
    val canRestoreAutomaticCover: Boolean = false,
    val latestChapterRef: String? = null,
    val latestChapterTitle: String? = null,
    val chapters: List<ReaderChapter> = emptyList(),
)
