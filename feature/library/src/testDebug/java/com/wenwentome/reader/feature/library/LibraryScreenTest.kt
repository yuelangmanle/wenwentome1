package com.wenwentome.reader.feature.library

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import androidx.test.core.app.ApplicationProvider
import com.wenwentome.reader.core.model.BookFormat
import com.wenwentome.reader.core.model.BookRecord
import com.wenwentome.reader.core.model.OriginType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayOutputStream
import java.io.File

@RunWith(RobolectricTestRunner::class)
class LibraryScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun rendersContinueReadingCardAndBookActionsMenu() {
        val state = sampleState()
        composeTestRule.setContent {
            LibraryScreen(
                state = state,
                onImportClick = {},
                onContinueReadingClick = {},
                onBookClick = {},
                onRefreshCatalog = {},
                onRefreshCover = {},
                onImportPhoto = {},
                onRestoreAutomaticCover = {},
            )
        }

        composeTestRule.onNodeWithTag("continue-reading-card").assertExistsCompat()
        val book1Index = state.visibleBooks.indexOfFirst { it.book.id == "book-1" }
        assertTrue("预期 sampleState.visibleBooks 中包含 book-1", book1Index >= 0)
        composeTestRule.onNodeWithTag("library-grid-section").performScrollToIndex(
            book1Index
        )
        composeTestRule.onNodeWithTag("book-cover-card-book-1").assertExistsCompat()
        composeTestRule.onNodeWithTag("book-cover-card-book-1").performTouchInput { longClick() }
        composeTestRule.onNodeWithText("书籍操作").assertExistsCompat()
    }

    @Test
    fun libraryScreen_exposesSectionTagsAndMenuBoundaries() {
        val state = sampleState()
        composeTestRule.setContent {
            LibraryScreen(
                state = state,
                onImportClick = {},
                onContinueReadingClick = {},
                onBookClick = {},
                onRefreshCatalog = {},
                onRefreshCover = {},
                onImportPhoto = {},
                onRestoreAutomaticCover = {},
            )
        }

        composeTestRule.onNodeWithTag("library-hero-section").assertExistsCompat()
        composeTestRule.onNodeWithTag("library-grid-section").assertExistsCompat()
        val book1Index = state.visibleBooks.indexOfFirst { it.book.id == "book-1" }
        assertTrue("预期 sampleState.visibleBooks 中包含 book-1", book1Index >= 0)
        composeTestRule.onNodeWithTag("library-grid-section").performScrollToIndex(
            book1Index
        )
        composeTestRule.onNodeWithTag("book-cover-card-book-1").assertExistsCompat()
        composeTestRule.onNodeWithTag("book-cover-card-book-1").performTouchInput { longClick() }
        composeTestRule.onNodeWithText("打开详情").assertExistsCompat()
        composeTestRule.onNodeWithText("刷新目录").assertExistsCompat()
        composeTestRule.onNodeWithText("刷新封面").assertExistsCompat()
        composeTestRule.onNodeWithText("导入照片").assertExistsCompat()
        composeTestRule.onNodeWithText("恢复自动封面").assertExistsCompat()
    }

    @Test
    fun libraryScreen_bookActionMenuTriggersCoverCallbacks() {
        val state = sampleState()
        val refreshedBookIds = mutableListOf<String>()
        val importedPhotoBookIds = mutableListOf<String>()
        val restoredCoverBookIds = mutableListOf<String>()

        composeTestRule.setContent {
            LibraryScreen(
                state = state,
                onImportClick = {},
                onContinueReadingClick = {},
                onBookClick = {},
                onRefreshCatalog = {},
                onRefreshCover = { refreshedBookIds += it },
                onImportPhoto = { importedPhotoBookIds += it },
                onRestoreAutomaticCover = { restoredCoverBookIds += it },
            )
        }

        val book1Index = state.visibleBooks.indexOfFirst { it.book.id == "book-1" }
        assertTrue("预期 sampleState.visibleBooks 中包含 book-1", book1Index >= 0)
        composeTestRule.onNodeWithTag("library-grid-section").performScrollToIndex(book1Index)

        composeTestRule.onNodeWithTag("book-cover-card-book-1").performTouchInput { longClick() }
        composeTestRule.onNodeWithText("刷新封面").performClick()
        composeTestRule.onNodeWithTag("book-cover-card-book-1").performTouchInput { longClick() }
        composeTestRule.onNodeWithText("导入照片").performClick()
        composeTestRule.onNodeWithTag("book-cover-card-book-1").performTouchInput { longClick() }
        composeTestRule.onNodeWithText("恢复自动封面").performClick()

        org.junit.Assert.assertEquals(listOf("book-1"), refreshedBookIds)
        org.junit.Assert.assertEquals(listOf("book-1"), importedPhotoBookIds)
        org.junit.Assert.assertEquals(listOf("book-1"), restoredCoverBookIds)
    }

    @Test
    fun loadReadableCoverBitmap_decodesIndependentReadableLocalCoverUris() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val continueReadingCoverUri = createReadableLocalCoverUri("continue-reading")
        val bookshelfCoverUri = createReadableLocalCoverUri("bookshelf")
        assertNotNull(loadReadableCoverBitmap(context, continueReadingCoverUri))
        assertNotNull(loadReadableCoverBitmap(context, bookshelfCoverUri))
    }

    @Test
    fun loadReadableCoverBitmap_decodesRemoteCoverUri() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val server = MockWebServer()
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "image/png")
                .setBody(Buffer().write(createPngBytes()))
        )
        server.start()
        try {
            assertNotNull(loadReadableCoverBitmap(context, server.url("/cover.png").toString()))
        } finally {
            server.shutdown()
        }
    }

    private fun sampleState(
        continueReadingCoverUri: String? = null,
        bookshelfCoverUri: String? = null,
    ) =
        LibraryUiState(
            continueReading = LibraryBookItem(
                book = BookRecord(
                    id = "book-2",
                    title = "三体",
                    author = "刘慈欣",
                    originType = OriginType.LOCAL,
                    primaryFormat = BookFormat.EPUB,
                ),
                effectiveCover = continueReadingCoverUri,
                progressPercent = 0.42f,
                progressLabel = "42%",
                hasUpdates = false,
                canRestoreAutomaticCover = false,
            ),
            // 让 book-1 位于列表靠后位置，测试用例必须通过 library-grid-section 滚动才能触达它。
            visibleBooks = buildList {
                for (i in 3..52) {
                    add(
                        LibraryBookItem(
                            book = BookRecord(
                                id = "book-$i",
                                title = "示例书籍 $i",
                                author = "作者 $i",
                                originType = OriginType.LOCAL,
                                primaryFormat = BookFormat.EPUB,
                            ),
                            effectiveCover = null,
                            progressPercent = 0f,
                            progressLabel = "0%",
                            hasUpdates = false,
                            canRestoreAutomaticCover = false,
                        )
                    )
                }
                add(
                    LibraryBookItem(
                        book = BookRecord(
                            id = "book-1",
                            title = "雪中悍刀行",
                            author = "烽火戏诸侯",
                            originType = OriginType.WEB,
                            primaryFormat = BookFormat.WEB,
                        ),
                        effectiveCover = bookshelfCoverUri ?: "https://example.com/cover.jpg",
                        progressPercent = 0.66f,
                        progressLabel = "66%",
                        hasUpdates = true,
                        canRestoreAutomaticCover = true,
                    )
                )
            },
        )

    private fun createReadableLocalCoverUri(prefix: String): String {
        val cacheDir = ApplicationProvider.getApplicationContext<android.content.Context>().cacheDir
        val file = File.createTempFile("library-cover-$prefix", ".png", cacheDir)
        file.deleteOnExit()
        val bitmap = Bitmap.createBitmap(16, 24, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.rgb(182, 122, 74))
        }
        file.outputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
        bitmap.recycle()
        return file.toURI().toString()
    }

    private fun createPngBytes(): ByteArray {
        val bitmap = Bitmap.createBitmap(16, 24, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.rgb(182, 122, 74))
        }
        return ByteArrayOutputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            bitmap.recycle()
            output.toByteArray()
        }
    }
}

private fun androidx.compose.ui.test.SemanticsNodeInteraction.assertExistsCompat() {
    fetchSemanticsNode()
}

private fun androidx.compose.ui.test.SemanticsNodeInteraction.assertDoesNotExistCompat() {
    check(runCatching { fetchSemanticsNode() }.isFailure) {
        "Expected semantics node to be absent, but it still exists."
    }
}

private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.waitUntilTagExists(
    tag: String,
    timeoutMillis: Long = 5_000L,
    useUnmergedTree: Boolean = false,
) {
    waitUntil(timeoutMillis) {
        runCatching { onNodeWithTag(tag, useUnmergedTree = useUnmergedTree).fetchSemanticsNode() }.isSuccess
    }
}

private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.waitUntilTagGone(
    tag: String,
    timeoutMillis: Long = 5_000L,
    useUnmergedTree: Boolean = false,
) {
    waitUntil(timeoutMillis) {
        runCatching { onNodeWithTag(tag, useUnmergedTree = useUnmergedTree).fetchSemanticsNode() }.isFailure
    }
}
