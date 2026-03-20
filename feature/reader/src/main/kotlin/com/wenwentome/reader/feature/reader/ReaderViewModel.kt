package com.wenwentome.reader.feature.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wenwentome.reader.core.model.BookRecord
import com.wenwentome.reader.core.model.ReadingState
import com.wenwentome.reader.data.localbooks.ReaderContent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ReaderViewModel(
    private val bookId: String,
    observeBook: Flow<BookRecord?>,
    observeReadingState: Flow<ReadingState?>,
    observeContent: Flow<ReaderContent>,
    private val updateReadingState: suspend (ReadingState) -> Unit,
) : ViewModel() {
    val uiState: StateFlow<ReaderUiState> =
        combine(observeBook, observeReadingState, observeContent) { book, state, content ->
            ReaderUiState(
                book = book,
                locator = state?.locator,
                progressPercent = state?.progressPercent ?: 0f,
                chapterTitle = content.chapterTitle,
                chapterRef = content.chapterRef ?: state?.chapterRef,
                paragraphs = content.paragraphs,
                bookmarks = state?.bookmarks.orEmpty(),
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ReaderUiState(),
        )

    fun updateLocator(locator: String, chapterRef: String?, progressPercent: Float) {
        val state = uiState.value
        val resolvedBookId = state.book?.id ?: bookId
        viewModelScope.launch {
            updateReadingState(
                ReadingState(
                    bookId = resolvedBookId,
                    locator = locator,
                    chapterRef = chapterRef,
                    progressPercent = progressPercent,
                    bookmarks = state.bookmarks,
                    updatedAt = System.currentTimeMillis(),
                )
            )
        }
    }
}
