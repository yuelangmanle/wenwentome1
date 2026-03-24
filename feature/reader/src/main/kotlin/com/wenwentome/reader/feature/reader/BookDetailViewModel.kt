package com.wenwentome.reader.feature.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wenwentome.reader.core.model.BookRecord
import com.wenwentome.reader.core.model.OriginType
import com.wenwentome.reader.core.model.ReaderChapter
import com.wenwentome.reader.core.model.ReadingState
import com.wenwentome.reader.data.apihub.ability.BookMetadataEnhancementFacade
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
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
    private val metadataEnhancementFacade: BookMetadataEnhancementFacade? = null,
    private val applyMetadataAction: suspend (BookRecord) -> Unit = {},
    private val updateReadingState: suspend (ReadingState) -> Unit,
) : ViewModel() {
    private val mutableEvents = MutableSharedFlow<BookDetailEvent>(extraBufferCapacity = 1)
    private val metadataState = MutableStateFlow(BookMetadataState())
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
    private val detailEnhancementState =
        combine(coverState, metadataState) { coverState, metadataState ->
            coverState to metadataState
        }

    val uiState: StateFlow<BookDetailUiState> =
        combine(
            observeBook,
            readingState,
            observeChapters,
            observeLatestChapterRef,
            detailEnhancementState,
        ) { book, readingState, chapters, latestChapterRef, detailEnhancementState ->
            val (coverState, metadataState) = detailEnhancementState
            val (automaticCover, manualCover) = coverState
            val normalizedBook = book?.let(::normalizeBookRecord)
            val progressPercent = readingState?.progressPercent ?: 0f
            val currentChapterTitle = chapters
                .firstOrNull { it.chapterRef == readingState?.chapterRef }
                ?.title
            BookDetailUiState(
                book = normalizedBook,
                effectiveCover = manualCover ?: automaticCover ?: normalizedBook?.cover,
                readActionLabel = resolveReadActionLabel(readingState),
                progressPercent = progressPercent,
                progressLabel = formatProgressLabel(progressPercent),
                currentChapterRef = readingState?.chapterRef,
                currentChapterTitle = currentChapterTitle,
                lastReadLabel = currentChapterTitle?.let { "上次读到 $it" },
                showTocAction = chapters.isNotEmpty(),
                showRefreshCatalogAction =
                    normalizedBook?.originType == OriginType.WEB || normalizedBook?.originType == OriginType.MIXED,
                showJumpToLatestAction = !latestChapterRef.isNullOrBlank(),
                canRestoreAutomaticCover = !manualCover.isNullOrBlank(),
                isEnhancingMetadata = metadataState.isLoading,
                metadataError = metadataState.errorMessage,
                aiSummary = metadataState.aiSummary,
                authorIntroduction = metadataState.authorIntroduction,
                aiTags = metadataState.aiTags,
                suggestedCoverUri = metadataState.suggestedCoverUri,
                canApplyMetadataSuggestion =
                    !metadataState.aiSummary.isNullOrBlank() ||
                        !metadataState.suggestedCoverUri.isNullOrBlank(),
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

    fun enhanceMetadata() {
        val book = uiState.value.book ?: return
        val facade = metadataEnhancementFacade
        if (facade == null) {
            metadataState.update { it.copy(errorMessage = "当前还没有配置 AI 补全能力。") }
            return
        }
        viewModelScope.launch {
            metadataState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                facade.enhance(book)
            }.onSuccess { result ->
                metadataState.update {
                    it.copy(
                        isLoading = false,
                        aiSummary = result.improvedSummary,
                        authorIntroduction = result.authorIntroduction,
                        aiTags = result.tags,
                        suggestedCoverUri = result.suggestedCoverUri,
                        errorMessage = null,
                    )
                }
            }.onFailure { error ->
                metadataState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "AI 补全失败",
                    )
                }
            }
        }
    }

    fun applyMetadataSuggestion() {
        val book = uiState.value.book ?: return
        val summary = uiState.value.aiSummary
        val cover = uiState.value.suggestedCoverUri
        if (summary.isNullOrBlank() && cover.isNullOrBlank()) {
            return
        }
        viewModelScope.launch {
            applyMetadataAction(
                book.copy(
                    summary = summary ?: book.summary,
                    cover = cover ?: book.cover,
                    updatedAt = System.currentTimeMillis(),
                )
            )
        }
    }

    private fun emitOpenReader() {
        mutableEvents.tryEmit(BookDetailEvent.OpenReader(bookId = bookId))
    }
}

private data class BookMetadataState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val aiSummary: String? = null,
    val authorIntroduction: String? = null,
    val aiTags: List<String> = emptyList(),
    val suggestedCoverUri: String? = null,
)

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

private fun normalizeBookRecord(book: BookRecord): BookRecord {
    val normalizedTitle = book.title.trim().ifBlank { "未命名" }
    val normalizedAuthor = book.author?.trim()?.takeIf { it.isNotBlank() }
    return if (normalizedTitle == book.title && normalizedAuthor == book.author) {
        book
    } else {
        book.copy(
            title = normalizedTitle,
            author = normalizedAuthor,
        )
    }
}
