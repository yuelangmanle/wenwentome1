package com.wenwentome.reader.feature.reader

import com.wenwentome.reader.core.model.BookFormat
import com.wenwentome.reader.core.model.BookRecord
import com.wenwentome.reader.core.model.OriginType
import com.wenwentome.reader.core.model.ReaderChapter
import com.wenwentome.reader.core.model.ReadingState
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
                    locator = "chapter-5",
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
        val eventDeferred = async { viewModel.events.first() }

        viewModel.jumpToLatest()
        advanceUntilIdle()

        assertEquals("chapter-12", persistedState?.chapterRef)
        assertEquals("chapter-12", persistedState?.locator)
        assertEquals(
            BookDetailEvent.OpenReader(bookId = "book-1"),
            eventDeferred.await(),
        )
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
                locatorHint = "chapter-12",
                isLatest = true,
            ),
        )

    private companion object {
        val sampleImageBytes = byteArrayOf(1, 2, 3, 4)
    }
}
