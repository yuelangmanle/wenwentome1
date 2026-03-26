package com.wenwentome.reader.feature.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wenwentome.reader.core.model.BookRecord
import com.wenwentome.reader.core.model.BookFormat
import com.wenwentome.reader.core.model.ReaderChapter
import com.wenwentome.reader.core.model.ReaderMode
import com.wenwentome.reader.core.model.ReaderPresentationPrefs
import com.wenwentome.reader.core.model.ReadingState
import com.wenwentome.reader.data.apihub.ability.ReaderAbilityFacade
import com.wenwentome.reader.data.apihub.ability.ReaderAbilityInput
import com.wenwentome.reader.data.localbooks.ReaderContent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    private val readerAbilityFacade: ReaderAbilityFacade? = null,
) : ViewModel() {
    private val assistantState = MutableStateFlow(ReaderAssistantUiState())

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
        combine(observeBookState, observeReaderPrefs, observeCatalog, assistantState) { bookState, prefs, catalog, assistant ->
            val (book, state, content) = bookState
            val (readerMode, presentation) = prefs
            val (chapters, latestChapterRef) = catalog
            val chapterRef = content.chapterRef ?: state?.chapterRef
            val progressPercent =
                content.derivedProgressPercent(
                    format = book?.primaryFormat,
                    locator = state?.locator,
                    fallbackProgressPercent = state?.progressPercent ?: 0f,
                )
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
                windowStartParagraphIndex = content.windowStartParagraphIndex,
                totalParagraphCount = content.totalParagraphCount,
                bookmarks = state?.bookmarks.orEmpty(),
                assistant = assistant,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ReaderUiState(),
        )

    fun setReaderMode(mode: ReaderMode) {
        viewModelScope.launch {
            persistCurrentReadingState()
            saveReaderMode(mode)
        }
    }

    fun updatePresentation(prefs: ReaderPresentationPrefs) {
        viewModelScope.launch {
            savePresentationPrefs(prefs)
        }
    }

    fun setFontSize(fontSizeSp: Float) {
        updatePresentation { it.copy(fontSizeSp = fontSizeSp.coerceIn(14f, 30f)) }
    }

    fun setAutoFitFontSize(enabled: Boolean) {
        updatePresentation { it.copy(autoFitFontSize = enabled) }
    }

    fun setLineHeightMultiplier(multiplier: Float) {
        updatePresentation { it.copy(lineHeightMultiplier = multiplier.coerceIn(1.2f, 2.2f)) }
    }

    fun setLetterSpacing(em: Float) {
        updatePresentation { it.copy(letterSpacingEm = em.coerceIn(-0.05f, 0.2f)) }
    }

    fun setParagraphSpacing(em: Float) {
        updatePresentation { it.copy(paragraphSpacingEm = em.coerceIn(0f, 1f)) }
    }

    fun setSidePaddingDp(paddingDp: Int) {
        updatePresentation { it.copy(sidePaddingDp = paddingDp.coerceIn(8, 40)) }
    }

    fun setBackgroundPalette(key: String) {
        updatePresentation { it.copy(backgroundPaletteKey = key) }
    }

    private fun updatePresentation(transform: (ReaderPresentationPrefs) -> ReaderPresentationPrefs) {
        updatePresentation(transform(uiState.value.presentation))
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
                    progressPercent = 0f,
                    bookmarks = state.bookmarks,
                    updatedAt = System.currentTimeMillis(),
                )
            )
        }
    }

    fun generateChapterSummary() {
        executeAssistantRequest { facade, input ->
            val result = facade.summarizeChapter(input)
            assistantState.update {
                it.copy(
                    isLoading = false,
                    summary = result.text,
                    errorMessage = null,
                )
            }
        }
    }

    fun explainCurrentParagraph() {
        executeAssistantRequest { facade, input ->
            val result = facade.explainParagraph(input)
            assistantState.update {
                it.copy(
                    isLoading = false,
                    explanation = result.text,
                    errorMessage = null,
                )
            }
        }
    }

    fun translateCurrentParagraph() {
        executeAssistantRequest { facade, input ->
            val result = facade.translateParagraph(input)
            assistantState.update {
                it.copy(
                    isLoading = false,
                    translation = result.text,
                    errorMessage = null,
                )
            }
        }
    }

    fun speakCurrentChapter() {
        executeAssistantRequest { facade, input ->
            val result = facade.speakChapter(input)
            assistantState.update {
                it.copy(
                    isLoading = false,
                    ttsScript = result.text,
                    errorMessage = null,
                )
            }
        }
    }

    private fun executeAssistantRequest(
        block: suspend (ReaderAbilityFacade, ReaderAbilityInput) -> Unit,
    ) {
        val facade = readerAbilityFacade
        if (facade == null) {
            assistantState.update { it.copy(errorMessage = "当前还没有配置可用的 AI 能力。") }
            return
        }

        val input = currentAbilityInput()
        if (input == null) {
            assistantState.update { it.copy(errorMessage = "当前章节暂无可分析的正文内容。") }
            return
        }

        viewModelScope.launch {
            assistantState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                block(facade, input)
            }.onFailure { error ->
                assistantState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "调用 AI 失败",
                    )
                }
            }
        }
    }

    private suspend fun persistCurrentReadingState() {
        val state = uiState.value
        val locator = state.locatorForSave() ?: return
        updateReadingState(
            ReadingState(
                bookId = state.book?.id ?: bookId,
                locator = locator,
                chapterRef = state.chapterRef,
                progressPercent = state.progressPercent,
                bookmarks = state.bookmarks,
                updatedAt = System.currentTimeMillis(),
            )
        )
    }

    private fun currentAbilityInput(): ReaderAbilityInput? {
        val state = uiState.value
        val paragraphs = state.paragraphs.takeIf { it.isNotEmpty() } ?: return null
        return ReaderAbilityInput(
            bookId = state.book?.id ?: bookId,
            chapterRef = state.chapterRef,
            chapterTitle = state.chapterTitle,
            paragraphs = paragraphs,
            selectedParagraph = paragraphs.firstOrNull(),
        )
    }
}

private fun List<ReaderChapter>.withLatestFlag(latestChapterRef: String?): List<ReaderChapter> {
    if (latestChapterRef.isNullOrBlank()) return this
    return map { chapter ->
        chapter.copy(isLatest = chapter.isLatest || chapter.chapterRef == latestChapterRef)
    }
}

private fun ReaderContent.derivedProgressPercent(
    format: BookFormat?,
    locator: String?,
    fallbackProgressPercent: Float,
): Float {
    val paragraphCount = totalParagraphCount.coerceAtLeast(paragraphs.size)
    if (paragraphCount <= 1) return fallbackProgressPercent
    val paragraphIndex =
        when (format) {
            BookFormat.TXT,
            BookFormat.EPUB -> windowStartParagraphIndex
            BookFormat.WEB -> locateParagraphIndex(format, locator)
            null -> windowStartParagraphIndex
        }
    return (paragraphIndex.coerceIn(0, paragraphCount - 1) / (paragraphCount - 1).toFloat())
}

private fun locateParagraphIndex(
    format: BookFormat,
    locator: String?,
): Int = com.wenwentome.reader.core.model.resolveReaderParagraphIndex(format, locator)
