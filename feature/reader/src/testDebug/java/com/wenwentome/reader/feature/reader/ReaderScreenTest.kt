package com.wenwentome.reader.feature.reader

import androidx.compose.ui.semantics.SemanticsProperties
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
import com.wenwentome.reader.core.model.ReadingBookmark
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
    fun readerScreen_defaultDoesNotShowOverlayPanels() {
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

        composeTestRule.assertTagDoesNotExist("reader-mode-picker")
        composeTestRule.assertTagDoesNotExist("reader-settings-sheet")
        composeTestRule.assertTagDoesNotExist("reader-overlay-panel")
    }

    @Test
    fun readerScreen_exposesImmersiveSystemBarAndBottomMeta() {
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

        composeTestRule.assertTagExists("reader-system-bar")
        composeTestRule.assertTagExists("reader-bottom-meta")
        composeTestRule.assertTextExists("全书")
        composeTestRule.assertTextExists("章节")
    }

    @Test
    fun readerScreen_topMenuShowsReadingActions() {
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

        composeTestRule.onNodeWithTag("reader-top-menu").performClick()
        composeTestRule.assertTextExists("书籍详情")
        composeTestRule.assertTextExists("下载")
        composeTestRule.assertTextExists("换源")
    }

    @Test
    fun readerScreen_exposesSharedReaderContainerTag() {
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

        composeTestRule.assertTagExists("reader-shared-container-book-1")
    }

    @Test
    fun readerScreen_opensTocAndBookmarkDrawers() {
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

        composeTestRule.onNodeWithText("目录").performClick()
        composeTestRule.assertTagExists("reader-toc-drawer")
        composeTestRule.onNodeWithText("书签").performClick()
        composeTestRule.assertTagExists("reader-bookmark-drawer")
        composeTestRule.assertTextExists("摘录预览")
    }

    @Test
    fun readerScreen_settingsShowAdvancedTypographyControls() {
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

        composeTestRule.onNodeWithText("设置").performClick()
        composeTestRule.assertTextExists("自动适配字号")
        composeTestRule.assertTextExists("字距")
        composeTestRule.assertTextExists("段落间距")
        composeTestRule.assertTextExists("页边距")
        composeTestRule.assertTextExists("阅读背景调色盘")
        composeTestRule.assertTextExists("导入字体")
    }

    @Test
    fun readerScreen_settingsCallbacksCanBeTriggered() {
        val autoFitChanges = mutableListOf<Boolean>()
        var importedFont = false

        composeTestRule.setContent {
            ReaderScreen(
                state = sampleState(),
                onLocatorChanged = { _, _ -> },
                onReaderModeChange = {},
                onThemeChange = {},
                onFontSizeChange = {},
                onAutoFitFontSizeChange = { autoFitChanges += it },
                onLineHeightChange = {},
                onLetterSpacingChange = {},
                onParagraphSpacingChange = {},
                onSidePaddingChange = {},
                onBackgroundPaletteChange = {},
                onImportFontClick = { importedFont = true },
                onBrightnessChange = {},
                onChapterSelected = {},
                onSummarizeChapter = {},
                onExplainParagraph = {},
                onTranslateParagraph = {},
                onSpeakChapter = {},
            )
        }

        composeTestRule.onNodeWithText("设置").performClick()
        composeTestRule.onNodeWithText("已开启").performClick()
        composeTestRule.onNodeWithText("导入字体").performClick()

        assertEquals(listOf(false), autoFitChanges)
        assertTrue(importedFont)
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
        composeTestRule.onNodeWithTag("reader-progress-label").assertTextContains("0%")
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

        assertEquals(listOf(0.75f), progressChanges)
    }

    @Test
    fun readerScreen_saveProgress_usesCurrentVerticalViewportProgress() {
        val progressChanges = mutableListOf<Float>()
        val verticalState =
            sampleState(readerMode = ReaderMode.VERTICAL_SCROLL).copy(
                locator = "chapter:chapter-3#paragraph:0",
                progressPercent = 0f,
                progressLabel = "0%",
                presentation = ReaderPresentationPrefs(fontSizeSp = 28f),
                paragraphs = (1..12).map { index -> "正文第${index}段" },
                totalParagraphCount = 12,
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
    fun readerScreen_localMidWindowProgressUsesTotalParagraphCount() {
        val progressChanges = mutableListOf<Float>()
        val localWindowState =
            sampleState(readerMode = ReaderMode.SIMULATED_PAGE_TURN).copy(
                locator = "chapter:chapter-3#paragraph:60",
                progressPercent = 0f,
                progressLabel = "0%",
                presentation = ReaderPresentationPrefs(fontSizeSp = 20f),
                paragraphs = (1..60).map { index -> "正文窗口第${index}段" },
                windowStartParagraphIndex = 60,
                totalParagraphCount = 120,
            )

        composeTestRule.setContent {
            ReaderScreen(
                state = localWindowState,
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

        composeTestRule.onNodeWithTag("reader-progress-summary").assertTextContains("50%")
        composeTestRule.onNodeWithText("保存进度").performClick()

        assertEquals(1, progressChanges.size)
        assertTrue(progressChanges.single() in 0.5f..0.51f)
    }

    @Test
    fun readerScreen_verticalProgressSummaryTracksViewportPosition() {
        val verticalState =
            sampleState(readerMode = ReaderMode.VERTICAL_SCROLL).copy(
                locator = "chapter:chapter-3#paragraph:0",
                progressPercent = 0f,
                progressLabel = "0%",
                presentation = ReaderPresentationPrefs(fontSizeSp = 28f),
                paragraphs = (1..12).map { index -> "正文第${index}段" },
                totalParagraphCount = 12,
            )

        composeTestRule.setContent {
            ReaderScreen(
                state = verticalState,
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

        composeTestRule.onNodeWithTag("reader-progress-summary").assertTextContains("0%")
        composeTestRule.onNodeWithTag("reader-body").performScrollToIndex(11)
        composeTestRule.waitForIdle()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onNodeWithTag("reader-progress-summary")
                .fetchSemanticsNode()
                .config
                .getOrElse(SemanticsProperties.Text) { emptyList() }
                .joinToString(separator = "") { text -> text.text } != "0%"
        }
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
        composeTestRule.assertTextExists("章节总结")
        composeTestRule.assertTextExists("段落解释")
        composeTestRule.assertTextExists("AI 翻译")
        composeTestRule.assertTextExists("TTS 朗读")
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
            totalParagraphCount = 5,
            bookmarks = listOf(
                ReadingBookmark(
                    chapterRef = "chapter-3",
                    locator = "chapter:chapter-3#paragraph:2",
                    label = "第三章关键转折",
                )
            ),
            assistant = ReaderAssistantUiState(),
        )
}

private fun ComposeContentTestRule.assertTagExists(tag: String) {
    assertTrue(onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty())
}

private fun ComposeContentTestRule.assertTagDoesNotExist(tag: String) {
    assertTrue(onAllNodesWithTag(tag).fetchSemanticsNodes().isEmpty())
}

private fun ComposeContentTestRule.assertTextExists(text: String) {
    assertTrue(onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty())
}

private fun ComposeContentTestRule.waitUntilTextExists(text: String, timeoutMillis: Long = 5_000) {
    waitUntil(timeoutMillis = timeoutMillis) {
        onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
    }
}
