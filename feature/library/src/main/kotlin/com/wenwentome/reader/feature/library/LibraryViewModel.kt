package com.wenwentome.reader.feature.library

import android.net.Uri
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LibraryViewModel(
    private val observeBookshelf: ObserveBookshelfUseCase,
    private val importLocalBook: suspend (Uri) -> Unit,
    private val filter: LibraryFilter = LibraryFilter.DEFAULT,
) {
    val uiState: Flow<LibraryUiState> =
        observeBookshelf()
            .map { books ->
                LibraryUiState(
                    filter = filter,
                    visibleBooks = filter.apply(books),
                )
            }

    suspend fun import(uri: Uri) {
        importLocalBook(uri)
    }
}

