package com.wenwentome.reader.feature.library

import android.net.Uri
import com.wenwentome.reader.core.model.BookFormat
import com.wenwentome.reader.core.model.BookRecord
import com.wenwentome.reader.core.model.OriginType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun defaultShelf_mergesLocalAndRemoteBooksIntoSingleList() = runTest {
        val viewModel = LibraryViewModel(
            observeBookshelf = fakeObserveBookshelfUseCase(
                flowOf(
                    listOf(
                        sampleItem(
                            book = BookRecord.newLocal("悉达多", "黑塞", BookFormat.EPUB),
                        ),
                        sampleItem(
                            book = BookRecord(
                                id = "web-1",
                                title = "雪中悍刀行",
                                author = "烽火戏诸侯",
                                originType = OriginType.WEB,
                                primaryFormat = BookFormat.WEB,
                            ),
                            hasUpdates = true,
                        ),
                    )
                )
            ),
            importLocalBook = { _ -> },
            refreshCatalogAction = {},
        )

        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals(2, state.visibleBooks.size)
    }

    @Test
    fun libraryUiState_exposesContinueReadingBookAndUpdateBadge() = runTest {
        val viewModel = LibraryViewModel(
            observeBookshelf = fakeObserveBookshelfUseCase(
                flowOf(
                    listOf(
                        sampleItem(
                            book = BookRecord.newLocal("悉达多", "黑塞", BookFormat.EPUB),
                        ),
                        sampleItem(
                            book = BookRecord(
                                id = "book-2",
                                title = "三体",
                                author = "刘慈欣",
                                originType = OriginType.LOCAL,
                                primaryFormat = BookFormat.EPUB,
                            ),
                            progressPercent = 0.42f,
                            progressLabel = "42%",
                        ),
                        sampleItem(
                            book = BookRecord(
                                id = "web-1",
                                title = "雪中悍刀行",
                                author = "烽火戏诸侯",
                                originType = OriginType.WEB,
                                primaryFormat = BookFormat.WEB,
                            ),
                            hasUpdates = true,
                            canRestoreAutomaticCover = true,
                        ),
                    )
                )
            ),
            importLocalBook = { _ -> },
            refreshCatalogAction = {},
        )

        advanceUntilIdle()
        val state = viewModel.uiState.value

        assertEquals("book-2", state.continueReading?.book?.id)
        assertTrue(state.visibleBooks.first { it.book.id == "web-1" }.hasUpdates)
    }

    @Test
    fun continueReading_prefersMostRecentlyReadItemOverListOrder() = runTest {
        val viewModel = LibraryViewModel(
            observeBookshelf = fakeObserveBookshelfUseCase(
                flowOf(
                    listOf(
                        sampleItem(
                            book = BookRecord.newLocal("旧进度", "作者甲", BookFormat.EPUB),
                            progressPercent = 0.65f,
                            progressLabel = "65%",
                            lastReadAt = 1_000L,
                        ),
                        sampleItem(
                            book = BookRecord(
                                id = "book-newer",
                                title = "新进度",
                                author = "作者乙",
                                originType = OriginType.LOCAL,
                                primaryFormat = BookFormat.EPUB,
                            ),
                            progressPercent = 0.2f,
                            progressLabel = "20%",
                            lastReadAt = 5_000L,
                        ),
                    )
                )
            ),
            importLocalBook = { _ -> },
            refreshCatalogAction = {},
        )

        advanceUntilIdle()

        assertEquals("book-newer", viewModel.uiState.value.continueReading?.book?.id)
    }

    @Test
    fun refreshCatalog_updatesHasUpdatesBadgeAfterLatestChapterChanges() = runTest {
        val bookshelf = MutableStateFlow(
            listOf(
                sampleItem(
                    book = BookRecord(
                        id = "web-1",
                        title = "雪中悍刀行",
                        author = "烽火戏诸侯",
                        originType = OriginType.WEB,
                        primaryFormat = BookFormat.WEB,
                    ),
                    hasUpdates = true,
                )
            )
        )
        var refreshCount = 0
        val viewModel = LibraryViewModel(
            observeBookshelf = fakeObserveBookshelfUseCase(bookshelf),
            importLocalBook = { _ -> },
            refreshCatalogAction = { bookId ->
                refreshCount++
                assertEquals("web-1", bookId)
                bookshelf.value = bookshelf.value.map { it.copy(hasUpdates = false) }
            },
        )

        viewModel.refreshCatalog("web-1")
        advanceUntilIdle()

        assertEquals(1, refreshCount)
        assertFalse(viewModel.uiState.value.visibleBooks.first { it.book.id == "web-1" }.hasUpdates)
    }

    @Test
    fun import_forwardsAllSelectedUrisToBatchImporter() = runTest {
        val importedBatches = mutableListOf<List<Uri>>()
        val viewModel = LibraryViewModel(
            observeBookshelf = fakeObserveBookshelfUseCase(flowOf(emptyList())),
            importLocalBook = { uris -> importedBatches += uris },
            refreshCatalogAction = {},
        )
        val selected = listOf(
            Uri.parse("content://books/one.epub"),
            Uri.parse("content://books/two.txt"),
        )

        viewModel.import(selected)
        advanceUntilIdle()

        assertEquals(listOf(selected), importedBatches)
    }

    @Test
    fun setFilterAndSort_updatesVisibleBooksInExpectedOrder() = runTest {
        val bookshelf = MutableStateFlow(
            listOf(
                sampleItem(
                    book = BookRecord.newLocal("悉达多", "黑塞", BookFormat.EPUB),
                    lastReadAt = 1_000L,
                ),
                sampleItem(
                    book = BookRecord(
                        id = "web-2",
                        title = "诡秘之主",
                        author = "爱潜水的乌贼",
                        originType = OriginType.WEB,
                        primaryFormat = BookFormat.WEB,
                    ),
                    lastReadAt = 2_000L,
                ),
                sampleItem(
                    book = BookRecord(
                        id = "web-1",
                        title = "雪中悍刀行",
                        author = "烽火戏诸侯",
                        originType = OriginType.WEB,
                        primaryFormat = BookFormat.WEB,
                    ),
                    lastReadAt = 4_000L,
                ),
            )
        )
        val viewModel = LibraryViewModel(
            observeBookshelf = fakeObserveBookshelfUseCase(bookshelf),
            importLocalBook = { _ -> },
            refreshCatalogAction = {},
        )

        viewModel.setFilter(LibraryFilter.WEB_ONLY)
        viewModel.setSort(LibrarySort.TITLE_ASC)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(LibraryFilter.WEB_ONLY, state.filter)
        assertEquals(LibrarySort.TITLE_ASC, state.sort)
        assertEquals(listOf("web-2", "web-1"), state.visibleBooks.map { it.book.id })
    }

    private fun fakeObserveBookshelfUseCase(flow: Flow<List<LibraryBookItem>>): ObserveBookshelfUseCase =
        ObserveBookshelfUseCase { flow }

    private fun sampleItem(
        book: BookRecord,
        progressPercent: Float = 0f,
        progressLabel: String = "0%",
        hasUpdates: Boolean = false,
        canRestoreAutomaticCover: Boolean = false,
        lastReadAt: Long = 0L,
    ) = LibraryBookItem(
        book = book,
        effectiveCover = book.cover,
        progressPercent = progressPercent,
        progressLabel = progressLabel,
        hasUpdates = hasUpdates,
        canRestoreAutomaticCover = canRestoreAutomaticCover,
        lastReadAt = lastReadAt,
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
