package com.wenwentome.reader.feature.discover

import com.wenwentome.reader.bridge.source.SourceBridgeRepository
import com.wenwentome.reader.bridge.source.model.RemoteBookDetail
import com.wenwentome.reader.bridge.source.model.RemoteChapter
import com.wenwentome.reader.bridge.source.model.RemoteChapterContent
import com.wenwentome.reader.bridge.source.model.RemoteSearchResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class DiscoverViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun addingSearchResult_createsBookRecordAndRemoteBinding() = runTest {
        val addRemoteBookUseCase = FakeAddRemoteBookToShelfUseCase()
        val viewModel = DiscoverViewModel(
            sourceBridgeRepository = FakeBridgeRepository(searchResults = listOf(sampleSearchResult())),
            addRemoteBookToShelf = addRemoteBookUseCase,
        )

        viewModel.search("雪中悍刀行")
        advanceUntilIdle()
        viewModel.addToShelf(sampleSearchResult().id)

        val state = viewModel.uiState.first { it.lastAddedTitle != null }
        assertEquals("雪中悍刀行", state.lastAddedTitle)
        assertEquals(1, addRemoteBookUseCase.invocations.size)
    }

    @Test
    fun newerSearchResult_isNotOverwrittenByOlderRequest() = runTest {
        val olderQueryResult = CompletableDeferred<List<RemoteSearchResult>>()
        val newerQueryResult = CompletableDeferred<List<RemoteSearchResult>>()
        val viewModel = DiscoverViewModel(
            sourceBridgeRepository = FakeBridgeRepository(
                deferredSearchResults = mapOf(
                    "雪" to olderQueryResult,
                    "雪中" to newerQueryResult,
                ),
            ),
            addRemoteBookToShelf = FakeAddRemoteBookToShelfUseCase(),
        )

        viewModel.search("雪")
        viewModel.search("雪中")

        newerQueryResult.complete(
            listOf(
                sampleSearchResult(
                    id = "remote-2",
                    title = "雪中悍刀行",
                ),
            ),
        )
        advanceUntilIdle()

        olderQueryResult.complete(
            listOf(
                sampleSearchResult(
                    id = "remote-3",
                    title = "雪",
                ),
            ),
        )
        advanceUntilIdle()

        assertEquals("雪中", viewModel.uiState.value.query)
        assertEquals(listOf("雪中悍刀行"), viewModel.uiState.value.results.map { it.title })
    }

    @Test
    fun discoverViewModel_selectResultLoadsPreviewState() = runTest {
        val viewModel = DiscoverViewModel(
            sourceBridgeRepository = FakeBridgeRepository(
                searchResults = listOf(sampleSearchResult()),
                detailByRemoteBookId = mapOf(
                    "remote-1" to RemoteBookDetail(
                        title = "雪中悍刀行",
                        author = "烽火戏诸侯",
                        summary = "北凉刀，江湖雪。",
                        lastChapter = "最新章",
                    )
                ),
            ),
            addRemoteBookToShelf = FakeAddRemoteBookToShelfUseCase(),
        )

        viewModel.search("雪中")
        advanceUntilIdle()
        viewModel.selectResult("remote-1")
        advanceUntilIdle()

        assertEquals("remote-1", viewModel.uiState.value.selectedResultId)
        assertEquals("雪中悍刀行", viewModel.uiState.value.selectedPreview?.title)
        assertEquals("最新章", viewModel.uiState.value.selectedPreview?.lastChapter)
    }

    @Test
    fun discoverViewModel_readLatest_refreshesSelectedBookAndOpensReader() = runTest {
        val addRemoteBookUseCase = FakeAddRemoteBookToShelfUseCase()
        var refreshedBookId: String? = null
        var persistedChapterRef: String? = null
        val viewModel = DiscoverViewModel(
            sourceBridgeRepository = FakeBridgeRepository(
                searchResults = listOf(sampleSearchResult()),
                detailByRemoteBookId = mapOf(
                    "remote-1" to RemoteBookDetail(
                        title = "雪中悍刀行",
                        author = "烽火戏诸侯",
                        summary = "北凉刀，江湖雪。",
                        lastChapter = "第十章",
                    )
                ),
            ),
            addRemoteBookToShelf = addRemoteBookUseCase,
            resolveShelfBookId = { "book-remote-1" },
            refreshRemoteBook = { bookId ->
                refreshedBookId = bookId
                RefreshRemoteBookResult(
                    latestKnownChapterRef = "chapter-10",
                    hasUpdates = true,
                )
            },
            updateReadingState = { state ->
                persistedChapterRef = state.chapterRef
            },
        )

        viewModel.search("雪中")
        advanceUntilIdle()
        viewModel.selectResult("remote-1")
        advanceUntilIdle()
        val eventDeferred = async(start = CoroutineStart.UNDISPATCHED) { viewModel.events.first() }

        viewModel.readLatest()
        advanceUntilIdle()

        assertEquals("book-remote-1", refreshedBookId)
        assertEquals("chapter-10", persistedChapterRef)
        assertEquals(
            DiscoverEvent.OpenReader(bookId = "book-remote-1"),
            eventDeferred.await(),
        )
        assertEquals(emptySet<String>(), viewModel.uiState.value.refreshingResultIds)
        assertEquals("第十章", viewModel.uiState.value.selectedPreview?.lastChapter)
    }
}

private class FakeAddRemoteBookToShelfUseCase : AddRemoteBookToShelf {
    val invocations = mutableListOf<RemoteSearchResult>()

    override suspend fun invoke(result: RemoteSearchResult) {
        invocations += result
    }
}

private class FakeBridgeRepository(
    private val searchResults: List<RemoteSearchResult> = emptyList(),
    private val deferredSearchResults: Map<String, CompletableDeferred<List<RemoteSearchResult>>> = emptyMap(),
    private val detailByRemoteBookId: Map<String, RemoteBookDetail> = emptyMap(),
) : SourceBridgeRepository {
    override suspend fun search(query: String, sourceIds: List<String>): List<RemoteSearchResult> =
        deferredSearchResults[query]?.await() ?: searchResults

    override suspend fun fetchBookDetail(sourceId: String, remoteBookId: String): RemoteBookDetail =
        detailByRemoteBookId[remoteBookId] ?: RemoteBookDetail(title = remoteBookId)

    override suspend fun fetchToc(sourceId: String, remoteBookId: String): List<RemoteChapter> = emptyList()

    override suspend fun fetchChapterContent(sourceId: String, chapterRef: String): RemoteChapterContent =
        RemoteChapterContent(chapterRef = chapterRef, title = chapterRef, content = "")
}

private fun sampleSearchResult(
    id: String = "remote-1",
    title: String = "雪中悍刀行",
): RemoteSearchResult =
    RemoteSearchResult(
        id = id,
        sourceId = "source-1",
        title = title,
        author = "烽火戏诸侯",
        detailUrl = "https://example.com/book/1",
    )

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
