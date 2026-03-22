package com.wenwentome.reader.feature.reader

import com.wenwentome.reader.core.model.BookRecord
import com.wenwentome.reader.core.model.ReaderChapter
import com.wenwentome.reader.core.model.ReaderMode
import com.wenwentome.reader.core.model.ReaderPresentationPrefs
import com.wenwentome.reader.core.model.ReadingBookmark

data class ReaderUiState(
    val book: BookRecord? = null,
    val readerMode: ReaderMode = ReaderMode.SIMULATED_PAGE_TURN,
    val presentation: ReaderPresentationPrefs = ReaderPresentationPrefs(),
    val chapters: List<ReaderChapter> = emptyList(),
    val latestChapterRef: String? = null,
    val tocHighlightedChapterRef: String? = null,
    val locator: String? = null,
    val progressPercent: Float = 0f,
    val progressLabel: String = "0%",
    val chapterTitle: String? = null,
    val chapterRef: String? = null,
    val paragraphs: List<String> = emptyList(),
    val bookmarks: List<ReadingBookmark> = emptyList(),
    val assistant: ReaderAssistantUiState = ReaderAssistantUiState(),
)
