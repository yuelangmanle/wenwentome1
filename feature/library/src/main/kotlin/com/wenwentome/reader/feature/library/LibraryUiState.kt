package com.wenwentome.reader.feature.library

import com.wenwentome.reader.core.model.BookRecord

data class LibraryBookItem(
    val book: BookRecord,
    val effectiveCover: String?,
    val progressPercent: Float,
    val progressLabel: String,
    val hasUpdates: Boolean,
    val canRestoreAutomaticCover: Boolean,
    val lastReadAt: Long = 0L,
)

data class LibraryUiState(
    val filter: LibraryFilter = LibraryFilter.DEFAULT,
    val continueReading: LibraryBookItem? = null,
    val visibleBooks: List<LibraryBookItem> = emptyList(),
)
