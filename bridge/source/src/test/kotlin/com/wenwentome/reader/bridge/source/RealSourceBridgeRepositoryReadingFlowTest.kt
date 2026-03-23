package com.wenwentome.reader.bridge.source

import com.wenwentome.reader.bridge.source.model.BookInfoRule
import com.wenwentome.reader.bridge.source.model.ContentRule
import com.wenwentome.reader.bridge.source.model.NormalizedSourceDefinition
import com.wenwentome.reader.bridge.source.model.ParsedSearchUrlTemplate
import com.wenwentome.reader.bridge.source.model.ResponseKind
import com.wenwentome.reader.bridge.source.model.RuleEngine
import com.wenwentome.reader.bridge.source.model.RuleExpression
import com.wenwentome.reader.bridge.source.model.TextCleaner
import com.wenwentome.reader.bridge.source.model.TocRule
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class RealSourceBridgeRepositoryReadingFlowTest {
    @Test
    fun fetchBookDetail_supportsJsonFields() = runTest {
        val repository = RealSourceBridgeRepository(
            sourceProvider = StaticSourceDefinitionProvider(listOf(jsonReadingSource())),
            httpClient = readingFakeClient(
                "https://json.example.com/book/1" to readingFixture("book-detail.json")
            ),
        )

        val detail = repository.fetchBookDetail("json-reading", "https://json.example.com/book/1")

        assertEquals("雪中悍刀行", detail.title)
        assertEquals("烽火戏诸侯", detail.author)
        assertEquals("江湖庙堂，一书尽览", detail.summary)
        assertEquals("https://json.example.com/covers/xz.jpg", detail.coverUrl)
        assertEquals("终章 北凉旧雪", detail.lastChapter)
    }

    @Test
    fun fetchToc_supportsJsonChapterListRules() = runTest {
        val repository = RealSourceBridgeRepository(
            sourceProvider = StaticSourceDefinitionProvider(listOf(jsonReadingSource())),
            httpClient = readingFakeClient(
                "https://json.example.com/book/1/toc" to readingFixture("chapter-list.json")
            ),
        )

        val toc = repository.fetchToc("json-reading", "https://json.example.com/book/1/toc")

        assertEquals(3, toc.size)
        assertEquals("第一章 山门", toc.first().title)
        assertEquals("https://json.example.com/chapter/1", toc.first().chapterRef)
    }

    @Test
    fun fetchChapterContent_appliesCleanerAndScriptPostProcess() = runTest {
        val repository = RealSourceBridgeRepository(
            sourceProvider = StaticSourceDefinitionProvider(listOf(scriptedContentSource())),
            httpClient = readingFakeClient(
                "https://html.example.com/chapter/1" to readingFixture("chapter-content.html")
            ),
        )

        val content = repository.fetchChapterContent("scripted-content", "https://html.example.com/chapter/1")

        assertEquals("第1章 初雪", content.title)
        assertFalse(content.content.contains("广告位"))
        assertEquals("真正正文 第一段。真正正文第二段。", content.content)
    }
}

private fun jsonReadingSource() = NormalizedSourceDefinition(
    id = "json-reading",
    name = "JSON Reading Source",
    baseUrl = "https://json.example.com",
    searchUrlTemplate = ParsedSearchUrlTemplate(
        raw = "https://json.example.com/search?title={{key}}",
        urlTemplate = "https://json.example.com/search?title={{key}}",
    ),
    bookInfoRule = BookInfoRule(
        responseKind = ResponseKind.JSON,
        name = RuleExpression(RuleEngine.JSON_PATH, "\$.data.name"),
        author = RuleExpression(RuleEngine.JSON_PATH, "\$.data.author"),
        intro = RuleExpression(RuleEngine.JSON_PATH, "\$.data.intro"),
        coverUrl = RuleExpression(RuleEngine.JSON_PATH, "\$.data.cover"),
        lastChapter = RuleExpression(RuleEngine.JSON_PATH, "\$.data.lastChapter"),
    ),
    tocRule = TocRule(
        responseKind = ResponseKind.JSON,
        chapterList = RuleExpression(RuleEngine.JSON_PATH, "\$.data[*]"),
        chapterName = RuleExpression(RuleEngine.JSON_PATH, "\$.title"),
        chapterUrl = RuleExpression(RuleEngine.JSON_PATH, "\$.url"),
    ),
)

private fun scriptedContentSource() = NormalizedSourceDefinition(
    id = "scripted-content",
    name = "Scripted Content Source",
    baseUrl = "https://html.example.com",
    contentRule = ContentRule(
        responseKind = ResponseKind.HTML,
        content = RuleExpression(
            engine = RuleEngine.HTML_CSS,
            selector = "#content",
            extractor = "text",
            cleaners = listOf(TextCleaner.RemoveRegex("广告位")),
            postScript = "result.trim().replace('\\u00a0', ' ')",
        ),
    ),
)

private fun readingFakeClient(successBodies: Map<String, String>): OkHttpClient =
    OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request()
            val body = successBodies[request.url.toString()]
            if (body == null) {
                readingFailureResponse(request)
            } else {
                readingSuccessResponse(request, body)
            }
        }
        .build()

private fun readingSuccessResponse(
    request: Request,
    body: String,
): Response =
    Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(200)
        .message("OK")
        .body(body.toResponseBody("text/plain".toMediaType()))
        .build()

private fun readingFailureResponse(request: Request): Response =
    Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(500)
        .message("Boom")
        .body("error".toResponseBody("text/plain".toMediaType()))
        .build()

private fun readingFixture(name: String): String =
    requireNotNull(RealSourceBridgeRepositoryReadingFlowTest::class.java.getResourceAsStream("/fixtures/$name")) {
        "Missing fixture $name"
    }.bufferedReader().use { it.readText() }
