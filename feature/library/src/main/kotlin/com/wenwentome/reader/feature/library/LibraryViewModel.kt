package com.wenwentome.reader.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryViewModel(
    private val observeBookshelf: ObserveBookshelfUseCase,
    private val importLocalBook: suspend (Uri) -> Unit,
    private val refreshCatalogAction: suspend (String) -> Unit = {},
    private val filter: LibraryFilter = LibraryFilter.DEFAULT,
) : ViewModel() {
    val uiState: StateFlow<LibraryUiState> =
        observeBookshelf()
            .map { items ->
                LibraryUiState(
                    filter = filter,
                    continueReading = items
                        .filter { it.progressPercent > 0f }
                        .maxWithOrNull(
                            compareBy<LibraryBookItem> { it.lastReadAt }
                                .thenBy { it.progressPercent }
                        ),
                    visibleBooks = filter.apply(items),
                )
            }.stateIn(
                scope = viewModelScope,
                started = kotlinx.coroutines.flow.SharingStarted.Eagerly,
                initialValue = LibraryUiState(filter = filter),
            )

    fun refreshCatalog(bookId: String) {
        viewModelScope.launch {
            refreshCatalogAction(bookId)
        }
    }

    fun import(uri: Uri) {
        viewModelScope.launch {
            importLocalBook(uri)
        }
    }
}
