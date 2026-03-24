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
    val sort: LibrarySort = LibrarySort.LAST_READ_DESC,
    val continueReading: LibraryBookItem? = null,
    val visibleBooks: List<LibraryBookItem> = emptyList(),
    val isImporting: Boolean = false,
    val importErrorMessage: String? = null,
)
