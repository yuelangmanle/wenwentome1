package com.wenwentome.reader.feature.reader

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.wenwentome.reader.core.model.BookFormat
import com.wenwentome.reader.core.model.BookRecord
import com.wenwentome.reader.core.model.OriginType
import com.wenwentome.reader.core.model.ReaderMode
import com.wenwentome.reader.core.model.ReaderPresentationPrefs
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReaderScreenRouteTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun readerScreenRoute_routesTopMenuActionsWithCurrentBookId() {
        val uiState = MutableStateFlow(sampleReaderRouteState())
        var detailBookId: String? = null
        var downloadBookId: String? = null
        var switchSourceBookId: String? = null

        composeTestRule.setContent {
            ReaderScreenRoute(
                uiStateFlow = uiState,
                onPersistLocator = { _, _, _ -> },
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
                onOpenBookDetail = { detailBookId = it },
                onOpenDownloadSheet = { downloadBookId = it },
                onOpenSourceSwitcher = { switchSourceBookId = it },
            )
        }

        composeTestRule.onNodeWithTag("reader-top-menu").performClick()
        composeTestRule.onNodeWithText("书籍详情").performClick()
        composeTestRule.onNodeWithTag("reader-top-menu").performClick()
        composeTestRule.onNodeWithText("下载").performClick()
        composeTestRule.onNodeWithTag("reader-top-menu").performClick()
        composeTestRule.onNodeWithText("换源").performClick()

        assertEquals("book-1", detailBookId)
        assertEquals("book-1", downloadBookId)
        assertEquals("book-1", switchSourceBookId)
    }
}

private fun sampleReaderRouteState() =
    ReaderUiState(
        book = BookRecord(
            id = "book-1",
            title = "测试书",
            author = "测试作者",
            originType = OriginType.LOCAL,
            primaryFormat = BookFormat.EPUB,
        ),
        readerMode = ReaderMode.SIMULATED_PAGE_TURN,
        presentation = ReaderPresentationPrefs(),
        chapterTitle = "第一章",
        chapterRef = "chapter-1",
        locator = "chapter:chapter-1#paragraph:0",
        progressPercent = 0.2f,
        progressLabel = "20%",
        paragraphs = listOf("第一段正文", "第二段正文"),
    )
