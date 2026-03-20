package com.wenwentome.reader.feature.reader

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import com.wenwentome.reader.core.model.BookFormat
import com.wenwentome.reader.core.model.BookRecord
import com.wenwentome.reader.core.model.OriginType
import com.wenwentome.reader.core.model.ReaderChapter
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BookDetailScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun detailScreen_clickImportPhoto_requestsImagePicker() {
        var pickerRequested = false

        composeTestRule.setContent {
            BookDetailScreen(
                state = sampleState(),
                onReadClick = {},
                onToggleCatalog = {},
                onChapterClick = {},
                onRefreshCatalogClick = {},
                onJumpToLatestClick = {},
                onRefreshCoverClick = {},
                onImportPhotoClick = { pickerRequested = true },
                onRestoreAutomaticCoverClick = {},
            )
            if (pickerRequested) {
                Box(modifier = Modifier.testTag("book-cover-picker-requested"))
            }
        }

        composeTestRule.onNodeWithTag("book-detail").performScrollToNode(hasTestTag("cover-import-photo-button"))
        composeTestRule.onNodeWithTag("cover-import-photo-button").performClick()
        composeTestRule.assertTagExists("book-cover-picker-requested")
    }

    @Test
    fun detailScreen_showsReadContinueTocAndProgress() {
        composeTestRule.setContent {
            BookDetailScreen(
                state = sampleState(),
                onReadClick = {},
                onToggleCatalog = {},
                onChapterClick = {},
                onRefreshCatalogClick = {},
                onJumpToLatestClick = {},
                onRefreshCoverClick = {},
                onImportPhotoClick = {},
                onRestoreAutomaticCoverClick = {},
            )
        }

        composeTestRule.onNodeWithTag("detail-read-button").performScrollTo().assertExistsCompat()
        composeTestRule.onNodeWithText("继续阅读").assertExistsCompat()
        composeTestRule.onNodeWithText("查看目录").assertExistsCompat()
        composeTestRule.onNodeWithTag("detail-progress-label").assertTextContains("42%")
    }

    private fun sampleState() =
        BookDetailUiState(
            book = BookRecord(
                id = "book-1",
                title = "测试书",
                author = "测试作者",
                originType = OriginType.WEB,
                primaryFormat = BookFormat.WEB,
                cover = "https://example.com/cover.jpg",
                summary = "一段用于详情页测试的简介。",
            ),
            effectiveCover = "https://example.com/cover.jpg",
            readActionLabel = "继续阅读",
            progressPercent = 0.42f,
            progressLabel = "42%",
            lastReadLabel = "上次读到 第三章",
            showTocAction = true,
            showRefreshCatalogAction = true,
            showJumpToLatestAction = true,
            canRestoreAutomaticCover = true,
            latestChapterRef = "chapter-8",
            latestChapterTitle = "第八章",
            chapters = listOf(
                ReaderChapter(
                    chapterRef = "chapter-3",
                    title = "第三章",
                    orderIndex = 0,
                    sourceType = BookFormat.WEB,
                    locatorHint = "chapter-3",
                ),
                ReaderChapter(
                    chapterRef = "chapter-8",
                    title = "第八章",
                    orderIndex = 1,
                    sourceType = BookFormat.WEB,
                    locatorHint = "chapter-8",
                    isLatest = true,
                ),
            ),
        )
}

private fun androidx.compose.ui.test.SemanticsNodeInteraction.assertExistsCompat() {
    fetchSemanticsNode()
}

private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.assertTagExists(tag: String) {
    assertTrue(onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty())
}
