package com.wenwentome.reader.feature.reader

import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.wenwentome.reader.core.model.BookFormat
import com.wenwentome.reader.core.model.BookRecord
import com.wenwentome.reader.core.model.OriginType
import com.wenwentome.reader.core.model.ReaderChapter
import com.wenwentome.reader.core.model.ReaderMode
import com.wenwentome.reader.core.model.ReaderPresentationPrefs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReaderScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun readerScreen_opensModePickerAndSettingsControls() {
        composeTestRule.setContent {
            ReaderScreen(
                state = sampleState(),
                onLocatorChanged = { _, _ -> },
                onReaderModeChange = {},
                onThemeChange = {},
                onFontSizeChange = {},
                onLineHeightChange = {},
                onBrightnessChange = {},
                onChapterSelected = {},
            )
        }

        composeTestRule.onNodeWithText("模式").performClick()
        composeTestRule.assertTagExists("reader-mode-picker")
        composeTestRule.onNodeWithText("设置").performClick()
        composeTestRule.assertTagExists("reader-settings-sheet")
        composeTestRule.assertTextExists("字体大小")
    }

    @Test
    fun readerScreen_rendersDedicatedContainersForEveryReaderMode() {
        composeTestRule.setContent {
            ReaderScreen(
                state = sampleState(readerMode = ReaderMode.SIMULATED_PAGE_TURN),
                onLocatorChanged = { _, _ -> },
                onReaderModeChange = {},
                onThemeChange = {},
                onFontSizeChange = {},
                onLineHeightChange = {},
                onBrightnessChange = {},
                onChapterSelected = {},
            )
        }

        composeTestRule.assertTagExists("reader-simulated-pager")
        composeTestRule.onNodeWithTag("reader-page-indicator").assertTextContains("/")

        composeTestRule.setContent {
            ReaderScreen(
                state = sampleState(readerMode = ReaderMode.HORIZONTAL_PAGING),
                onLocatorChanged = { _, _ -> },
                onReaderModeChange = {},
                onThemeChange = {},
                onFontSizeChange = {},
                onLineHeightChange = {},
                onBrightnessChange = {},
                onChapterSelected = {},
            )
        }

        composeTestRule.assertTagExists("reader-horizontal-pager")

        composeTestRule.setContent {
            ReaderScreen(
                state = sampleState(readerMode = ReaderMode.VERTICAL_SCROLL),
                onLocatorChanged = { _, _ -> },
                onReaderModeChange = {},
                onThemeChange = {},
                onFontSizeChange = {},
                onLineHeightChange = {},
                onBrightnessChange = {},
                onChapterSelected = {},
            )
        }

        composeTestRule.assertTagExists("reader-vertical-scroll")
    }

    @Test
    fun tocSheet_highlightsCurrentChapter_andLatestChapter_withoutResettingLocatorUntilSelect() {
        val locatorChanges = mutableListOf<String>()

        composeTestRule.setContent {
            ReaderScreen(
                state = sampleState(),
                onLocatorChanged = { locator, _ -> locatorChanges += locator },
                onReaderModeChange = {},
                onThemeChange = {},
                onFontSizeChange = {},
                onLineHeightChange = {},
                onBrightnessChange = {},
                onChapterSelected = {},
            )
        }

        composeTestRule.onNodeWithText("目录").performClick()
        composeTestRule.assertTagExists("toc-current-chapter")
        composeTestRule.assertTagExists("toc-latest-chapter")
        composeTestRule.onNodeWithTag("reader-progress-label").assertTextContains("50%")
        assertEquals(emptyList<String>(), locatorChanges)
    }

    private fun sampleState(
        readerMode: ReaderMode = ReaderMode.SIMULATED_PAGE_TURN,
    ) =
        ReaderUiState(
            book = BookRecord(
                id = "book-1",
                title = "测试书",
                author = "测试作者",
                originType = OriginType.LOCAL,
                primaryFormat = BookFormat.EPUB,
            ),
            readerMode = readerMode,
            presentation = ReaderPresentationPrefs(),
            chapters = listOf(
                ReaderChapter(
                    chapterRef = "chapter-3",
                    title = "第三章",
                    orderIndex = 0,
                    sourceType = BookFormat.EPUB,
                    locatorHint = "chapter:chapter-3#paragraph:0",
                ),
                ReaderChapter(
                    chapterRef = "chapter-8",
                    title = "第八章",
                    orderIndex = 1,
                    sourceType = BookFormat.EPUB,
                    locatorHint = "chapter:chapter-8#paragraph:0",
                    isLatest = true,
                ),
            ),
            chapterRef = "chapter-3",
            latestChapterRef = "chapter-8",
            tocHighlightedChapterRef = "chapter-3",
            locator = "chapter:chapter-3#paragraph:2",
            progressPercent = 0.5f,
            progressLabel = "50%",
            chapterTitle = "第三章",
            paragraphs = listOf(
                "正文第一段",
                "正文第二段",
                "正文第三段",
                "正文第四段",
                "正文第五段",
            ),
        )
}

private fun ComposeContentTestRule.assertTagExists(tag: String) {
    assertTrue(onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty())
}

private fun ComposeContentTestRule.assertTextExists(text: String) {
    assertTrue(onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty())
}
