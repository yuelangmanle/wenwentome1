package io.legado.app.ui.wenwen

import org.junit.Assert.assertEquals
import org.junit.Test

class WenwenReaderProgressTest {

    @Test
    fun calculateChapterProgress_returnsZeroWithoutContent() {
        assertEquals(0f, calculateChapterProgress(hasContent = false, scrollValue = 120, maxScrollValue = 600), 0f)
    }

    @Test
    fun calculateChapterProgress_returnsCompleteForShortChapter() {
        assertEquals(1f, calculateChapterProgress(hasContent = true, scrollValue = 0, maxScrollValue = 0), 0f)
    }

    @Test
    fun calculateBookProgress_includesCurrentChapterScrollRatio() {
        assertEquals(0.425f, calculateBookProgress(chapterIndex = 4, chapterCount = 10, chapterProgress = 0.25f), 0.0001f)
    }

    @Test
    fun calculateBookProgress_clampsInvalidInputs() {
        assertEquals(0f, calculateBookProgress(chapterIndex = 3, chapterCount = 0, chapterProgress = 0.8f), 0f)
        assertEquals(1f, calculateBookProgress(chapterIndex = 99, chapterCount = 10, chapterProgress = 1.2f), 0f)
    }
}
