package com.wenwentome.reader

import android.app.Application
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.wenwentome.reader.bridge.source.SourceBridgeRepository
import com.wenwentome.reader.bridge.source.model.RemoteBookDetail
import com.wenwentome.reader.bridge.source.model.RemoteChapter
import com.wenwentome.reader.bridge.source.model.RemoteChapterContent
import com.wenwentome.reader.bridge.source.model.RemoteSearchResult
import com.wenwentome.reader.core.database.ReaderDatabase
import com.wenwentome.reader.core.database.toEntity
import com.wenwentome.reader.core.model.BookFormat
import com.wenwentome.reader.core.model.BookRecord
import com.wenwentome.reader.core.model.OriginType
import com.wenwentome.reader.core.model.RemoteBinding
import com.wenwentome.reader.di.AppContainer
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
class AppReaderFlowTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun appReaderFlow_canOpenBookDetailThenReaderThenToc() {
        val appContainer = createWebReaderAppContainer()

        composeTestRule.setContent {
            ReaderApp(appContainer = appContainer)
        }

        composeTestRule.onNodeWithTag("book-cover-card-book-web-flow").performClick()
        composeTestRule.waitUntilTagExists("book-detail")
        composeTestRule.onNodeWithTag("book-detail").performScrollToNode(hasText("开始阅读"))
        composeTestRule.onNodeWithTag("detail-read-button").performClick()
        composeTestRule.waitUntilTagExists("reader-screen")
        composeTestRule.onNodeWithText("目录").performClick()
        composeTestRule.waitUntilTagExists("reader-toc-sheet")
        composeTestRule.onNodeWithTag("reader-toc-sheet").assertExistsCompat()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag("toc-latest-chapter").fetchSemanticsNodes().isNotEmpty() ||
                composeTestRule.onAllNodesWithTag("toc-current-chapter").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun appReaderFlow_jumpToLatest_opensReaderAtLatestChapter() {
        val appContainer = createWebReaderAppContainer()

        composeTestRule.setContent {
            ReaderApp(appContainer = appContainer)
        }

        composeTestRule.onNodeWithTag("book-cover-card-book-web-flow").performClick()
        composeTestRule.waitUntilTagExists("book-detail")
        composeTestRule.onNodeWithTag("book-detail").performScrollToNode(hasText("跳转最新章"))
        composeTestRule.onNodeWithText("跳转最新章").performClick()
        composeTestRule.waitUntilTagExists("reader-screen")
        composeTestRule.waitUntilTextExists("最新章正文第一段")
        composeTestRule.onNodeWithTag("reader-chapter-title").assertTextContains("最新章")
    }

