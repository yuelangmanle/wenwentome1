package com.wenwentome.reader.feature.library

import com.wenwentome.reader.core.model.BookRecord

data class LibraryUiState(
    val filter: LibraryFilter = LibraryFilter.DEFAULT,
    val visibleBooks: List<BookRecord> = emptyList(),
)

