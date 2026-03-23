package com.wenwentome.reader.bridge.source

import com.wenwentome.reader.bridge.source.model.NormalizedSourceDefinition
import com.wenwentome.reader.bridge.source.model.ParsedSearchUrlTemplate
import com.wenwentome.reader.bridge.source.model.ResponseKind
import com.wenwentome.reader.bridge.source.model.RuleEngine
import com.wenwentome.reader.bridge.source.model.RuleExpression
import com.wenwentome.reader.bridge.source.model.SearchRule
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RealSourceBridgeRepositorySearchTest {
    @Test
    fun search_supportsHtmlRules() = runTest {
        val repository = RealSourceBridgeRepository(
            sourceProvider = StaticSourceDefinitionProvider(listOf(htmlSource())),
            httpClient = fakeClient(
                "https://html.example.com/search?title=%E6%B5%8B%E8%AF%95" to searchFixture("search-result.html")
            ),
        )

        val results = repository.search(query = "测试", sourceIds = listOf("html-demo"))

        assertEquals(1, results.size)
        assertEquals("测试书名", results.first().title)
        assertEquals("测试作者", results.first().author)
        assertEquals("https://html.example.com/book/1", results.first().detailUrl)
        assertEquals("https://html.example.com/cover/1.jpg", results.first().coverUrl)
        assertEquals("这里是第一本简介", results.first().intro)
    }

    @Test
    fun search_supportsJsonBookListRules() = runTest {
        val repository = RealSourceBridgeRepository(
            sourceProvider = StaticSourceDefinitionProvider(listOf(jsonSource())),
            httpClient = fakeClient(
                "https://json.example.com/search?title=%E5%B0%86%E5%A4%9C" to searchFixture("search-result-json-list.json")
            ),
        )

        val results = repository.search(query = "将夜", sourceIds = listOf("json-demo"))

        assertEquals(2, results.size)
        assertEquals("将夜", results.first().title)
        assertEquals("猫腻", results.first().author)
        assertEquals("https://json.example.com/book/jiangye", results.first().detailUrl)
        assertEquals("https://json.example.com/cover/jiangye.jpg", results.first().coverUrl)
        assertEquals("昊天之下，众生皆苦", results.first().intro)
    }

    @Test
    fun search_skipsSingleSourceFailureButKeepsOtherSources() = runTest {
        val repository = RealSourceBridgeRepository(
            sourceProvider = StaticSourceDefinitionProvider(listOf(brokenSource(), htmlSource())),
            httpClient = fakeClient(
                "https://html.example.com/search?title=%E5%87%A1%E4%BA%BA" to searchFixture("search-result.html")
            ),
        )

        val results = repository.search(query = "凡人", sourceIds = listOf("broken-source", "html-demo"))

        assertEquals(1, results.size)
        assertTrue(results.any { it.sourceId == "html-demo" })
    }
}

private fun htmlSource() = NormalizedSourceDefinition(
    id = "html-demo",
    name = "HTML Source",
    baseUrl = "https://html.example.com",
    searchUrlTemplate = ParsedSearchUrlTemplate(
        raw = "https://html.example.com/search?title={{key}}",
        urlTemplate = "https://html.example.com/search?title={{key}}",
    ),
    searchRule = SearchRule(
        responseKind = ResponseKind.HTML,
        bookList = RuleExpression(RuleEngine.HTML_CSS, ".result-item"),
        name = RuleExpression(RuleEngine.HTML_CSS, "h3", "text"),
        bookUrl = RuleExpression(RuleEngine.HTML_CSS, "a", "href"),
        author = RuleExpression(RuleEngine.HTML_CSS, ".author", "text"),
        coverUrl = RuleExpression(RuleEngine.HTML_CSS, "img", "src"),
        intro = RuleExpression(RuleEngine.HTML_CSS, ".intro", "text"),
    ),
)

private fun jsonSource() = NormalizedSourceDefinition(
    id = "json-demo",
    name = "JSON Source",
    baseUrl = "https://json.example.com",
    searchUrlTemplate = ParsedSearchUrlTemplate(
        raw = "https://json.example.com/search?title={{key}}",
        urlTemplate = "https://json.example.com/search?title={{key}}",
    ),
    searchRule = SearchRule(
        responseKind = ResponseKind.JSON,
        bookList = RuleExpression(RuleEngine.JSON_PATH, "\$.data[*]"),
        name = RuleExpression(RuleEngine.JSON_PATH, "\$.name"),
        bookUrl = RuleExpression(RuleEngine.JSON_PATH, "\$.url"),
        author = RuleExpression(RuleEngine.JSON_PATH, "\$.author"),
        coverUrl = RuleExpression(RuleEngine.JSON_PATH, "\$.cover"),
        intro = RuleExpression(RuleEngine.JSON_PATH, "\$.intro"),
    ),
)

private fun brokenSource() = NormalizedSourceDefinition(
    id = "broken-source",
    name = "Broken Source",
    baseUrl = "https://broken.example.com",
    searchUrlTemplate = ParsedSearchUrlTemplate(
        raw = "https://broken.example.com/search?title={{key}}",
        urlTemplate = "https://broken.example.com/search?title={{key}}",
    ),
    searchRule = SearchRule(
        responseKind = ResponseKind.HTML,
        bookList = RuleExpression(RuleEngine.HTML_CSS, ".result-item"),
        name = RuleExpression(RuleEngine.HTML_CSS, "h3", "text"),
        bookUrl = RuleExpression(RuleEngine.HTML_CSS, "a", "href"),
    ),
)

private fun fakeClient(successBodies: Map<String, String>): OkHttpClient =
    OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request()
            val body = successBodies[request.url.toString()]
            if (body == null) {
                failureResponse(request)
            } else {
                successResponse(request, body)
            }
        }
        .build()

private fun successResponse(
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

private fun failureResponse(request: Request): Response =
    Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(500)
        .message("Boom")
        .body("error".toResponseBody("text/plain".toMediaType()))
        .build()

private fun searchFixture(name: String): String =
    requireNotNull(RealSourceBridgeRepositorySearchTest::class.java.getResourceAsStream("/fixtures/$name")) {
        "Missing fixture $name"
    }.bufferedReader().use { it.readText() }
