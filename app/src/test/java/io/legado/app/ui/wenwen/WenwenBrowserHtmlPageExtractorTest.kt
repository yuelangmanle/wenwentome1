package io.legado.app.ui.wenwen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class WenwenBrowserHtmlPageExtractorTest {

    @Test
    fun extract_prefersReadableContentAndNextLink() {
        val html =
            """
            <html>
              <head>
                <title>第一章 山雨欲来</title>
                <meta property="og:image" content="/cover.jpg" />
              </head>
              <body>
                <div class="header">导航</div>
                <div id="content">
                  第一段正文写得更长一些，确保提取器在手动优化模式下也会把这里认定成真实正文，而不是长度过短的噪声内容。
                  <br />
                  第二段正文继续补足篇幅，同时保留明显的章节正文语义，验证下一章链接和目录识别不会互相干扰。
                  <br />
                  第三段正文补上一点收尾，让整段内容更贴近真实小说网页的排版长度和结构。
                </div>
                <div class="page">
                  <a href="/chapter/2">下一章</a>
                </div>
                <div id="list">
                  <a href="/chapter/1">第一章 山雨欲来</a>
                  <a href="/chapter/2">第二章 夜雪</a>
                  <a href="/chapter/3">第三章 风雪中</a>
                </div>
              </body>
            </html>
            """.trimIndent()

        val article =
            WenwenBrowserHtmlPageExtractor.extract(
                url = "https://novel.example.org/chapter/1",
                html = html,
                trigger = WenwenBrowserOptimizeTrigger.MANUAL,
                searchQuery = "雪中悍刀行",
            )

        assertNotNull(article)
        assertEquals("第一章 山雨欲来", article?.title)
        assertEquals("https://novel.example.org/cover.jpg", article?.coverUrl)
        assertEquals("https://novel.example.org/chapter/2", article?.nextPageUrl)
        assertEquals("雪中悍刀行", article?.searchQuery)
        assertEquals(3, article?.tocEntries?.size)
        assertEquals("https://novel.example.org/chapter/3", article?.tocEntries?.lastOrNull()?.url)
    }

    @Test
    fun extract_usesRuleSelectorsWhenGenericContentIsNoisy() {
        val html =
            """
            <html>
              <head>
                <title>第二章 夜雪</title>
              </head>
              <body>
                <div class="layout">
                  <div class="sidebar">推荐书单 推荐书单 推荐书单 推荐书单</div>
                  <div class="read-content j_readContent">
                    夜里有雪，街巷被风吹得发白，人物站在檐下看着远处灯火，这一段正文故意写得更长一些。
                    <p>风更冷，衣角与马鬃一同被掀起，章节内容继续延展，确保规则选择器命中的正文长度足够。</p>
                    <p>刀更快，故事情绪往前推进，同时保留“下一页”链接供提取器识别。</p>
                  </div>
                </div>
                <a class="next" href="/chapter/3">下一页</a>
              </body>
            </html>
            """.trimIndent()

        val article =
            WenwenBrowserHtmlPageExtractor.extract(
                url = "https://fanqienovel.com/reader/1001",
                html = html,
                trigger = WenwenBrowserOptimizeTrigger.MANUAL,
                searchQuery = "雪中悍刀行",
            )

        assertNotNull(article)
        assertEquals("第二章 夜雪", article?.title)
        assertEquals("https://fanqienovel.com/chapter/3", article?.nextPageUrl)
    }
}
