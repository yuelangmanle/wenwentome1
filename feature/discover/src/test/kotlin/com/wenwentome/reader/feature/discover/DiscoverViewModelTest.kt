package com.wenwentome.reader.feature.discover

import com.wenwentome.reader.bridge.source.SourceBridgeErrorCode
import com.wenwentome.reader.bridge.source.SourceBridgeException
import com.wenwentome.reader.bridge.source.SourceBridgeRepository
import com.wenwentome.reader.bridge.source.model.RemoteBookDetail
import com.wenwentome.reader.bridge.source.model.RemoteChapter
import com.wenwentome.reader.bridge.source.model.RemoteChapterContent
import com.wenwentome.reader.bridge.source.model.RemoteSearchResult
import com.wenwentome.reader.core.model.ReadingState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import kotlin.coroutines.ContinuationInterceptor

@OptIn(ExperimentalCoroutinesApi::class)
class DiscoverViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun addingSearchResult_createsBookRecordAndRemoteBinding() = runTest {
        var resolvedBookId: String? = null
        val addRemoteBookUseCase = FakeAddRemoteBookToShelfUseCase { result ->
            val bookId = "book-${result.id}"
            resolvedBookId = bookId
            bookId
        }
        val viewModel = DiscoverViewModel(
            sourceBridgeRepository = FakeBridgeRepository(searchResults = listOf(sampleSearchResult())),
            addRemoteBookToShelf = addRemoteBookUseCase,
            resolveShelfBookId = { resolvedBookId },
            ioDispatcher = Dispatchers.Main,
        )

        viewModel.search("雪中悍刀行")
        advanceUntilIdle()
        viewModel.addToShelf(viewModel.uiState.value.results.single().result)
        advanceUntilIdle()

