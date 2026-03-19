package com.wenwentome.reader.feature.discover

import com.wenwentome.reader.bridge.source.SourceBridgeRepository
import com.wenwentome.reader.bridge.source.model.RemoteBookDetail
import com.wenwentome.reader.bridge.source.model.RemoteChapter
import com.wenwentome.reader.bridge.source.model.RemoteChapterContent
import com.wenwentome.reader.bridge.source.model.RemoteSearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
        viewModel.addToShelf(sampleSearchResult().id)

        val state = viewModel.uiState.first { it.lastAddedTitle != null }
        assertEquals("雪中悍刀行", state.lastAddedTitle)
        assertEquals(1, addRemoteBookUseCase.invocations.size)
    }
}

private class FakeAddRemoteBookToShelfUseCase : AddRemoteBookToShelf {
    val invocations = mutableListOf<RemoteSearchResult>()

    override suspend fun invoke(result: RemoteSearchResult) {
        invocations += result
    }
}

private class FakeBridgeRepository(
    private val searchResults: List<RemoteSearchResult>,
) : SourceBridgeRepository {
    override suspend fun search(query: String, sourceIds: List<String>): List<RemoteSearchResult> = searchResults

    override suspend fun fetchBookDetail(sourceId: String, remoteBookId: String): RemoteBookDetail =
        RemoteBookDetail(title = remoteBookId)

    override suspend fun fetchToc(sourceId: String, remoteBookId: String): List<RemoteChapter> = emptyList()

    override suspend fun fetchChapterContent(sourceId: String, chapterRef: String): RemoteChapterContent =
        RemoteChapterContent(chapterRef = chapterRef, title = chapterRef, content = "")
}

private fun sampleSearchResult(): RemoteSearchResult =
    RemoteSearchResult(
        id = "remote-1",
        sourceId = "source-1",
        title = "雪中悍刀行",
        author = "烽火戏诸侯",
        detailUrl = "https://example.com/book/1",
    )

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
