package com.wenwentome.reader.feature.discover

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.wenwentome.reader.bridge.source.model.RemoteBookDetail
import com.wenwentome.reader.bridge.source.model.RemoteSearchResult
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DiscoverScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun previewAddButtonInvokesCallbackWithSelectedResult() {
        val selected = sampleResult()
        var added: RemoteSearchResult? = null

        composeTestRule.setContent {
            DiscoverScreen(
                state = DiscoverUiState(
                    draftQuery = "雪中",
                    query = "雪中",
                    results = listOf(selected),
                    selectedResultId = selected.id,
                    selectedResult = selected,
                    selectedPreview = RemoteBookDetail(
                        title = selected.title,
                        author = selected.author,
                        summary = "用于验证预览卡加入书库按钮回调。",
                        lastChapter = "最新章",
                    ),
                ),
                onQueryChange = {},
                onSubmitSearch = {},
                onPreview = {},
                onAddToShelf = { added = it },
                onRefreshSelected = {},
                onReadLatest = {},
                onManageSources = {},
            )
        }

        composeTestRule.onNodeWithTag("discover-preview-add-button").performClick()

        assertEquals(selected.result, added)
    }

    @Test
    fun discoverScreen_showsSourceHealthBadge() {
        val selected = sampleResult()

        composeTestRule.setContent {
            DiscoverScreen(
                state = DiscoverUiState(
                    draftQuery = "雪中",
                    query = "雪中",
                    results = listOf(selected),
                    selectedResultId = selected.id,
                    selectedResult = selected,
                    selectedPreview = RemoteBookDetail(
                        title = selected.title,
                        author = selected.author,
                    ),
                ),
                onQueryChange = {},
                onSubmitSearch = {},
                onPreview = {},
                onAddToShelf = {},
                onRefreshSelected = {},
                onReadLatest = {},
                onManageSources = {},
            )
        }

        composeTestRule.onNodeWithTag("source-health-badge-preview").fetchSemanticsNode()
    }

    @Test
    fun discoverScreen_searchButtonInvokesSubmitCallback() {
        var submitCount = 0

        composeTestRule.setContent {
            DiscoverScreen(
                state = DiscoverUiState(draftQuery = "雪中"),
                onQueryChange = {},
                onSubmitSearch = { submitCount++ },
                onPreview = {},
                onAddToShelf = {},
                onRefreshSelected = {},
                onReadLatest = {},
                onManageSources = {},
            )
        }

        composeTestRule.onNodeWithTag("discover-search-submit").performClick()

        assertEquals(1, submitCount)
    }
}

private fun sampleResult() =
    DiscoverSearchResult(
        result = RemoteSearchResult(
            id = "remote-1",
            sourceId = "source-1",
            title = "雪中悍刀行",
            author = "烽火戏诸侯",
            detailUrl = "https://example.com/book/1",
        ),
        healthScore = 0.92f,
        healthLabel = "优秀",
    )
