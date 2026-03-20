package com.wenwentome.reader

import android.app.Application
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
        composeTestRule.onNodeWithText("立即备份").assertTextEquals("立即备份")
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

        composeTestRule.onNodeWithTag("book-$bookId").assertExists().performClick()
        composeTestRule.onNodeWithTag("book-detail").assertExists()
        composeTestRule.onNodeWithText("开始阅读").performClick()
        composeTestRule.onNodeWithTag("reader-screen").assertExists()

        // Task 4: web-origin content should come from source bridge + binding, not placeholder summary.
        composeTestRule.onNodeWithText("网文正文桥接将在后续任务接入。").assertDoesNotExist()
        composeTestRule.onNodeWithText("第一章").assertExists()
        composeTestRule.onNodeWithText("真实正文第一段").assertExists()
    }
}
