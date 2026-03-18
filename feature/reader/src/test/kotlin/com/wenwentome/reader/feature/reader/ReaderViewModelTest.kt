package com.wenwentome.reader.feature.reader

import com.wenwentome.reader.core.model.BookFormat
import com.wenwentome.reader.core.model.BookRecord
import com.wenwentome.reader.core.model.ReadingBookmark
import com.wenwentome.reader.core.model.ReadingState
import com.wenwentome.reader.data.localbooks.ReaderContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
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
                )
            ),
            updateReadingState = {},
        )

        val state = viewModel.uiState.first { it.book != null }
        assertEquals("chapter-3", state.locator)
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

        assertNotNull(persistedState)
        assertEquals("book-1", persistedState?.bookId)
        assertEquals("chapter-4", persistedState?.locator)
        assertEquals("chapter-4", persistedState?.chapterRef)
        assertEquals(0.42f, persistedState?.progressPercent)
        assertEquals(1, persistedState?.bookmarks?.size)
        assertEquals("起点", persistedState?.bookmarks?.first()?.label)
    }
}

private class MainDispatcherRule(
    private val dispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
