package io.legado.app.ui.wenwen

import io.legado.app.constant.BookType
import io.legado.app.help.book.isType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WenwenBrowserBookBridgeTest {

    @Test
    fun createShelfBook_buildsPersistedBrowserBook() {
        val saved =
            WenwenBrowserBookBridge.createBook(
                article =
                    WenwenBrowserArticle(
                        url = "https://novel.example.org/chapter/1",
                        title = "第一章 山雨欲来",
                        content = "第一段正文\n\n第二段正文\n\n第三段正文",
                        sourceLabel = "novel.example.org",
                        coverUrl = "https://novel.example.org/cover.jpg",
                        searchQuery = "雪中悍刀行",
                        nextPageUrl = "https://novel.example.org/chapter/2",
                    ),
                addToShelf = true,
            )

        assertEquals("wenwentome-browser://novel.example.org/%E9%9B%AA%E4%B8%AD%E6%82%8D%E5%88%80%E8%A1%8C", saved.book.bookUrl)
        assertEquals(WENWEN_BROWSER_BOOK_ORIGIN, saved.book.origin)
        assertEquals("雪中悍刀行", saved.book.name)
        assertEquals("novel.example.org", saved.book.author)
        assertEquals("https://novel.example.org/cover.jpg", saved.book.coverUrl)
        assertEquals("第一段正文", saved.book.getDisplayIntro())
        assertFalse(saved.book.isType(BookType.notShelf))
        assertTrue(saved.book.variable.orEmpty().contains("chapter/2"))
        assertTrue(saved.book.variable.orEmpty().contains("雪中悍刀行"))
        assertEquals("https://novel.example.org/chapter/1", saved.chapter.url)
        assertEquals(saved.book.bookUrl, saved.chapter.bookUrl)
        assertEquals("第一章 山雨欲来", saved.chapter.title)
        assertEquals(0, saved.chapter.index)
    }

    @Test
    fun createTransientBook_marksNotShelf() {
        val saved =
            WenwenBrowserBookBridge.createBook(
                article =
                    WenwenBrowserArticle(
                        url = "https://novel.example.org/chapter/2",
                        title = "第二章 夜雪",
                        content = "正文内容",
                        sourceLabel = "novel.example.org",
                    ),
                addToShelf = false,
            )

        assertTrue(saved.book.isType(BookType.notShelf))
        assertEquals("正文内容", saved.content)
    }

    @Test
    fun createBook_reusesStableBookUrlForSameSearchQuery() {
        val chapterOne =
            WenwenBrowserBookBridge.createBook(
                article =
                    WenwenBrowserArticle(
                        url = "https://novel.example.org/chapter/1",
                        title = "第一章",
                        content = "正文一",
                        sourceLabel = "novel.example.org",
                        searchQuery = "雪中悍刀行",
                    ),
                addToShelf = true,
            )

        val chapterTwo =
            WenwenBrowserBookBridge.createBook(
                article =
                    WenwenBrowserArticle(
                        url = "https://novel.example.org/chapter/2",
                        title = "第二章",
                        content = "正文二",
                        sourceLabel = "novel.example.org",
                        searchQuery = "雪中悍刀行",
                    ),
                addToShelf = true,
            )

        assertEquals(chapterOne.book.bookUrl, chapterTwo.book.bookUrl)
        assertEquals("https://novel.example.org/chapter/2", chapterTwo.chapter.url)
        assertEquals("第二章", chapterTwo.chapter.title)
    }

    @Test
    fun createTocChapters_buildsCatalogPlaceholders() {
        val article =
            WenwenBrowserArticle(
                url = "https://novel.example.org/chapter/2",
                title = "第二章",
                content = "正文二",
                sourceLabel = "novel.example.org",
                searchQuery = "雪中悍刀行",
                tocEntries =
                    listOf(
                        WenwenBrowserTocEntry("第一章", "https://novel.example.org/chapter/1", 0),
                        WenwenBrowserTocEntry("第二章", "https://novel.example.org/chapter/2", 1),
                        WenwenBrowserTocEntry("第三章", "https://novel.example.org/chapter/3", 2),
                    ),
            )
        val saved = WenwenBrowserBookBridge.createBook(article, addToShelf = true)

        val tocChapters = WenwenBrowserBookBridge.createTocChapters(article, saved.book.bookUrl)

        assertEquals(3, tocChapters.size)
        assertEquals(saved.book.bookUrl, tocChapters.first().bookUrl)
        assertEquals(0, tocChapters.first().index)
        assertEquals("https://novel.example.org/chapter/3", tocChapters.last().url)
    }

    @Test
    fun articleExtractor_preservesRecognizedCatalogEntries() {
        val tocEntries =
            listOf(
                WenwenBrowserTocEntry("第一章", "https://novel.example.org/chapter/1", 0),
                WenwenBrowserTocEntry("第二章", "https://novel.example.org/chapter/2", 1),
                WenwenBrowserTocEntry("第三章", "https://novel.example.org/chapter/3", 2),
            )

        val article =
            WenwenBrowserArticleExtractor.extract(
                url = "https://novel.example.org/chapter/2",
                title = "第二章",
                rawContent = "第一段正文\n\n第二段正文\n\n第三段正文",
                trigger = WenwenBrowserOptimizeTrigger.MANUAL,
                nextPageUrl = "https://novel.example.org/chapter/3",
                tocEntries = tocEntries,
            )

        assertEquals(3, article?.tocEntries?.size)
        assertEquals("https://novel.example.org/chapter/1", article?.tocEntries?.firstOrNull()?.url)
        assertEquals("https://novel.example.org/chapter/3", article?.tocEntries?.lastOrNull()?.url)
    }

    @Test
    fun merge_appendsNewChapterForExistingBrowserBook() {
        val chapterOne =
            WenwenBrowserBookBridge.createBook(
                article =
                    WenwenBrowserArticle(
                        url = "https://novel.example.org/chapter/1",
                        title = "第一章",
                        content = "正文一",
                        sourceLabel = "novel.example.org",
                        searchQuery = "雪中悍刀行",
                    ),
                addToShelf = true,
            )
        val chapterTwo =
            WenwenBrowserBookBridge.createBook(
                article =
                    WenwenBrowserArticle(
                        url = "https://novel.example.org/chapter/2",
                        title = "第二章",
                        content = "正文二",
                        sourceLabel = "novel.example.org",
                        searchQuery = "雪中悍刀行",
                        nextPageUrl = "https://novel.example.org/chapter/3",
                    ),
                addToShelf = true,
            )

        val plan =
            WenwenBrowserBookMergePlanner.merge(
                incoming = chapterTwo,
                existingBook =
                    chapterOne.book.copy(
                        totalChapterNum = 1,
                        latestChapterTitle = chapterOne.chapter.title,
                        durChapterIndex = 0,
                        durChapterTitle = chapterOne.chapter.title,
                    ),
                existingChapters = listOf(chapterOne.chapter),
                duplicateByNameAuthor = null,
                addToShelf = true,
                minShelfOrder = -10,
            )

        assertEquals(chapterOne.book.bookUrl, plan.book.bookUrl)
        assertEquals(2, plan.book.totalChapterNum)
        assertEquals("第二章", plan.book.latestChapterTitle)
        assertEquals(1, plan.book.durChapterIndex)
        assertEquals("第二章", plan.book.durChapterTitle)
        assertEquals(1, plan.chapter.index)
        assertEquals("https://novel.example.org/chapter/2", plan.chapter.url)
        assertTrue(plan.book.variable.orEmpty().contains("chapter/3"))
    }

    @Test
    fun merge_reusesExistingChapterIndexWhenChapterAlreadyCaptured() {
        val chapterOne =
            WenwenBrowserBookBridge.createBook(
                article =
                    WenwenBrowserArticle(
                        url = "https://novel.example.org/chapter/1",
                        title = "第一章",
                        content = "正文一",
                        sourceLabel = "novel.example.org",
                        searchQuery = "雪中悍刀行",
                    ),
                addToShelf = true,
            )
        val existingChapterTwo =
            WenwenBrowserBookBridge.createBook(
                article =
                    WenwenBrowserArticle(
                        url = "https://novel.example.org/chapter/2",
                        title = "第二章 旧标题",
                        content = "正文二",
                        sourceLabel = "novel.example.org",
                        searchQuery = "雪中悍刀行",
                    ),
                addToShelf = true,
            )
        val refreshedChapterTwo =
            WenwenBrowserBookBridge.createBook(
                article =
                    WenwenBrowserArticle(
                        url = "https://novel.example.org/chapter/2",
                        title = "第二章 新标题",
                        content = "正文二新版本",
                        sourceLabel = "novel.example.org",
                        searchQuery = "雪中悍刀行",
                    ),
                addToShelf = true,
            )

        val plan =
            WenwenBrowserBookMergePlanner.merge(
                incoming = refreshedChapterTwo,
                existingBook =
                    chapterOne.book.copy(
                        totalChapterNum = 2,
                        latestChapterTitle = existingChapterTwo.chapter.title,
                        durChapterIndex = 1,
                        durChapterTitle = existingChapterTwo.chapter.title,
                    ),
                existingChapters = listOf(chapterOne.chapter, existingChapterTwo.chapter.copy(index = 1)),
                duplicateByNameAuthor = null,
                addToShelf = true,
                minShelfOrder = -10,
            )

        assertEquals(2, plan.book.totalChapterNum)
        assertEquals(1, plan.chapter.index)
        assertEquals("第二章 新标题", plan.chapter.title)
        assertEquals("https://novel.example.org/chapter/2", plan.chapter.url)
    }
}
