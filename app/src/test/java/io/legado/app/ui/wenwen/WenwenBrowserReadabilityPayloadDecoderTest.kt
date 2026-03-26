package io.legado.app.ui.wenwen

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WenwenBrowserReadabilityPayloadDecoderTest {

    @Test
    fun decode_returnsPayloadFromEvaluateJavascriptResult() {
        val raw =
            JSONObject.quote(
                """
                {
                  "title":"雪中悍刀行",
                  "textContent":"第一段正文\n\n第二段正文",
                  "content":"<article><p>第一段正文</p></article>",
                  "excerpt":"第一段正文",
                  "byline":"烽火戏诸侯",
                  "siteName":"novel.example.org",
                  "coverUrl":"https://novel.example.org/cover.jpg",
                  "nextPageUrl":"https://novel.example.org/chapter/2",
                  "tocEntries":[
                    {"title":"第一章 山雨欲来","url":"https://novel.example.org/chapter/1","orderIndex":0},
                    {"title":"第二章 夜雪","url":"https://novel.example.org/chapter/2","orderIndex":1}
                  ]
                }
                """.trimIndent()
            )

        val payload = WenwenBrowserReadabilityPayloadDecoder.decode(raw)

        assertEquals("雪中悍刀行", payload?.title)
        assertEquals("第一段正文\n\n第二段正文", payload?.textContent)
        assertEquals("<article><p>第一段正文</p></article>", payload?.contentHtml)
        assertEquals("第一段正文", payload?.excerpt)
        assertEquals("烽火戏诸侯", payload?.byline)
        assertEquals("novel.example.org", payload?.siteName)
        assertEquals("https://novel.example.org/cover.jpg", payload?.coverUrl)
        assertEquals("https://novel.example.org/chapter/2", payload?.nextPageUrl)
        assertEquals(2, payload?.tocEntries?.size)
        assertEquals("第一章 山雨欲来", payload?.tocEntries?.firstOrNull()?.title)
    }

    @Test
    fun decode_returnsNullForInvalidPayload() {
        assertNull(WenwenBrowserReadabilityPayloadDecoder.decode(null))
        assertNull(WenwenBrowserReadabilityPayloadDecoder.decode("null"))
        assertNull(WenwenBrowserReadabilityPayloadDecoder.decode("\"not json\""))
    }
}