    @Test
    fun appReaderFlow_discoverPreviewCanAddBookIntoBookshelf() {
        val harness = createDiscoverReaderHarness()

        composeTestRule.setContent {
            ReaderApp(appContainer = harness.appContainer)
        }

        composeTestRule.onNodeWithTag("nav-discover").performClick()
        composeTestRule.onNodeWithTag("discover-search-input").performTextInput("雪中")
        composeTestRule.waitUntilTagExists("discover-result-remote-discover-flow")
        composeTestRule.onNodeWithTag("discover-result-remote-discover-flow").performClick()
        composeTestRule.waitUntilTagExists("discover-selected-preview")
        composeTestRule.waitUntilTextExists("最新章节：最新章")
        composeTestRule.onNodeWithTag("discover-preview-add-button").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("加入中").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            harness.tocRequestCount() > 0
        }
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            runBlocking {
                harness.database.bookRecordDao().getAll().isNotEmpty()
            }
        }
        val addedBookId = runBlocking {
            harness.database.bookRecordDao().getAll().single().id
        }

        composeTestRule.onNodeWithTag("nav-bookshelf").performClick()
        composeTestRule.waitUntilTagExists("library-screen")
        composeTestRule.waitUntilTagExists("book-cover-card-$addedBookId")
        composeTestRule.onNodeWithTag("book-cover-card-$addedBookId").assertExistsCompat()
    }

    private fun createWebReaderAppContainer(): AppContainer {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val database =
            Room.inMemoryDatabaseBuilder(application, ReaderDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        val fakeBridge = object : SourceBridgeRepository {
            override suspend fun search(query: String, sourceIds: List<String>): List<RemoteSearchResult> =
                throw UnsupportedOperationException("Not needed in this test")

            override suspend fun fetchBookDetail(sourceId: String, remoteBookId: String): RemoteBookDetail =
                RemoteBookDetail(
                    title = "测试网文流",
                    author = "测试作者",
                    summary = "用于验证详情页到阅读器的闭环。",
                    lastChapter = "最新章",
                )

            override suspend fun fetchToc(sourceId: String, remoteBookId: String): List<RemoteChapter> =
                listOf(
                    RemoteChapter(chapterRef = "chapter-1", title = "第一章"),
                    RemoteChapter(chapterRef = "chapter-latest", title = "最新章"),
                )

            override suspend fun fetchChapterContent(sourceId: String, chapterRef: String): RemoteChapterContent =
                when (chapterRef) {
                    "chapter-latest" ->
                        RemoteChapterContent(
                            chapterRef = chapterRef,
                            title = "最新章",
                            content = "最新章正文第一段\n\n最新章正文第二段",
                        )

                    else ->
                        RemoteChapterContent(
                            chapterRef = chapterRef,
                            title = "第一章",
                            content = "第一章正文第一段\n\n第一章正文第二段",
                        )
                }
        }
        val appContainer = AppContainer(
            application = application,
            databaseOverride = database,
            sourceBridgeRepositoryOverride = fakeBridge,
        )

        runBlocking {
            database.bookRecordDao().upsert(
                BookRecord(
                    id = "book-web-flow",
                    title = "测试网文流",
                    author = "测试作者",
                    originType = OriginType.WEB,
                    primaryFormat = BookFormat.WEB,
                    summary = "用于验证详情页到阅读器的闭环。",
                ).toEntity()
            )
            database.remoteBindingDao().upsert(
                RemoteBinding(
                    bookId = "book-web-flow",
                    sourceId = "fake-src",
                    remoteBookId = "remote-flow",
                    remoteBookUrl = "https://example.com/books/flow",
                    tocRef = "chapter-1",
                    latestKnownChapterRef = "chapter-latest",
                ).toEntity()
            )
        }

        return appContainer
    }

    private fun createDiscoverReaderHarness(): DiscoverReaderHarness {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val database =
            Room.inMemoryDatabaseBuilder(application, ReaderDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        val tocRequests = AtomicInteger(0)
        val fakeBridge = object : SourceBridgeRepository {
            override suspend fun search(query: String, sourceIds: List<String>): List<RemoteSearchResult> =
                if (query.isBlank()) {
                    emptyList()
                } else {
                    listOf(
                        RemoteSearchResult(
                            id = "remote-discover-flow",
                            sourceId = "discover-source",
                            title = "发现页阅读最新测试书",
                            author = "测试作者",
                            detailUrl = "https://example.com/books/discover",
                        )
                    )
                }

            override suspend fun fetchBookDetail(sourceId: String, remoteBookId: String): RemoteBookDetail =
                RemoteBookDetail(
                    title = "发现页阅读最新测试书",
                    author = "测试作者",
                    summary = "用于验证发现页预览和阅读最新的闭环。",
                    lastChapter = "最新章",
                )

            override suspend fun fetchToc(sourceId: String, remoteBookId: String): List<RemoteChapter> {
                tocRequests.incrementAndGet()
                return listOf(
                    RemoteChapter(chapterRef = "chapter-1", title = "第一章"),
                    RemoteChapter(chapterRef = "chapter-latest", title = "最新章"),
                )
            }

            override suspend fun fetchChapterContent(sourceId: String, chapterRef: String): RemoteChapterContent =
                when (chapterRef) {
                    "chapter-latest" ->
                        RemoteChapterContent(
                            chapterRef = chapterRef,
                            title = "最新章",
                            content = "最新章正文第一段\n\n最新章正文第二段",
                        )

                    else ->
                        RemoteChapterContent(
                            chapterRef = chapterRef,
                            title = "第一章",
                            content = "第一章正文第一段\n\n第一章正文第二段",
                        )
                }
        }
        return DiscoverReaderHarness(
            appContainer = AppContainer(
                application = application,
                databaseOverride = database,
                sourceBridgeRepositoryOverride = fakeBridge,
            ),
            database = database,
            tocRequestCount = tocRequests::get,
        )
    }
}

private data class DiscoverReaderHarness(
    val appContainer: AppContainer,
    val database: ReaderDatabase,
    val tocRequestCount: () -> Int,
)

private fun ComposeContentTestRule.waitUntilTagExists(tag: String, timeoutMillis: Long = 5_000) {
    waitUntil(timeoutMillis = timeoutMillis) {
        onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
    }
}

private fun ComposeContentTestRule.waitUntilTextExists(text: String, timeoutMillis: Long = 5_000) {
    waitUntil(timeoutMillis = timeoutMillis) {
        onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
    }
}

private fun androidx.compose.ui.test.SemanticsNodeInteraction.assertExistsCompat() {
    fetchSemanticsNode()
}
