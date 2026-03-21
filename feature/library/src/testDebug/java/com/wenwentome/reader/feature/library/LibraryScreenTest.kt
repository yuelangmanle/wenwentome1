package com.wenwentome.reader.feature.library

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import com.wenwentome.reader.core.model.BookFormat
import com.wenwentome.reader.core.model.BookRecord
import com.wenwentome.reader.core.model.OriginType
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LibraryScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun rendersContinueReadingCardAndBookActionsMenu() {
        composeTestRule.setContent {
            LibraryScreen(
                state = sampleState(),
                onImportClick = {},
                onContinueReadingClick = {},
                onBookClick = {},
                onImportPhoto = {},
                onRefreshCover = {},
                onRestoreAutomaticCover = {},
                onRefreshCatalog = {},
            )
        }

        composeTestRule.onNodeWithTag("continue-reading-card").assertExistsCompat()
        composeTestRule.onNodeWithTag("book-cover-card-book-1").assertExistsCompat()
        composeTestRule.onNodeWithTag("book-cover-card-book-1").performTouchInput { longClick() }
        composeTestRule.onNodeWithText("书籍操作").assertExistsCompat()
    }

    @Test
    fun libraryScreen_exposesSectionTagsAndMenuBoundaries() {
        composeTestRule.setContent {
            LibraryScreen(
                state = sampleState(),
                onImportClick = {},
                onContinueReadingClick = {},
                onBookClick = {},
                onImportPhoto = {},
                onRefreshCover = {},
                onRestoreAutomaticCover = {},
                onRefreshCatalog = {},
            )
        }

        composeTestRule.onNodeWithTag("library-hero-section").assertExistsCompat()
        composeTestRule.onNodeWithTag("library-grid-section").assertExistsCompat()
        composeTestRule.onNodeWithTag("book-cover-card-book-1").performTouchInput { longClick() }
        composeTestRule.onNodeWithText("打开详情").assertExistsCompat()
        composeTestRule.onNodeWithText("刷新目录").assertExistsCompat()
        composeTestRule.onNodeWithText("导入照片").assertDoesNotExist()
        composeTestRule.onNodeWithText("刷新封面").assertDoesNotExist()
        composeTestRule.onNodeWithText("恢复自动封面").assertDoesNotExist()
    }

    private fun sampleState() =
        LibraryUiState(
            continueReading = LibraryBookItem(
                book = BookRecord(
                    id = "book-2",
                    title = "三体",
                    author = "刘慈欣",
                    originType = OriginType.LOCAL,
                    primaryFormat = BookFormat.EPUB,
                ),
                effectiveCover = null,
                progressPercent = 0.42f,
                progressLabel = "42%",
                hasUpdates = false,
                canRestoreAutomaticCover = false,
            ),
            visibleBooks = listOf(
                LibraryBookItem(
                    book = BookRecord(
                        id = "book-1",
                        title = "雪中悍刀行",
                        author = "烽火戏诸侯",
                        originType = OriginType.WEB,
                        primaryFormat = BookFormat.WEB,
                    ),
                    effectiveCover = "https://example.com/cover.jpg",
                    progressPercent = 0.66f,
                    progressLabel = "66%",
                    hasUpdates = true,
                    canRestoreAutomaticCover = true,
                )
            ),
        )
}

private fun androidx.compose.ui.test.SemanticsNodeInteraction.assertExistsCompat() {
    fetchSemanticsNode()
}
