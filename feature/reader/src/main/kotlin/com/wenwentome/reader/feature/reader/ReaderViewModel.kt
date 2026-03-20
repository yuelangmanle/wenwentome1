package com.wenwentome.reader.feature.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wenwentome.reader.core.model.BookRecord
import com.wenwentome.reader.core.model.BookFormat
import com.wenwentome.reader.core.model.ReaderChapter
import com.wenwentome.reader.core.model.ReaderMode
import com.wenwentome.reader.core.model.ReaderPresentationPrefs
import com.wenwentome.reader.core.model.ReadingState
import com.wenwentome.reader.data.localbooks.ReaderContent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class ReaderViewModel(
    private val bookId: String,
    observeBook: Flow<BookRecord?>,
    observeReadingState: Flow<ReadingState?>,
    observeContent: Flow<ReaderContent>,
    observeReaderMode: Flow<ReaderMode> = flowOf(ReaderMode.SIMULATED_PAGE_TURN),
    observePresentationPrefs: Flow<ReaderPresentationPrefs> = flowOf(ReaderPresentationPrefs()),
    observeChapters: Flow<List<ReaderChapter>> = flowOf(emptyList()),
    observeLatestChapterRef: Flow<String?> = flowOf(null),
    private val saveReaderMode: suspend (ReaderMode) -> Unit = {},
    private val savePresentationPrefs: suspend (ReaderPresentationPrefs) -> Unit = {},
    private val updateReadingState: suspend (ReadingState) -> Unit,
) : ViewModel() {
    private val observeBookState =
        combine(observeBook, observeReadingState, observeContent) { book, state, content ->
            Triple(book, state, content)
        }

    private val observeReaderPrefs =
        combine(observeReaderMode, observePresentationPrefs) { readerMode, presentation ->
            readerMode to presentation
        }

    private val observeCatalog =
        combine(observeChapters, observeLatestChapterRef) { chapters, latestChapterRef ->
            chapters to latestChapterRef
        }

    val uiState: StateFlow<ReaderUiState> =
        combine(observeBookState, observeReaderPrefs, observeCatalog) { bookState, prefs, catalog ->
            val (book, state, content) = bookState
            val (readerMode, presentation) = prefs
            val (chapters, latestChapterRef) = catalog
            val chapterRef = content.chapterRef ?: state?.chapterRef
            val progressPercent = state?.progressPercent ?: 0f
            ReaderUiState(
                book = book,
                readerMode = readerMode,
                presentation = presentation,
                chapters = chapters.withLatestFlag(latestChapterRef),
                latestChapterRef = latestChapterRef,
                tocHighlightedChapterRef = chapterRef,
                locator = state?.locator,
                progressPercent = progressPercent,
                progressLabel = formatProgressLabel(progressPercent),
                chapterTitle = content.chapterTitle,
                chapterRef = chapterRef,
                paragraphs = content.paragraphs,
                bookmarks = state?.bookmarks.orEmpty(),
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ReaderUiState(),
        )

    fun setReaderMode(mode: ReaderMode) {
        viewModelScope.launch {
            saveReaderMode(mode)
        }
    }

    fun updatePresentation(prefs: ReaderPresentationPrefs) {
        viewModelScope.launch {
            savePresentationPrefs(prefs)
        }
    }

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

    fun jumpToChapter(chapterRef: String) {
        val state = uiState.value
        val resolvedChapter = state.chapters.firstOrNull { it.chapterRef == chapterRef } ?: return
        val resolvedLocator =
            resolvedChapter.locatorHint?.takeIf { it.isNotBlank() }
                ?: buildLocatorForChapter(
                    format = state.book?.primaryFormat,
                    chapterRef = resolvedChapter.chapterRef,
                )
                ?: return

        val resolvedBookId = state.book?.id ?: bookId
        viewModelScope.launch {
            updateReadingState(
                ReadingState(
                    bookId = resolvedBookId,
                    locator = resolvedLocator,
                    chapterRef = resolvedChapter.chapterRef,
                    progressPercent = state.progressPercent,
                    bookmarks = state.bookmarks,
                    updatedAt = System.currentTimeMillis(),
                )
            )
        }
    }
}

private fun formatProgressLabel(progressPercent: Float): String =
    "${(progressPercent.coerceIn(0f, 1f) * 100).roundToInt()}%"

private fun List<ReaderChapter>.withLatestFlag(latestChapterRef: String?): List<ReaderChapter> {
    if (latestChapterRef.isNullOrBlank()) return this
    return map { chapter ->
        chapter.copy(isLatest = chapter.isLatest || chapter.chapterRef == latestChapterRef)
    }
}

private fun buildLocatorForChapter(format: BookFormat?, chapterRef: String): String? =
    when (format) {
        BookFormat.TXT -> "0"
        BookFormat.EPUB -> "chapter:$chapterRef#paragraph:0"
        BookFormat.WEB -> chapterRef
        null -> chapterRef
    }
