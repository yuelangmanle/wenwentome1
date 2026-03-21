package com.wenwentome.reader.feature.library

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import com.wenwentome.reader.core.model.BookFormat
import com.wenwentome.reader.core.model.BookRecord
import com.wenwentome.reader.core.model.OriginType
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
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
        composeTestRule.onNodeWithText("导入照片").assertDoesNotExistCompat()
        composeTestRule.onNodeWithText("刷新封面").assertDoesNotExistCompat()
        composeTestRule.onNodeWithText("恢复自动封面").assertDoesNotExistCompat()
    }

    @Test
    fun libraryScreen_prefersReadableLocalCoverOverPlaceholder() {
        val readableCoverUri = createReadableLocalCoverUri()
        val state = sampleState(readableCoverUri)

        composeTestRule.setContent {
            LibraryScreen(
                state = state,
                onImportClick = {},
                onContinueReadingClick = {},
                onBookClick = {},
                onRefreshCatalog = {},
            )
        }

        composeTestRule.waitUntilTagExists("continue-reading-real-cover")
        composeTestRule.waitUntilTagGone("continue-reading-placeholder-cover")
        composeTestRule.onNodeWithTag("continue-reading-real-cover").assertExistsCompat()
        composeTestRule.onNodeWithTag("continue-reading-placeholder-cover").assertDoesNotExistCompat()

        val book1Index = state.visibleBooks.indexOfFirst { it.book.id == "book-1" }
        assertTrue("预期 sampleState.visibleBooks 中包含 book-1", book1Index >= 0)
        composeTestRule.onNodeWithTag("library-grid-section").performScrollToIndex(book1Index)
        composeTestRule.waitUntilTagExists("book-cover-real-cover-book-1")
        composeTestRule.waitUntilTagGone("book-cover-placeholder-book-1")
        composeTestRule.onNodeWithTag("book-cover-real-cover-book-1").assertExistsCompat()
        composeTestRule.onNodeWithTag("book-cover-placeholder-book-1").assertDoesNotExistCompat()
    }

    private fun sampleState(readableCoverUri: String? = null) =
        LibraryUiState(
            continueReading = LibraryBookItem(
                book = BookRecord(
                    id = "book-2",
                    title = "三体",
                    author = "刘慈欣",
                    originType = OriginType.LOCAL,
                    primaryFormat = BookFormat.EPUB,
                ),
                effectiveCover = readableCoverUri,
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
                        effectiveCover = readableCoverUri ?: "https://example.com/cover.jpg",
                        progressPercent = 0.66f,
                        progressLabel = "66%",
                        hasUpdates = true,
                        canRestoreAutomaticCover = true,
                    )
                )
            },
        )

    private fun createReadableLocalCoverUri(): String {
        val file = File.createTempFile("library-cover", ".png")
        val bitmap = Bitmap.createBitmap(2, 3, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.rgb(182, 122, 74))
        }
        file.outputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
        bitmap.recycle()
        return file.toURI().toString()
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
) {
    waitUntil(timeoutMillis) {
        runCatching { onNodeWithTag(tag).fetchSemanticsNode() }.isSuccess
    }
}

private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.waitUntilTagGone(
    tag: String,
    timeoutMillis: Long = 5_000L,
) {
    waitUntil(timeoutMillis) {
        runCatching { onNodeWithTag(tag).fetchSemanticsNode() }.isFailure
    }
}