        assertEquals("雪中悍刀行", viewModel.uiState.value.lastAddedTitle)
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
            ioDispatcher = Dispatchers.Main,
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
            ioDispatcher = Dispatchers.Main,
        )

        viewModel.search("雪中")
        advanceUntilIdle()
        viewModel.selectResult("remote-1")
        advanceUntilIdle()

        assertEquals("remote-1", viewModel.uiState.value.selectedResultId)
        assertEquals(sampleSearchResult(), viewModel.uiState.value.selectedResult?.result)
        assertEquals("雪中悍刀行", viewModel.uiState.value.selectedPreview?.title)
        assertEquals("最新章", viewModel.uiState.value.selectedPreview?.lastChapter)
    }

    @Test
    fun selectResult_showsUnsupportedHintWhenBridgeRejectsRule() = runTest {
        val viewModel = DiscoverViewModel(
            sourceBridgeRepository = FakeBridgeRepository(
                searchResults = listOf(sampleSearchResult()),
                fetchBookDetailError = SourceBridgeException(
                    code = SourceBridgeErrorCode.UNSUPPORTED_RULE_KIND,
                    message = "JS_TEMPLATE",
                ),
            ),
            addRemoteBookToShelf = FakeAddRemoteBookToShelfUseCase(),
            ioDispatcher = Dispatchers.Main,
        )

        viewModel.search("雪中")
        advanceUntilIdle()
        viewModel.selectResult("remote-1")
        advanceUntilIdle()

        assertEquals("当前书源规则超出 1.4 第一阶段支持范围", viewModel.uiState.value.lastRefreshHint)
    }

    @Test
    fun selectResult_keepsGenericHintWhenBridgeFailsNormally() = runTest {
        val viewModel = DiscoverViewModel(
            sourceBridgeRepository = FakeBridgeRepository(
                searchResults = listOf(sampleSearchResult()),
                fetchBookDetailError = SourceBridgeException(
                    code = SourceBridgeErrorCode.REQUEST_FAILED,
                    message = "network down",
                ),
            ),
            addRemoteBookToShelf = FakeAddRemoteBookToShelfUseCase(),
            ioDispatcher = Dispatchers.Main,
        )

        viewModel.search("雪中")
        advanceUntilIdle()
        viewModel.selectResult("remote-1")
        advanceUntilIdle()

        assertEquals("当前书源预览失败，请稍后重试", viewModel.uiState.value.lastRefreshHint)
    }

    @Test
    fun search_usesConfiguredIoDispatcherForBridgeLookup() = runTest {
        val ioDispatcher = StandardTestDispatcher(testScheduler)
        var observedDispatcher: CoroutineDispatcher? = null
        val viewModel = DiscoverViewModel(
            sourceBridgeRepository = FakeBridgeRepository(
                searchResults = listOf(sampleSearchResult()),
                onSearch = {
                    observedDispatcher = currentCoroutineContext()[ContinuationInterceptor] as? CoroutineDispatcher
                },
            ),
            addRemoteBookToShelf = FakeAddRemoteBookToShelfUseCase(),
            ioDispatcher = ioDispatcher,
        )

        viewModel.search("雪中")
        advanceUntilIdle()

        assertEquals(ioDispatcher, observedDispatcher)
    }

    @Test
    fun selectResult_usesConfiguredIoDispatcherForPreviewLookup() = runTest {
        val ioDispatcher = StandardTestDispatcher(testScheduler)
        var observedDispatcher: CoroutineDispatcher? = null
        val viewModel = DiscoverViewModel(
            sourceBridgeRepository = FakeBridgeRepository(
                searchResults = listOf(sampleSearchResult()),
                detailByRemoteBookId = mapOf("remote-1" to RemoteBookDetail(title = "雪中悍刀行")),
                onFetchBookDetail = {
                    observedDispatcher = currentCoroutineContext()[ContinuationInterceptor] as? CoroutineDispatcher
                },
            ),
            addRemoteBookToShelf = FakeAddRemoteBookToShelfUseCase(),
            ioDispatcher = ioDispatcher,
        )

        viewModel.search("雪中")
        advanceUntilIdle()
        viewModel.selectResult("remote-1")
        advanceUntilIdle()

        assertEquals(ioDispatcher, observedDispatcher)
    }

    @Test
    fun refreshSelected_usesSwitchedSourcePreviewAndHint() = runTest {
        val viewModel = DiscoverViewModel(
            sourceBridgeRepository = FakeBridgeRepository(
                searchResults = listOf(sampleSearchResult()),
                detailByRemoteBookId = mapOf(
                    "remote-1" to RemoteBookDetail(
                        title = "雪中悍刀行",
                        author = "烽火戏诸侯",
                        summary = "主源预览。",
                        lastChapter = "第十章",
                    ),
                    "backup-1" to RemoteBookDetail(
                        title = "雪中悍刀行",
                        author = "烽火戏诸侯",
                        summary = "已切换备用源。",
                        lastChapter = "第十二章",
                    ),
                ),
            ),
            addRemoteBookToShelf = FakeAddRemoteBookToShelfUseCase(),
            resolveShelfBookId = { "book-remote-1" },
            refreshRemoteBook = {
                RefreshRemoteBookResult(
                    latestKnownChapterRef = "chapter-12",
                    hasUpdates = true,
                    activeSourceId = "backup-source",
                    activeRemoteBookId = "backup-1",
                    activeRemoteBookUrl = "https://example.com/book/backup-1",
                    autoSwitched = true,
                    primarySourceId = "source-1",
                    primarySourceFailed = true,
                )
            },
            ioDispatcher = Dispatchers.Main,
        )

        viewModel.search("雪中")
        advanceUntilIdle()
        viewModel.selectResult("remote-1")
        advanceUntilIdle()

        viewModel.refreshSelected()
        advanceUntilIdle()

        assertEquals("第十二章", viewModel.uiState.value.selectedPreview?.lastChapter)
        assertEquals("已自动切换到书源：backup-source", viewModel.uiState.value.lastRefreshHint)
        assertEquals(true, viewModel.uiState.value.selectedResult?.sourceId == "backup-source")
    }

    @Test
    fun discoverViewModel_readLatest_refreshesSelectedBookAndOpensReader() = runTest {
        val addRemoteBookUseCase = FakeAddRemoteBookToShelfUseCase()
        var refreshedBookId: String? = null
        var persistedChapterRef: String? = null
        var persistedProgressPercent: Float? = null
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
                    activeSourceId = "source-1",
                    activeRemoteBookId = "remote-1",
                    activeRemoteBookUrl = "https://example.com/book/1",
                    autoSwitched = false,
                    primarySourceId = "source-1",
                    primarySourceFailed = false,
                )
            },
            updateReadingState = { state ->
                persistedChapterRef = state.chapterRef
                persistedProgressPercent = state.progressPercent
            },
            ioDispatcher = Dispatchers.Main,
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
        assertEquals(0f, persistedProgressPercent)
        assertEquals(
            DiscoverEvent.OpenReader(bookId = "book-remote-1"),
            eventDeferred.await(),
        )
        assertEquals(emptySet<String>(), viewModel.uiState.value.refreshingResultIds)
        assertEquals("第十章", viewModel.uiState.value.selectedPreview?.lastChapter)
    }

    @Test
    fun discoverViewModel_readLatest_preservesExistingProgressUntilReaderWritesNewProgress() = runTest {
        var persistedProgressPercent: Float? = null
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
            addRemoteBookToShelf = FakeAddRemoteBookToShelfUseCase(),
            resolveShelfBookId = { "book-remote-1" },
            refreshRemoteBook = {
                RefreshRemoteBookResult(
                    latestKnownChapterRef = "chapter-10",
                    hasUpdates = true,
                    activeSourceId = "source-1",
                    activeRemoteBookId = "remote-1",
                    activeRemoteBookUrl = "https://example.com/book/1",
                    autoSwitched = false,
                    primarySourceId = "source-1",
                    primarySourceFailed = false,
                )
            },
            loadReadingState = {
                ReadingState(
                    bookId = "book-remote-1",
                    locator = "chapter-6",
                    chapterRef = "chapter-6",
                    progressPercent = 0.42f,
                )
            },
            updateReadingState = { state ->
                persistedProgressPercent = state.progressPercent
            },
            ioDispatcher = Dispatchers.Main,
        )

        viewModel.search("雪中")
        advanceUntilIdle()
        viewModel.selectResult("remote-1")
        advanceUntilIdle()

        viewModel.readLatest()
        advanceUntilIdle()

        assertEquals(0.42f, persistedProgressPercent)
    }

    @Test
    fun searchRanksResultsByHealthScoreAndApiBoost() = runTest {
        val tracker = SourceHealthTracker(nowProvider = { 1_000L })
        repeat(10) {
            tracker.recordResult(sourceId = "source-fast", success = true, latencyMs = 300)
        }
        val viewModel = DiscoverViewModel(
            sourceBridgeRepository = FakeBridgeRepository(
                searchResults = listOf(
                    sampleSearchResult(id = "slow-1", sourceId = "source-slow"),
                    sampleSearchResult(id = "fast-1", sourceId = "source-fast"),
                ),
            ),
            addRemoteBookToShelf = FakeAddRemoteBookToShelfUseCase(),
            healthTracker = tracker,
            ioDispatcher = Dispatchers.Main,
        )

        viewModel.search("雪中悍刀行")
        advanceUntilIdle()

        assertEquals("source-fast", viewModel.uiState.value.results.first().sourceId)
        assertTrue(viewModel.uiState.value.results.first().healthScore > 0.8f)
    }
}

