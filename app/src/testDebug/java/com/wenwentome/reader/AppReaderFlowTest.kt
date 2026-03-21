package com.wenwentome.reader

import android.app.Application
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.DialogNavigator
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.wenwentome.reader.bridge.source.SourceBridgeRepository
import com.wenwentome.reader.bridge.source.model.RemoteBookDetail
import com.wenwentome.reader.bridge.source.model.RemoteChapter
import com.wenwentome.reader.bridge.source.model.RemoteChapterContent
import com.wenwentome.reader.bridge.source.model.RemoteSearchResult
import com.wenwentome.reader.core.database.ReaderDatabase
import com.wenwentome.reader.core.database.toEntity
import com.wenwentome.reader.core.database.toModel
import com.wenwentome.reader.core.model.BookFormat
import com.wenwentome.reader.core.model.BookRecord
import com.wenwentome.reader.core.model.OriginType
import com.wenwentome.reader.core.model.ReadingState
import com.wenwentome.reader.core.model.RemoteBinding
import com.wenwentome.reader.core.model.buildReaderParagraphLocator
import com.wenwentome.reader.di.AppContainer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

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
        composeTestRule.onNodeWithTag("book-detail").performScrollToNode(hasTestTag("detail-read-button"))
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
    fun appReaderFlow_continueReadingCardOpensReaderDirectly() {
        val appContainer = createWebReaderAppContainer(
            readingState = ReadingState(
                bookId = "book-web-flow",
                locator = "chapter-latest",
                chapterRef = "chapter-latest",
                progressPercent = 0.66f,
            ),
        )

        composeTestRule.setContent {
            ReaderApp(appContainer = appContainer)
        }

        composeTestRule.waitUntilTagExists("continue-reading-card")
        composeTestRule.onNodeWithTag("continue-reading-card").performClick()
        composeTestRule.waitUntilTagExists("reader-screen")
        composeTestRule.waitUntilTextExists("最新章正文第一段")
        composeTestRule.onNodeWithTag("reader-chapter-title").assertTextContains("最新章")
    }

    @Test
    fun appReaderFlow_savedReaderProgressFlowsBackToBookshelf() {
        val appContainer = createWebReaderAppContainer()
        val application = ApplicationProvider.getApplicationContext<Application>()
        val navController =
            NavHostController(application).apply {
                navigatorProvider.addNavigator(ComposeNavigator())
                navigatorProvider.addNavigator(DialogNavigator())
            }

        composeTestRule.setContent {
            ReaderApp(
                appContainer = appContainer,
                navController = navController,
            )
        }

        composeTestRule.onNodeWithTag("book-cover-card-book-web-flow").performClick()
        composeTestRule.waitUntilTagExists("book-detail")
        composeTestRule.onNodeWithTag("book-detail").performScrollToNode(hasTestTag("detail-read-button"))
        composeTestRule.onNodeWithTag("detail-read-button").performClick()
        composeTestRule.waitUntilTagExists("reader-screen")
        composeTestRule.waitUntilTextExists("第 1 / 3 页")
        composeTestRule.onNodeWithText("下一页").performClick()
        composeTestRule.waitUntilTextExists("第 2 / 3 页")
        composeTestRule.onNodeWithText("保存进度").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            runBlocking {
                appContainer.database.readingStateDao()
                    .observeByBookId("book-web-flow")
                    .first()
                    ?.toModel()
                    ?.progressPercent
                    ?.let { it > 0.4f } == true
            }
        }
        composeTestRule.runOnIdle {
            navController.navigate("bookshelf")
        }
        composeTestRule.waitUntilTagExists("library-screen")
        composeTestRule.waitUntilTextExists("已读 43%")
    }

    @Test
    fun appReaderFlow_reopenWebBookRestoresSavedParagraphPage() {
        val appContainer = createWebReaderAppContainer(
            readingState = ReadingState(
                bookId = "book-web-flow",
                locator = buildReaderParagraphLocator(BookFormat.WEB, "chapter-latest", 3),
                chapterRef = "chapter-latest",
                progressPercent = 0.43f,
            ),
        )

        composeTestRule.setContent {
            ReaderApp(appContainer = appContainer)
        }

        composeTestRule.waitUntilTagExists("continue-reading-card")
        composeTestRule.onNodeWithTag("continue-reading-card").performClick()
        composeTestRule.waitUntilTagExists("reader-screen")
        composeTestRule.waitUntilTextExists("第 2 / 3 页")
    }

    private fun createWebReaderAppContainer(
        readingState: ReadingState? = null,
    ): AppContainer {
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
                            content = listOf(
                                "最新章正文第一段",
                                "最新章正文第二段",
                                "最新章正文第三段",
                                "最新章正文第四段",
                                "最新章正文第五段",
                                "最新章正文第六段",
                                "最新章正文第七段",
                                "最新章正文第八段",
                            ).joinToString(separator = "\n\n"),
                        )

                    else ->
                        RemoteChapterContent(
                            chapterRef = chapterRef,
                            title = "第一章",
                            content = listOf(
                                "第一章正文第一段",
                                "第一章正文第二段",
                                "第一章正文第三段",
                                "第一章正文第四段",
                                "第一章正文第五段",
                                "第一章正文第六段",
                                "第一章正文第七段",
                                "第一章正文第八段",
                            ).joinToString(separator = "\n\n"),
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
            readingState?.let { database.readingStateDao().upsert(it.toEntity()) }
        }

        return appContainer
    }

}

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