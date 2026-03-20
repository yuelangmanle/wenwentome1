package com.wenwentome.reader.feature.reader

import com.wenwentome.reader.core.model.BookFormat
import com.wenwentome.reader.core.model.BookRecord
import com.wenwentome.reader.core.model.ReaderChapter
import com.wenwentome.reader.core.model.ReaderMode
import com.wenwentome.reader.core.model.ReaderPresentationPrefs
import com.wenwentome.reader.core.model.ReaderTheme
import com.wenwentome.reader.core.model.ReadingBookmark
import com.wenwentome.reader.core.model.ReadingState
import com.wenwentome.reader.data.localbooks.ReaderContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class ReaderViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun openingBook_restoresSavedLocatorAndBookmarks() = runTest {
        val viewModel = ReaderViewModel(
            bookId = "book-1",
            observeReadingState = flowOf(
                ReadingState(
                    bookId = "book-1",
                    locator = "chapter-3",
                    chapterRef = "chapter-3",
                    progressPercent = 0.31f,
                    bookmarks = listOf(
                        ReadingBookmark(
                            chapterRef = "chapter-3",
                            locator = "chapter-3",
                            label = "重读这里",
                        )
                    ),
                )
            ),
            observeBook = flowOf(BookRecord.newLocal("悉达多", "黑塞", BookFormat.EPUB)),
            observeContent = flowOf(
                ReaderContent(
                    chapterTitle = "第三章",
                    paragraphs = listOf("正文第一段"),
                    chapterRef = "OPS/chapter-3.xhtml",
                )
            ),
            updateReadingState = {},
        )

        val state = viewModel.uiState.first { it.book != null }
        assertEquals("chapter-3", state.locator)
        assertEquals("OPS/chapter-3.xhtml", state.chapterRef)
        assertEquals(0.31f, state.progressPercent)
        assertEquals(1, state.bookmarks.size)
        assertEquals("第三章", state.chapterTitle)
    }

    @Test
    fun updateLocator_persistsCurrentLocatorAndBookmarks() = runTest {
        var persistedState: ReadingState? = null
        val viewModel = ReaderViewModel(
            bookId = "book-1",
            observeReadingState = flowOf(
                ReadingState(
                    bookId = "book-1",
                    locator = "chapter-1",
                    progressPercent = 0.12f,
                    bookmarks = listOf(
                        ReadingBookmark(
                            chapterRef = "chapter-1",
                            locator = "chapter-1",
                            label = "起点",
                        )
                    ),
                )
            ),
            observeBook = flowOf(BookRecord.newLocal("悉达多", "黑塞", BookFormat.EPUB).copy(id = "book-1")),
            observeContent = flowOf(
                ReaderContent(
                    chapterTitle = "第一章",
                    paragraphs = listOf("正文第一段"),
                )
            ),
            updateReadingState = { state -> persistedState = state },
        )

        viewModel.uiState.first { it.book != null }
        viewModel.updateLocator(locator = "chapter-4", chapterRef = "chapter-4", progressPercent = 0.42f)
        advanceUntilIdle()

        assertNotNull(persistedState)
        assertEquals("book-1", persistedState?.bookId)
        assertEquals("chapter-4", persistedState?.locator)
        assertEquals("chapter-4", persistedState?.chapterRef)
        assertEquals(0.42f, persistedState?.progressPercent)
        assertEquals(1, persistedState?.bookmarks?.size)
        assertEquals("起点", persistedState?.bookmarks?.first()?.label)
    }

    @Test
    fun updateReaderMode_persistsSelectedModeWithoutResettingChapter() = runTest {
        val readerMode = MutableStateFlow(ReaderMode.SIMULATED_PAGE_TURN)
        var persistedMode: ReaderMode? = null
        val viewModel = ReaderViewModel(
            bookId = "book-1",
            observeReadingState = flowOf(
                ReadingState(
                    bookId = "book-1",
                    locator = "chapter:chapter-3#paragraph:0",
                    chapterRef = "chapter-3",
                )
            ),
            observeBook = flowOf(readerBook()),
            observeContent = flowOf(
                ReaderContent(
                    chapterTitle = "第三章",
                    paragraphs = listOf("正文第一段"),
                    chapterRef = "chapter-3",
                )
            ),
            observeReaderMode = readerMode,
            observePresentationPrefs = MutableStateFlow(ReaderPresentationPrefs()),
            observeChapters = flowOf(readerChapters()),
            observeLatestChapterRef = flowOf("chapter-8"),
            saveReaderMode = { mode ->
                persistedMode = mode
                readerMode.value = mode
            },
            savePresentationPrefs = {},
            updateReadingState = {},
        )

        viewModel.uiState.first { it.book != null }
        viewModel.setReaderMode(ReaderMode.HORIZONTAL_PAGING)
        advanceUntilIdle()

        assertEquals("chapter-3", viewModel.uiState.value.chapterRef)
        assertEquals(ReaderMode.HORIZONTAL_PAGING, viewModel.uiState.value.readerMode)
        assertEquals(ReaderMode.HORIZONTAL_PAGING, persistedMode)
    }

    @Test
    fun updatePresentationPrefs_persistsThemeFontAndLineHeight() = runTest {
        val presentation = MutableStateFlow(ReaderPresentationPrefs())
        var persistedPresentation: ReaderPresentationPrefs? = null
        val viewModel = ReaderViewModel(
            bookId = "book-1",
            observeReadingState = flowOf(ReadingState(bookId = "book-1")),
            observeBook = flowOf(readerBook()),
            observeContent = flowOf(
                ReaderContent(
                    chapterTitle = "第三章",
                    paragraphs = listOf("正文第一段"),
                    chapterRef = "chapter-3",
                )
            ),
            observeReaderMode = MutableStateFlow(ReaderMode.SIMULATED_PAGE_TURN),
            observePresentationPrefs = presentation,
            observeChapters = flowOf(readerChapters()),
            observeLatestChapterRef = flowOf("chapter-8"),
            saveReaderMode = {},
            savePresentationPrefs = { prefs ->
                persistedPresentation = prefs
                presentation.value = prefs
            },
            updateReadingState = {},
        )

        viewModel.uiState.first { it.book != null }
        viewModel.updatePresentation(
            ReaderPresentationPrefs(
                theme = ReaderTheme.NIGHT,
                fontSizeSp = 22,
                lineHeightMultiplier = 1.8f,
                brightnessPercent = 60,
            )
        )
        advanceUntilIdle()

        assertEquals(ReaderTheme.NIGHT, viewModel.uiState.value.presentation.theme)
        assertEquals(22, viewModel.uiState.value.presentation.fontSizeSp)
        assertEquals(1.8f, viewModel.uiState.value.presentation.lineHeightMultiplier)
        assertEquals(60, persistedPresentation?.brightnessPercent)
    }

    @Test
    fun jumpToChapter_updatesLocatorUsingChapterLocatorHint() = runTest {
        var persistedState: ReadingState? = null
        val viewModel = ReaderViewModel(
            bookId = "book-1",
            observeReadingState = flowOf(
                ReadingState(
                    bookId = "book-1",
                    locator = "chapter:chapter-3#paragraph:0",
                    chapterRef = "chapter-3",
                    progressPercent = 0.42f,
                )
            ),
            observeBook = flowOf(readerBook()),
            observeContent = flowOf(
                ReaderContent(
                    chapterTitle = "第三章",
                    paragraphs = listOf("正文第一段"),
                    chapterRef = "chapter-3",
                )
            ),
            observeReaderMode = MutableStateFlow(ReaderMode.SIMULATED_PAGE_TURN),
            observePresentationPrefs = MutableStateFlow(ReaderPresentationPrefs()),
            observeChapters = flowOf(readerChapters()),
            observeLatestChapterRef = flowOf("chapter-8"),
            saveReaderMode = {},
            savePresentationPrefs = {},
            updateReadingState = { state -> persistedState = state },
        )

        viewModel.uiState.first { it.book != null }
        viewModel.jumpToChapter(chapterRef = "chapter-8")
        advanceUntilIdle()

        assertEquals("chapter:chapter-8#paragraph:0", persistedState?.locator)
        assertEquals("chapter-8", persistedState?.chapterRef)
    }

    private fun readerBook(): BookRecord =
        BookRecord.newLocal("悉达多", "黑塞", BookFormat.EPUB).copy(id = "book-1")

    private fun readerChapters(): List<ReaderChapter> =
        listOf(
            ReaderChapter(
                chapterRef = "chapter-3",
                title = "第三章",
                orderIndex = 0,
                sourceType = BookFormat.EPUB,
                locatorHint = "chapter:chapter-3#paragraph:0",
            ),
            ReaderChapter(
                chapterRef = "chapter-8",
                title = "第八章",
                orderIndex = 1,
                sourceType = BookFormat.EPUB,
                locatorHint = "chapter:chapter-8#paragraph:0",
                isLatest = true,
            ),
        )
}

class MainDispatcherRule(
    private val dispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
