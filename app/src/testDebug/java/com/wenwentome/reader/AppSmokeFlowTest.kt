package com.wenwentome.reader

import android.app.Application
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
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
import com.wenwentome.reader.core.model.buildReaderChapterLocator
import com.wenwentome.reader.di.AppContainer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppSmokeFlowTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun appShell_topLevelRoutesRemainReachable() {
        val appContainer = AppContainer(ApplicationProvider.getApplicationContext())

        composeTestRule.setContent {
            ReaderApp(appContainer = appContainer)
        }

        composeTestRule.onNodeWithTag("screen").assertTextEquals("书库")
        composeTestRule.onNodeWithText("发现").performClick()
        composeTestRule.onNodeWithText("我的").performClick()
        composeTestRule.onNodeWithTag("settings-cloud-sync-entry").assertExistsCompat()
    }

    @Test
    fun openingWebBook_entersReader_andDoesNotShowWebPlaceholder() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val database =
            Room.inMemoryDatabaseBuilder(application, ReaderDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        val fakeBridge = object : SourceBridgeRepository {
            override suspend fun search(query: String, sourceIds: List<String>): List<RemoteSearchResult> =
                throw UnsupportedOperationException("Not needed in this test")

            override suspend fun fetchBookDetail(sourceId: String, remoteBookId: String): RemoteBookDetail =
                throw UnsupportedOperationException("Not needed in this test")

            override suspend fun fetchToc(sourceId: String, remoteBookId: String): List<RemoteChapter> =
                throw UnsupportedOperationException("Not needed in this test")

            override suspend fun fetchChapterContent(sourceId: String, chapterRef: String): RemoteChapterContent =
                RemoteChapterContent(
                    chapterRef = chapterRef,
                    title = "第一章",
                    content = "真实正文第一段\n\n真实正文第二段",
                )
        }
        val appContainer = AppContainer(
            application = application,
            databaseOverride = database,
            sourceBridgeRepositoryOverride = fakeBridge,
        )

        val bookId = "book-web-1"
        runBlocking {
            database.bookRecordDao().upsert(
                BookRecord(
                    id = bookId,
                    title = "测试网文",
                    author = "测试作者",
                    originType = OriginType.WEB,
                    primaryFormat = BookFormat.WEB,
                    summary = null,
                ).toEntity()
            )
            database.remoteBindingDao().upsert(
                RemoteBinding(
                    bookId = bookId,
                    sourceId = "fake-src",
                    remoteBookId = "https://example.com/book",
                    remoteBookUrl = "https://example.com/book",
                    tocRef = "https://example.com/chapter-1",
                    latestKnownChapterRef = "https://example.com/chapter-1",
                ).toEntity()
            )
        }

        composeTestRule.setContent {
            ReaderApp(appContainer = appContainer)
        }

        composeTestRule.waitUntilTagExists("book-cover-card-$bookId")
        composeTestRule.onNodeWithTag("book-cover-card-$bookId").performClick()
        composeTestRule.waitUntilTagExists("book-detail")
        composeTestRule.onNodeWithTag("book-detail").performScrollToNode(hasTestTag("detail-read-button"))
        composeTestRule.onNodeWithTag("detail-read-button").performClick()
        composeTestRule.waitUntilTagExists("reader-screen")
        composeTestRule.waitUntilTextExists("第一章")
        composeTestRule.waitUntilTextExists("真实正文第一段")

        // Task 4: web-origin content should come from source bridge + binding, not placeholder summary.
        assertThrows(AssertionError::class.java) {
            composeTestRule.onNodeWithText("网文正文桥接将在后续任务接入。").assertTextEquals("网文正文桥接将在后续任务接入。")
        }
        composeTestRule.onNodeWithText("第一章").assertTextEquals("第一章")
        composeTestRule.onNodeWithText("真实正文第一段").assertTextEquals("真实正文第一段")
    }

    @Test
    fun openingWebBook_withoutHistory_prefersTocRefInsteadOfLatestKnownChapterRef() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val database =
            Room.inMemoryDatabaseBuilder(application, ReaderDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        val firstChapterRef = "https://example.com/chapter-1"
        val latestChapterRef = "https://example.com/chapter-9"
        val fakeBridge = object : SourceBridgeRepository {
            override suspend fun search(query: String, sourceIds: List<String>): List<RemoteSearchResult> =
                throw UnsupportedOperationException("Not needed in this test")

            override suspend fun fetchBookDetail(sourceId: String, remoteBookId: String): RemoteBookDetail =
                throw UnsupportedOperationException("Not needed in this test")

            override suspend fun fetchToc(sourceId: String, remoteBookId: String): List<RemoteChapter> =
                throw UnsupportedOperationException("Not needed in this test")

            override suspend fun fetchChapterContent(sourceId: String, chapterRef: String): RemoteChapterContent =
                when (chapterRef) {
                    firstChapterRef ->
                        RemoteChapterContent(
                            chapterRef = chapterRef,
                            title = "第一章",
                            content = "第一章正文第一段\n\n第一章正文第二段",
                        )

                    latestChapterRef ->
                        RemoteChapterContent(
                            chapterRef = chapterRef,
                            title = "第九章",
                            content = "第九章正文第一段\n\n第九章正文第二段",
                        )

                    else -> error("unexpected chapterRef: $chapterRef")
                }
        }
        val appContainer = AppContainer(
            application = application,
            databaseOverride = database,
            sourceBridgeRepositoryOverride = fakeBridge,
        )

        val bookId = "book-web-first-chapter"
        runBlocking {
            database.bookRecordDao().upsert(
                BookRecord(
                    id = bookId,
                    title = "首章优先测试书",
                    author = "测试作者",
                    originType = OriginType.WEB,
                    primaryFormat = BookFormat.WEB,
                    summary = null,
                ).toEntity()
            )
            database.remoteBindingDao().upsert(
                RemoteBinding(
                    bookId = bookId,
                    sourceId = "fake-src",
                    remoteBookId = "https://example.com/book",
                    remoteBookUrl = "https://example.com/book",
                    tocRef = firstChapterRef,
                    latestKnownChapterRef = latestChapterRef,
                ).toEntity()
            )
        }

        composeTestRule.setContent {
            ReaderApp(appContainer = appContainer)
        }

        composeTestRule.onNodeWithTag("book-cover-card-$bookId").performClick()
        composeTestRule.waitUntilTagExists("book-detail")
        composeTestRule.onNodeWithTag("book-detail").performScrollToNode(hasTestTag("detail-read-button"))
        composeTestRule.onNodeWithTag("detail-read-button").performClick()
        composeTestRule.waitUntilTagExists("reader-screen")
        composeTestRule.waitUntilTextExists("第一章正文第一段")
        composeTestRule.onNodeWithTag("reader-chapter-title").assertTextEquals("第一章")
        assertThrows(AssertionError::class.java) {
            composeTestRule.onNodeWithText("第九章正文第一段").assertTextEquals("第九章正文第一段")
        }
    }

    @Test
    fun openingWebBook_withoutLatestKnownChapterRef_rendersTocFailureMessage() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val database =
            Room.inMemoryDatabaseBuilder(application, ReaderDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        val fakeBridge = object : SourceBridgeRepository {
            override suspend fun search(query: String, sourceIds: List<String>): List<RemoteSearchResult> =
                throw UnsupportedOperationException("Not needed in this test")

            override suspend fun fetchBookDetail(sourceId: String, remoteBookId: String): RemoteBookDetail =
                throw UnsupportedOperationException("Not needed in this test")

            override suspend fun fetchToc(sourceId: String, remoteBookId: String): List<RemoteChapter> =
                error("目录拉取失败")

            override suspend fun fetchChapterContent(sourceId: String, chapterRef: String): RemoteChapterContent =
                throw UnsupportedOperationException("Not needed in this test")
        }
        val appContainer = AppContainer(
            application = application,
            databaseOverride = database,
            sourceBridgeRepositoryOverride = fakeBridge,
        )

        val bookId = "book-web-toc-error"
        runBlocking {
            database.bookRecordDao().upsert(
                BookRecord(
                    id = bookId,
                    title = "目录失败测试书",
                    author = "测试作者",
                    originType = OriginType.WEB,
                    primaryFormat = BookFormat.WEB,
                    summary = null,
                ).toEntity()
            )
            database.remoteBindingDao().upsert(
                RemoteBinding(
                    bookId = bookId,
                    sourceId = "fake-src",
                    remoteBookId = "https://example.com/book",
                    remoteBookUrl = "https://example.com/book",
                    tocRef = null,
                    latestKnownChapterRef = null,
                ).toEntity()
            )
        }

        composeTestRule.setContent {
            ReaderApp(appContainer = appContainer)
        }

        composeTestRule.onNodeWithTag("book-cover-card-$bookId").performClick()
        composeTestRule.waitUntilTagExists("book-detail")
        composeTestRule.onNodeWithTag("book-detail").performScrollToNode(hasTestTag("detail-read-button"))
        composeTestRule.onNodeWithTag("detail-read-button").performClick()
        composeTestRule.waitUntilTagExists("reader-screen")
        composeTestRule.waitUntilTextExists("目录拉取失败")
        composeTestRule.onNodeWithText("目录拉取失败").assertTextEquals("目录拉取失败")
    }

    @Test
    fun savingWebBookProgress_persistsRemoteChapterRefInsteadOfTitle() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val database =
            Room.inMemoryDatabaseBuilder(application, ReaderDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        val chapterRef = "https://example.com/chapter-1"
        val fakeBridge = object : SourceBridgeRepository {
            override suspend fun search(query: String, sourceIds: List<String>): List<RemoteSearchResult> =
                throw UnsupportedOperationException("Not needed in this test")

            override suspend fun fetchBookDetail(sourceId: String, remoteBookId: String): RemoteBookDetail =
                throw UnsupportedOperationException("Not needed in this test")

            override suspend fun fetchToc(sourceId: String, remoteBookId: String): List<RemoteChapter> =
                throw UnsupportedOperationException("Not needed in this test")

            override suspend fun fetchChapterContent(sourceId: String, chapterRef: String): RemoteChapterContent =
                RemoteChapterContent(
                    chapterRef = chapterRef,
                    title = "第一章",
                    content = "真实正文第一段\n\n真实正文第二段",
                )
        }
        val appContainer = AppContainer(
            application = application,
            databaseOverride = database,
            sourceBridgeRepositoryOverride = fakeBridge,
        )

        val bookId = "book-web-save-progress"
        runBlocking {
            database.bookRecordDao().upsert(
                BookRecord(
                    id = bookId,
                    title = "进度保存测试书",
                    author = "测试作者",
                    originType = OriginType.WEB,
                    primaryFormat = BookFormat.WEB,
                    summary = null,
                ).toEntity()
            )
            database.remoteBindingDao().upsert(
                RemoteBinding(
                    bookId = bookId,
                    sourceId = "fake-src",
                    remoteBookId = "https://example.com/book",
                    remoteBookUrl = "https://example.com/book",
                    tocRef = chapterRef,
                    latestKnownChapterRef = chapterRef,
                ).toEntity()
            )
        }

        composeTestRule.setContent {
            ReaderApp(appContainer = appContainer)
        }

        composeTestRule.onNodeWithTag("book-cover-card-$bookId").performClick()
        composeTestRule.waitUntilTagExists("book-detail")
        composeTestRule.onNodeWithTag("book-detail").performScrollToNode(hasTestTag("detail-read-button"))
        composeTestRule.onNodeWithTag("detail-read-button").performClick()
        composeTestRule.waitUntilTagExists("reader-screen")
        composeTestRule.waitUntilTextExists("第一章")
        composeTestRule.onNodeWithText("保存进度").performClick()
        composeTestRule.waitForIdle()

        val savedState = runBlocking {
            database.readingStateDao().observeByBookId(bookId).first()
        }
        assertEquals(buildReaderChapterLocator(BookFormat.WEB, chapterRef), savedState?.locator)
        assertEquals(chapterRef, savedState?.chapterRef)
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
