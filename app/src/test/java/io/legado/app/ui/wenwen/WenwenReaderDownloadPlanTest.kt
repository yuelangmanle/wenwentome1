package io.legado.app.ui.wenwen

import io.legado.app.constant.BookType
import io.legado.app.data.entities.Book
import io.legado.app.utils.GSON
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WenwenReaderDownloadPlanTest {

    @Test
    fun resolveDownloadPlan_usesKnownChapterRangeForWebBook() {
        val book =
            Book(
                bookUrl = "https://source.example/book/1",
                origin = "https://source.example",
                originName = "示例书源",
                type = BookType.text,
            )

        val plan =
            resolveDownloadPlan(
                book = book,
                currentChapterIndex = 12,
                chapterCount = 40,
                option = DownloadOption.Next50,
            )

        assertEquals(12, plan.startIndex)
        assertEquals(39, plan.endIndex)
        assertNull(plan.message)
    }

    @Test
    fun resolveDownloadPlan_downloadsForwardForBrowserBook() {
        val book =
            Book(
                bookUrl = "wenwentome-browser://novel.example.org/demo",
                origin = WENWEN_BROWSER_BOOK_ORIGIN,
                originName = "novel.example.org",
                type = BookType.text,
            )

        val nextPlan =
            resolveDownloadPlan(
                book = book,
                currentChapterIndex = 3,
                chapterCount = 4,
                option = DownloadOption.Next50,
            )
        val fullPlan =
            resolveDownloadPlan(
                book = book,
                currentChapterIndex = 3,
                chapterCount = 4,
                option = DownloadOption.FullBook,
            )

        assertEquals(4, nextPlan.startIndex)
        assertEquals(53, nextPlan.endIndex)
        assertNull(nextPlan.message)

        assertEquals(4, fullPlan.startIndex)
        assertEquals(-1, fullPlan.endIndex)
        assertEquals("浏览器找书暂按当前进度向后缓存到结尾处理", fullPlan.message)
    }

    @Test
    fun resolveDownloadPlan_usesRecognizedCatalogForBrowserFullBook() {
        val book =
            Book(
                bookUrl = "wenwentome-browser://novel.example.org/demo",
                origin = WENWEN_BROWSER_BOOK_ORIGIN,
                originName = "novel.example.org",
                type = BookType.text,
                variable =
                    GSON.toJson(
                        WenwenBrowserBookMetadata(
                            originalUrl = "https://novel.example.org/catalog",
                            sourceLabel = "novel.example.org",
                            tocEntries =
                                listOf(
                                    WenwenBrowserTocEntry("第一章", "https://novel.example.org/1", 0),
                                    WenwenBrowserTocEntry("第二章", "https://novel.example.org/2", 1),
                                    WenwenBrowserTocEntry("第三章", "https://novel.example.org/3", 2),
                                    WenwenBrowserTocEntry("第四章", "https://novel.example.org/4", 3),
                                ),
                        )
                    ),
            )

        val fullPlan =
            resolveDownloadPlan(
                book = book,
                currentChapterIndex = 2,
                chapterCount = 1,
                option = DownloadOption.FullBook,
            )

        assertEquals(0, fullPlan.startIndex)
        assertEquals(3, fullPlan.endIndex)
        assertEquals("浏览器找书已按识别目录整本缓存", fullPlan.message)
    }
}
