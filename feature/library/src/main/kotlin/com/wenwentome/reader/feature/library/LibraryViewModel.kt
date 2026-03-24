package com.wenwentome.reader.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryViewModel(
    private val observeBookshelf: ObserveBookshelfUseCase,
    private val importLocalBook: suspend (List<Uri>) -> Unit,
    private val refreshCatalogAction: suspend (String) -> Unit = {},
    private val filter: LibraryFilter = LibraryFilter.DEFAULT,
    private val sort: LibrarySort = LibrarySort.LAST_READ_DESC,
) : ViewModel() {
    private val filterState = MutableStateFlow(filter)
    private val sortState = MutableStateFlow(sort)
    private val importingState = MutableStateFlow(false)
    private val importErrorState = MutableStateFlow<String?>(null)

    val uiState: StateFlow<LibraryUiState> =
        combine(
            observeBookshelf(),
            filterState,
            sortState,
            importingState,
            importErrorState,
        ) { items, filter, sort, isImporting, importErrorMessage ->
            val filtered = filter.apply(items)
            val sorted = sort.apply(filtered)
            val continueReading = items
                .filter { it.progressPercent > 0f }
                .maxWithOrNull(
                    compareBy<LibraryBookItem> { it.lastReadAt }
                        .thenBy { it.progressPercent }
                )
            LibraryUiState(
                filter = filter,
                sort = sort,
                continueReading = continueReading,
                visibleBooks = sorted,
                isImporting = isImporting,
                importErrorMessage = importErrorMessage,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = LibraryUiState(filter = filter, sort = sort),
        )

    fun setFilter(filter: LibraryFilter) {
        filterState.value = filter
    }

    fun setSort(sort: LibrarySort) {
        sortState.value = sort
    }

    fun refreshCatalog(bookId: String) {
        viewModelScope.launch {
            refreshCatalogAction(bookId)
        }
    }

    fun import(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            importingState.value = true
            importErrorState.value = null
            try {
                importLocalBook(uris)
            } catch (error: Throwable) {
                importErrorState.value = error.toImportErrorMessage()
            } finally {
                importingState.value = false
            }
        }
    }

    private fun Throwable.toImportErrorMessage(): String {
        val detail = message?.trim().orEmpty()
        return if (detail.isBlank()) {
            "导入失败，请检查文件格式或读取权限后重试"
        } else {
            "导入失败：$detail"
        }
    }
}
