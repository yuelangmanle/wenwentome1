package com.wenwentome.reader.feature.reader

import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import com.wenwentome.reader.core.model.BookFormat
import com.wenwentome.reader.core.model.BookRecord
import com.wenwentome.reader.core.model.OriginType
import com.wenwentome.reader.core.model.ReaderChapter
import com.wenwentome.reader.core.model.ReaderMode
import com.wenwentome.reader.core.model.ReaderPresentationPrefs
import com.wenwentome.reader.core.model.buildReaderParagraphLocator
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
                onSummarizeChapter = {},
                onExplainParagraph = {},
                onTranslateParagraph = {},
                onSpeakChapter = {},
            )
        }

        composeTestRule.onNodeWithText("模式").performClick()
        composeTestRule.assertTagExists("reader-mode-picker")
        composeTestRule.onNodeWithText("设置").performClick()
        composeTestRule.assertTagExists("reader-settings-sheet")
        composeTestRule.assertTextExists("字体大小")
    }

    @Test
    fun readerScreen_rendersSimulatedPagerContainer() {
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
                onSummarizeChapter = {},
                onExplainParagraph = {},
                onTranslateParagraph = {},
                onSpeakChapter = {},
            )
        }

        composeTestRule.assertTagExists("reader-simulated-pager")
    }

    @Test
    fun readerScreen_rendersHorizontalPagerContainer() {
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
                onSummarizeChapter = {},
                onExplainParagraph = {},
                onTranslateParagraph = {},
                onSpeakChapter = {},
            )
        }

        composeTestRule.assertTagExists("reader-horizontal-pager")
    }

    @Test
    fun readerScreen_rendersVerticalScrollContainer() {
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
                onSummarizeChapter = {},
                onExplainParagraph = {},
                onTranslateParagraph = {},
                onSpeakChapter = {},
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
                onSummarizeChapter = {},
                onExplainParagraph = {},
                onTranslateParagraph = {},
                onSpeakChapter = {},
            )
        }

        composeTestRule.onNodeWithText("目录").performClick()
        composeTestRule.assertTagExists("toc-current-chapter")
        composeTestRule.assertTagExists("toc-latest-chapter")
        composeTestRule.onNodeWithTag("reader-progress-label").assertTextContains("50%")
        assertEquals(emptyList<String>(), locatorChanges)
    }

    @Test
    fun readerScreen_saveProgress_usesCurrentPagedViewportProgress() {
        val progressChanges = mutableListOf<Float>()

        composeTestRule.setContent {
            ReaderScreen(
                state = sampleState(readerMode = ReaderMode.SIMULATED_PAGE_TURN),
                onLocatorChanged = { _, progress -> progressChanges += progress },
                onReaderModeChange = {},
                onThemeChange = {},
                onFontSizeChange = {},
                onLineHeightChange = {},
                onBrightnessChange = {},
                onChapterSelected = {},
                onSummarizeChapter = {},
                onExplainParagraph = {},
                onTranslateParagraph = {},
                onSpeakChapter = {},
            )
        }

        composeTestRule.onNodeWithText("下一页").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("保存进度").performClick()

        assertEquals(listOf(1f), progressChanges)
    }

    @Test
    fun readerScreen_saveProgress_usesCurrentVerticalViewportProgress() {
        val progressChanges = mutableListOf<Float>()
        val verticalState =
            sampleState(readerMode = ReaderMode.VERTICAL_SCROLL).copy(
                locator = "chapter:chapter-3#paragraph:0",
                progressPercent = 0f,
                progressLabel = "0%",
                presentation = ReaderPresentationPrefs(fontSizeSp = 28),
                paragraphs = (1..12).map { index -> "正文第${index}段" },
            )

        composeTestRule.setContent {
            ReaderScreen(
                state = verticalState,
                onLocatorChanged = { _, progress -> progressChanges += progress },
                onReaderModeChange = {},
                onThemeChange = {},
                onFontSizeChange = {},
                onLineHeightChange = {},
                onBrightnessChange = {},
                onChapterSelected = {},
                onSummarizeChapter = {},
                onExplainParagraph = {},
                onTranslateParagraph = {},
                onSpeakChapter = {},
            )
        }

        composeTestRule.onNodeWithTag("reader-body").performScrollToIndex(11)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("保存进度").performClick()

        assertEquals(1, progressChanges.size)
        assertTrue(progressChanges.single() > 0f)
    }

    @Test
    fun readerScreen_webLocatorRestoresHorizontalPageFromParagraphIndex() {
        val chapterRef = "https://example.com/chapter-9"
        val webState =
            sampleState(readerMode = ReaderMode.HORIZONTAL_PAGING).copy(
                book = BookRecord(
                    id = "book-web",
                    title = "测试网文",
                    author = "测试作者",
                    originType = OriginType.WEB,
                    primaryFormat = BookFormat.WEB,
                ),
                chapterRef = chapterRef,
                locator = buildReaderParagraphLocator(BookFormat.WEB, chapterRef, 4),
                chapterTitle = "第九章",
                paragraphs = (1..8).map { index -> "正文第${index}段" },
            )

        composeTestRule.setContent {
            ReaderScreen(
                state = webState,
                onLocatorChanged = { _, _ -> },
                onReaderModeChange = {},
                onThemeChange = {},
                onFontSizeChange = {},
                onLineHeightChange = {},
                onBrightnessChange = {},
                onChapterSelected = {},
                onSummarizeChapter = {},
                onExplainParagraph = {},
                onTranslateParagraph = {},
                onSpeakChapter = {},
            )
        }

        composeTestRule.waitUntilTextExists("第 2 / 2 页")
    }

    @Test
    fun readerScreen_showsAssistantSheetActions() {
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
                onSummarizeChapter = {},
                onExplainParagraph = {},
                onTranslateParagraph = {},
                onSpeakChapter = {},
            )
        }

        composeTestRule.onNodeWithText("AI").performClick()
        composeTestRule.onNodeWithText("章节总结").assertExists()
        composeTestRule.onNodeWithText("段落解释").assertExists()
        composeTestRule.onNodeWithText("AI 翻译").assertExists()
        composeTestRule.onNodeWithText("TTS 朗读").assertExists()
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
            assistant = ReaderAssistantUiState(),
        )
}

private fun ComposeContentTestRule.assertTagExists(tag: String) {
    assertTrue(onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty())
}

private fun ComposeContentTestRule.assertTextExists(text: String) {
    assertTrue(onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty())
}

private fun ComposeContentTestRule.waitUntilTextExists(text: String, timeoutMillis: Long = 5_000) {
    waitUntil(timeoutMillis = timeoutMillis) {
        onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
    }
}
