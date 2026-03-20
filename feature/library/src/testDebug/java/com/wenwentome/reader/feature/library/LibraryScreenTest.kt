package com.wenwentome.reader.feature.library

import androidx.compose.ui.test.assertExists
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
                onBookClick = {},
                onImportPhoto = {},
                onRefreshCover = {},
                onRestoreAutomaticCover = {},
                onRefreshCatalog = {},
            )
        }

        composeTestRule.onNodeWithTag("continue-reading-card").assertExists()
        composeTestRule.onNodeWithTag("book-cover-card-book-1").assertExists()
        composeTestRule.onNodeWithTag("book-cover-card-book-1").performTouchInput { longClick() }
        composeTestRule.onNodeWithText("导入照片").assertExists()
        composeTestRule.onNodeWithText("恢复自动封面").assertExists()
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
