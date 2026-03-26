package com.wenwentome.reader.feature.reader

import androidx.compose.ui.test.junit4.createComposeRule
import com.wenwentome.reader.core.model.BookFormat
import com.wenwentome.reader.core.model.BookRecord
import com.wenwentome.reader.core.model.OriginType
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BookDetailRouteTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun bookDetailRoute_collectsOpenReaderEvents() {
        val uiState = MutableStateFlow(sampleBookDetailRouteState())
        val events = MutableSharedFlow<BookDetailEvent>(extraBufferCapacity = 1)
        var openedBookId: String? = null

        composeTestRule.setContent {
            BookDetailRoute(
                uiStateFlow = uiState,
                events = events,
                onReadClick = {},
                onChapterClick = {},
                onRefreshCatalogClick = {},
                onJumpToLatestClick = {},
                onRefreshCoverClick = {},
                onImportPhotoClick = {},
                onRestoreAutomaticCoverClick = {},
                onEnhanceMetadataClick = {},
                onApplyMetadataClick = {},
                onOpenReader = { openedBookId = it },
            )
        }

        composeTestRule.runOnIdle {
            events.tryEmit(BookDetailEvent.OpenReader(bookId = "book-1"))
        }
        composeTestRule.waitForIdle()

        assertEquals("book-1", openedBookId)
    }
}

private fun sampleBookDetailRouteState() =
    BookDetailUiState(
        book = BookRecord(
            id = "book-1",
            title = "测试书",
            author = "测试作者",
            originType = OriginType.WEB,
            primaryFormat = BookFormat.WEB,
            summary = "测试简介",
        ),
        readActionLabel = "继续阅读",
        progressPercent = 0.3f,
        progressLabel = "30%",
    )
