package com.wenwentome.reader.feature.reader

import com.wenwentome.reader.core.model.BookFormat
import com.wenwentome.reader.core.model.BookRecord
import com.wenwentome.reader.core.model.OriginType
import com.wenwentome.reader.core.model.ReaderChapter
import com.wenwentome.reader.core.model.ReadingState
import com.wenwentome.reader.core.model.buildReaderChapterLocator
import com.wenwentome.reader.data.apihub.ability.BookMetadataEnhancementFacade
import com.wenwentome.reader.data.apihub.ability.BookMetadataEnhancementResult
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class BookDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun importCover_setsManualCoverAssetAndExposesRestoreAction() = runTest {
        val manualCover = MutableStateFlow<String?>(null)
        var importedMime: String? = null
        val viewModel = BookDetailViewModel(
            bookId = "book-1",
            observeBook = flowOf(sampleLocalBook()),
            observeReadingState = flowOf(ReadingState(bookId = "book-1")),
            observeChapters = flowOf(sampleChapters()),
            observeLatestChapterRef = flowOf("chapter-12"),
            observeAutomaticCover = flowOf("file:///auto-cover.jpg"),
            observeManualCover = manualCover,
            importCoverAction = { _, mime ->
                importedMime = mime
                manualCover.value = "file:///manual-cover.png"
            },
            updateReadingState = {},
        )

        viewModel.uiState.first { it.book != null }
        viewModel.importCover(sampleImageBytes, "image/png")
        advanceUntilIdle()

        assertEquals("image/png", importedMime)
        assertEquals(true, viewModel.uiState.value.canRestoreAutomaticCover)
        assertEquals("file:///manual-cover.png", viewModel.uiState.value.effectiveCover)
    }

    @Test
    fun webBookDetail_showsRefreshAndJumpToLatestActions() = runTest {
        val viewModel = BookDetailViewModel(
            bookId = "book-1",
            observeBook = flowOf(sampleWebBook()),
            observeReadingState = flowOf(ReadingState(bookId = "book-1", chapterRef = "chapter-5")),
            observeChapters = flowOf(sampleChapters()),
            observeLatestChapterRef = flowOf("chapter-12"),
            observeAutomaticCover = flowOf("https://example.com/cover.jpg"),
            observeManualCover = flowOf(null),
            updateReadingState = {},
        )

        val state = viewModel.uiState.first { it.book != null }

        assertTrue(state.showRefreshCatalogAction)
        assertTrue(state.showJumpToLatestAction)
    }

    @Test
    fun jumpToLatest_persistsLatestChapterLocatorBeforeOpeningReader() = runTest {
        var persistedState: ReadingState? = null
        val viewModel = BookDetailViewModel(
            bookId = "book-1",
            observeBook = flowOf(sampleWebBook()),
            observeReadingState = flowOf(
                ReadingState(
                    bookId = "book-1",
                    chapterRef = "chapter-5",
                    locator = buildReaderChapterLocator(BookFormat.WEB, "chapter-5"),
                    progressPercent = 0.42f,
                )
            ),
            observeChapters = flowOf(sampleChapters()),
            observeLatestChapterRef = flowOf("chapter-12"),
            observeAutomaticCover = flowOf(null),
            observeManualCover = flowOf(null),
            updateReadingState = { state -> persistedState = state },
        )
        viewModel.uiState.first { it.book != null }
        val eventDeferred = async(start = CoroutineStart.UNDISPATCHED) { viewModel.events.first() }

        viewModel.jumpToLatest()
        advanceUntilIdle()

        assertEquals("chapter-12", persistedState?.chapterRef)
        assertEquals(buildReaderChapterLocator(BookFormat.WEB, "chapter-12"), persistedState?.locator)
        assertEquals(0f, persistedState?.progressPercent)
        assertEquals(
            BookDetailEvent.OpenReader(bookId = "book-1"),
            eventDeferred.await(),
        )
    }

    @Test
    fun openChapter_resetsProgressAtSelectedChapterStart() = runTest {
        var persistedState: ReadingState? = null
        val viewModel = BookDetailViewModel(
            bookId = "book-1",
            observeBook = flowOf(sampleLocalBook()),
            observeReadingState = flowOf(
                ReadingState(
                    bookId = "book-1",
                    chapterRef = "chapter-3",
                    locator = "chapter:chapter-3#paragraph:4",
                    progressPercent = 0.57f,
                )
            ),
            observeChapters = flowOf(sampleChapters()),
            observeLatestChapterRef = flowOf("chapter-12"),
            observeAutomaticCover = flowOf(null),
            observeManualCover = flowOf(null),
            updateReadingState = { state -> persistedState = state },
        )

        viewModel.uiState.first { it.book != null }
        viewModel.openChapter("chapter-5")
        advanceUntilIdle()

        assertEquals("chapter-5", persistedState?.chapterRef)
        assertEquals("chapter:chapter-5#paragraph:0", persistedState?.locator)
        assertEquals(0f, persistedState?.progressPercent)
    }

    @Test
    fun jumpToLatest_withoutLatestRef_fallsBackToCurrentChapterWithoutCrash() = runTest {
        var persistedState: ReadingState? = null
        val viewModel = BookDetailViewModel(
            bookId = "book-1",
            observeBook = flowOf(sampleLocalBook()),
            observeReadingState = flowOf(
                ReadingState(
                    bookId = "book-1",
                    chapterRef = "chapter-5",
                    locator = "chapter:chapter-5#paragraph:0",
                )
            ),
            observeChapters = flowOf(sampleChapters()),
            observeLatestChapterRef = flowOf(null),
            observeAutomaticCover = flowOf(null),
            observeManualCover = flowOf(null),
            updateReadingState = { state -> persistedState = state },
        )

        viewModel.uiState.first { it.book != null }
        viewModel.jumpToLatest()
        advanceUntilIdle()

        assertEquals("chapter-5", persistedState?.chapterRef)
        assertEquals("chapter:chapter-5#paragraph:0", persistedState?.locator)
    }

    @Test
    fun detailUiState_exposesReadTocAndProgressSummary() = runTest {
        val viewModel = BookDetailViewModel(
            bookId = "book-1",
            observeBook = flowOf(sampleLocalBook()),
            observeReadingState = flowOf(
                ReadingState(
                    bookId = "book-1",
                    chapterRef = "chapter-3",
                    locator = "chapter:chapter-3#paragraph:0",
                    progressPercent = 0.42f,
                )
            ),
            observeChapters = flowOf(sampleChapters()),
            observeLatestChapterRef = flowOf("chapter-12"),
            observeAutomaticCover = flowOf("file:///cover.jpg"),
            observeManualCover = flowOf(null),
            updateReadingState = {},
        )

        val state = viewModel.uiState.first { it.book != null }

        assertEquals("继续阅读", state.readActionLabel)
        assertTrue(state.showTocAction)
        assertEquals("42%", state.progressLabel)
        assertEquals("第三章", state.currentChapterTitle)
        assertEquals("上次读到 第三章", state.lastReadLabel)
    }

    @Test
    fun enhanceMetadata_updatesSummaryAndSuggestedCover() = runTest {
        val facade = FakeBookMetadataEnhancementFacade(
            result = BookMetadataEnhancementResult(
                improvedSummary = "AI 优化后的简介",
                suggestedCoverUri = "https://example.com/ai-cover.jpg",
                authorIntroduction = "黑塞，德国作家。",
                tags = listOf("成长", "哲思"),
            ),
        )
        val viewModel = BookDetailViewModel(
            bookId = "book-1",
            observeBook = flowOf(sampleLocalBook()),
            observeReadingState = flowOf(ReadingState(bookId = "book-1")),
            observeChapters = flowOf(sampleChapters()),
            observeLatestChapterRef = flowOf("chapter-12"),
            observeAutomaticCover = flowOf("file:///cover.jpg"),
            observeManualCover = flowOf(null),
            metadataEnhancementFacade = facade,
            updateReadingState = {},
        )

        viewModel.uiState.first { it.book != null }
        viewModel.enhanceMetadata()
        advanceUntilIdle()

        assertEquals("AI 优化后的简介", viewModel.uiState.value.aiSummary)
        assertEquals("https://example.com/ai-cover.jpg", viewModel.uiState.value.suggestedCoverUri)
        assertEquals(listOf("成长", "哲思"), viewModel.uiState.value.aiTags)
        assertEquals(1, facade.callCount)
    }

    @Test
    fun applyEnhancedMetadata_persistsUpdatedSummaryAndCover() = runTest {
        var updatedBook: BookRecord? = null
        val viewModel = BookDetailViewModel(
            bookId = "book-1",
            observeBook = flowOf(sampleLocalBook()),
            observeReadingState = flowOf(ReadingState(bookId = "book-1")),
            observeChapters = flowOf(sampleChapters()),
            observeLatestChapterRef = flowOf("chapter-12"),
            observeAutomaticCover = flowOf("file:///cover.jpg"),
            observeManualCover = flowOf(null),
            metadataEnhancementFacade = FakeBookMetadataEnhancementFacade(
                result = BookMetadataEnhancementResult(
                    improvedSummary = "AI 优化后的简介",
                    suggestedCoverUri = "https://example.com/ai-cover.jpg",
                    authorIntroduction = "黑塞，德国作家。",
                    tags = listOf("成长"),
                ),
            ),
            applyMetadataAction = { book -> updatedBook = book },
            updateReadingState = {},
        )

        viewModel.uiState.first { it.book != null }
        viewModel.enhanceMetadata()
        advanceUntilIdle()
        viewModel.applyMetadataSuggestion()
        advanceUntilIdle()

        assertEquals("AI 优化后的简介", updatedBook?.summary)
        assertEquals("https://example.com/ai-cover.jpg", updatedBook?.cover)
    }

    private fun sampleLocalBook(): BookRecord =
        BookRecord(
            id = "book-1",
            title = "悉达多",
            author = "黑塞",
            originType = OriginType.LOCAL,
            primaryFormat = BookFormat.EPUB,
            summary = "一段关于寻找自我的旅程。",
        )

    private fun sampleWebBook(): BookRecord =
        BookRecord(
            id = "book-1",
            title = "雪中悍刀行",
            author = "烽火戏诸侯",
            originType = OriginType.WEB,
            primaryFormat = BookFormat.WEB,
            cover = "https://example.com/cover.jpg",
            summary = "江湖庙堂，风雪夜归人。",
        )

    private fun sampleChapters(): List<ReaderChapter> =
        listOf(
            ReaderChapter(
                chapterRef = "chapter-3",
                title = "第三章",
                orderIndex = 0,
                sourceType = BookFormat.EPUB,
                locatorHint = "chapter:chapter-3#paragraph:0",
            ),
            ReaderChapter(
                chapterRef = "chapter-5",
                title = "第五章",
                orderIndex = 1,
                sourceType = BookFormat.EPUB,
                locatorHint = "chapter:chapter-5#paragraph:0",
            ),
            ReaderChapter(
                chapterRef = "chapter-12",
                title = "第十二章",
                orderIndex = 2,
                sourceType = BookFormat.WEB,
                locatorHint = buildReaderChapterLocator(BookFormat.WEB, "chapter-12"),
                isLatest = true,
            ),
        )

    private companion object {
        val sampleImageBytes = byteArrayOf(1, 2, 3, 4)
    }

    private class FakeBookMetadataEnhancementFacade(
        private val result: BookMetadataEnhancementResult,
    ) : BookMetadataEnhancementFacade {
        var callCount: Int = 0

        override suspend fun enhance(book: BookRecord): BookMetadataEnhancementResult {
            callCount += 1
            return result
        }
    }
}
