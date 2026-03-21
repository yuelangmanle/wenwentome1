package com.wenwentome.reader.feature.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wenwentome.reader.core.model.BookRecord
import com.wenwentome.reader.core.model.OriginType
import com.wenwentome.reader.core.model.ReaderChapter
import com.wenwentome.reader.core.model.ReadingState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface BookDetailEvent {
    data class OpenReader(
        val bookId: String,
    ) : BookDetailEvent
}

class BookDetailViewModel(
    private val bookId: String,
    observeBook: Flow<BookRecord?>,
    observeReadingState: Flow<ReadingState?>,
    observeChapters: Flow<List<ReaderChapter>>,
    observeLatestChapterRef: Flow<String?> = flowOf(null),
    observeAutomaticCover: Flow<String?> = flowOf(null),
    observeManualCover: Flow<String?> = flowOf(null),
    private val refreshCatalogAction: suspend (String) -> Unit = {},
    private val refreshCoverAction: suspend () -> Unit = {},
    private val importCoverAction: suspend (ByteArray, String) -> Unit = { _, _ -> },
    private val restoreAutomaticCoverAction: suspend () -> Unit = {},
    private val updateReadingState: suspend (ReadingState) -> Unit,
) : ViewModel() {
    private val mutableEvents = MutableSharedFlow<BookDetailEvent>(extraBufferCapacity = 1)
    private val readingState =
        observeReadingState.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    val events = mutableEvents.asSharedFlow()
    private val coverState =
        combine(observeAutomaticCover, observeManualCover) { automaticCover, manualCover ->
            automaticCover to manualCover
        }

    val uiState: StateFlow<BookDetailUiState> =
        combine(
            observeBook,
            readingState,
            observeChapters,
            observeLatestChapterRef,
            coverState,
        ) { book, readingState, chapters, latestChapterRef, coverState ->
            val (automaticCover, manualCover) = coverState
            val progressPercent = readingState?.progressPercent ?: 0f
            val currentChapterTitle = chapters
                .firstOrNull { it.chapterRef == readingState?.chapterRef }
                ?.title
            BookDetailUiState(
                book = book,
                effectiveCover = manualCover ?: automaticCover ?: book?.cover,
                readActionLabel = resolveReadActionLabel(readingState),
                progressPercent = progressPercent,
                progressLabel = formatProgressLabel(progressPercent),
                currentChapterRef = readingState?.chapterRef,
                lastReadLabel = currentChapterTitle?.let { "上次读到 $it" },
                showTocAction = chapters.isNotEmpty(),
                showRefreshCatalogAction = book?.originType == OriginType.WEB || book?.originType == OriginType.MIXED,
                showJumpToLatestAction = !latestChapterRef.isNullOrBlank(),
                canRestoreAutomaticCover = !manualCover.isNullOrBlank(),
                latestChapterRef = latestChapterRef,
                latestChapterTitle = chapters.firstOrNull { it.chapterRef == latestChapterRef }?.title,
                chapters = chapters,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = BookDetailUiState(),
        )

    fun openReader() {
        emitOpenReader()
    }

    fun openChapter(chapterRef: String) {
        val state = uiState.value
        val currentReadingState = readingState.value
        val locator = buildLocatorForChapter(state.book?.primaryFormat, chapterRef) ?: return
        val current = state.book?.id ?: bookId
        viewModelScope.launch {
            updateReadingState(
                ReadingState(
                    bookId = current,
                    locator = locator,
                    chapterRef = chapterRef,
                    progressPercent = 0f,
                    bookmarks = currentReadingState?.bookmarks.orEmpty(),
                    notes = currentReadingState?.notes.orEmpty(),
                    updatedAt = System.currentTimeMillis(),
                )
            )
            emitOpenReader()
        }
    }

    fun jumpToLatest() {
        val state = uiState.value
        val currentReadingState = readingState.value
        val latestRef = state.latestChapterRef ?: state.currentChapterRef
        if (latestRef.isNullOrBlank()) return
        val locator = buildLocatorForChapter(state.book?.primaryFormat, latestRef) ?: return
        val current = state.book?.id ?: bookId
        viewModelScope.launch {
            updateReadingState(
                ReadingState(
                    bookId = current,
                    locator = locator,
                    chapterRef = latestRef,
                    progressPercent = 0f,
                    bookmarks = currentReadingState?.bookmarks.orEmpty(),
                    notes = currentReadingState?.notes.orEmpty(),
                    updatedAt = System.currentTimeMillis(),
                )
            )
            emitOpenReader()
        }
    }

    fun refreshCatalog() {
        viewModelScope.launch {
            refreshCatalogAction(bookId)
        }
    }

    fun refreshCover() {
        viewModelScope.launch {
            refreshCoverAction()
        }
    }

    fun importCover(bytes: ByteArray, mime: String) {
        viewModelScope.launch {
            importCoverAction(bytes, mime)
        }
    }

    fun restoreAutomaticCover() {
        viewModelScope.launch {
            restoreAutomaticCoverAction()
        }
    }

    private fun emitOpenReader() {
        mutableEvents.tryEmit(BookDetailEvent.OpenReader(bookId = bookId))
    }
}

private fun resolveReadActionLabel(readingState: ReadingState?): String =
    if (
        readingState?.locator?.isNotBlank() == true ||
        readingState?.chapterRef?.isNotBlank() == true ||
        (readingState?.progressPercent ?: 0f) > 0f
    ) {
        "继续阅读"
    } else {
        "开始阅读"
    }
