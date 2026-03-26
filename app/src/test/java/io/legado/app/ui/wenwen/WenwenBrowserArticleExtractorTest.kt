package io.legado.app.ui.wenwen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class WenwenBrowserArticleExtractorTest {

    @Test
    fun extract_skipsSearchEngineResultPage() {
        val article =
            WenwenBrowserArticleExtractor.extract(
                url = "https://www.bing.com/search?q=%E9%9B%AA%E4%B8%AD",
                title = "雪中悍刀行 - 必应搜索",
                rawContent = """
                    网页
                    小说站 A
                    小说站 B
                    相关搜索
                    下一页
                """.trimIndent(),
                trigger = WenwenBrowserOptimizeTrigger.AUTO,
            )

        assertNull(article)
    }

    @Test
    fun extract_returnsNormalizedReadableArticleForAutoTrigger() {
        val article =
            WenwenBrowserArticleExtractor.extract(
                url = "https://www.example.com/book/123/chapter-15.html",
                title = "第十五章 风雪夜归人",
                rawContent = """
                    第十五章 风雪夜归人

                    北凉的雪落得很慢，像有人把整座城池按进了无声的白雾里。徐凤年站在檐下，听见长街尽头传来的马蹄声，一声一声，压着夜色往前走。

                    老黄把酒袋挂在门边，抬头看了他一眼，没有说话，只是把炉火拨得更旺。屋里暖意漫起来，和外面的风雪像隔着两重天地。

                    他知道今夜总会有人回来，也总会有人离开。江湖从来不肯等人，等人的往往只有屋檐下那一盏快要熄灭的灯。
                """.trimIndent(),
                trigger = WenwenBrowserOptimizeTrigger.AUTO,
            )

        assertNotNull(article)
        assertEquals("第十五章 风雪夜归人", article?.title)
        assertEquals("www.example.com", article?.sourceLabel)
        assertEquals(3, article?.content?.split("\n\n")?.size)
    }

    @Test
    fun extract_manualTriggerAllowsShorter正文() {
        val article =
            WenwenBrowserArticleExtractor.extract(
                url = "https://novel.example.org/read/88",
                title = "试读正文",
                rawContent = """
                    试读正文

                    这一页只有两段内容，但已经足够让用户判断这是不是自己要继续读的小说正文。

                    页面里没有太多导航杂讯，手动触发时应该允许先进入优化阅读页。
                """.trimIndent(),
                trigger = WenwenBrowserOptimizeTrigger.MANUAL,
            )

        assertNotNull(article)
        assertEquals("novel.example.org", article?.sourceLabel)
    }
}