private class FakeAddRemoteBookToShelfUseCase(
    private val onInvoke: ((RemoteSearchResult) -> String)? = null,
) : AddRemoteBookToShelf {
    val invocations = mutableListOf<RemoteSearchResult>()

    override suspend fun invoke(result: RemoteSearchResult): String {
        invocations += result
        return onInvoke?.invoke(result) ?: "book-${result.id}"
    }
}

private class FakeBridgeRepository(
    private val searchResults: List<RemoteSearchResult> = emptyList(),
    private val deferredSearchResults: Map<String, CompletableDeferred<List<RemoteSearchResult>>> = emptyMap(),
    private val detailByRemoteBookId: Map<String, RemoteBookDetail> = emptyMap(),
    private val fetchBookDetailError: Throwable? = null,
    private val onSearch: suspend () -> Unit = {},
    private val onFetchBookDetail: suspend () -> Unit = {},
) : SourceBridgeRepository {
    override suspend fun search(query: String, sourceIds: List<String>): List<RemoteSearchResult> {
        onSearch()
        return deferredSearchResults[query]?.await() ?: searchResults
    }

    override suspend fun fetchBookDetail(sourceId: String, remoteBookId: String): RemoteBookDetail {
        onFetchBookDetail()
        fetchBookDetailError?.let { throw it }
        return detailByRemoteBookId[remoteBookId] ?: RemoteBookDetail(title = remoteBookId)
    }

    override suspend fun fetchToc(sourceId: String, remoteBookId: String): List<RemoteChapter> = emptyList()

    override suspend fun fetchChapterContent(sourceId: String, chapterRef: String): RemoteChapterContent =
        RemoteChapterContent(chapterRef = chapterRef, title = chapterRef, content = "")
}

private fun sampleSearchResult(
    id: String = "remote-1",
    title: String = "雪中悍刀行",
    sourceId: String = "source-1",
    detailUrl: String = "https://example.com/book/1",
): RemoteSearchResult =
    RemoteSearchResult(
        id = id,
        sourceId = sourceId,
        title = title,
        author = "烽火戏诸侯",
        detailUrl = detailUrl,
    )

@OptIn(ExperimentalCoroutinesApi::class)
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
