package com.wenwentome.reader.bridge.source

import com.wenwentome.reader.bridge.source.model.RuleEngine
import com.wenwentome.reader.bridge.source.model.RuleExpression
import com.wenwentome.reader.bridge.source.model.RuleTarget
import com.wenwentome.reader.bridge.source.model.TextCleaner
import com.wenwentome.reader.bridge.source.model.UnsupportedRuleKind
import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class RuleExecutionEngineTest {
    private val engine = RuleExecutionEngine()

    @Test
    fun executeValue_runsExtractionCleanerScriptAndNormalizationInOrder() {
        val document = Jsoup.parse(
            """
            <html>
              <body>
                <div class="content"> 广告 正文&nbsp;第一段 </div>
              </body>
            </html>
            """.trimIndent()
        )

        val outcome = engine.extractValue(
            target = RuleTarget.Html(document),
            expression = RuleExpression(
                engine = RuleEngine.HTML_CSS,
                selector = ".content",
                extractor = "text",
                cleaners = listOf(TextCleaner.RemoveRegex("广告")),
                postScript = "result.trim().replace('\\u00a0', ' ')",
            ),
            context = RuleExecutionContext(
                baseUrl = "https://demo.test",
                pageUrl = "https://demo.test/chapter/1",
            ),
        )

        assertEquals("正文 第一段", outcome.value)
        assertTrue(outcome.diagnostics.isEmpty())
    }

    @Test
    fun executeValue_supportsXPathExtraction() {
        val html = fixture("search-result.html")

        val outcome = engine.extractValue(
            target = RuleTarget.Html(Jsoup.parse(html)),
            expression = RuleExpression(
                engine = RuleEngine.HTML_XPATH,
                selector = "//div[@class='result-item']/a/h3",
                extractor = "text",
            ),
            context = RuleExecutionContext(
                baseUrl = "https://demo.test",
                pageUrl = "https://demo.test/search",
            ),
        )

        assertEquals("测试书名", outcome.value)
    }

    @Test
    fun executeValue_supportsJsonPathExtractionFromListItem() {
        val listOutcome = engine.extractList(
            target = RuleTarget.Json(fixture("search-result.json")),
            expression = RuleExpression(
                engine = RuleEngine.JSON_PATH,
                selector = "\$.data[*]",
            ),
            context = RuleExecutionContext(
                baseUrl = "https://demo.test",
                pageUrl = "https://demo.test/search",
            ),
        )

        assertEquals(2, listOutcome.value.size)

        val valueOutcome = engine.extractValue(
            target = listOutcome.value.first(),
            expression = RuleExpression(
                engine = RuleEngine.JSON_PATH,
                selector = "\$.name",
            ),
            context = RuleExecutionContext(
                baseUrl = "https://demo.test",
                pageUrl = "https://demo.test/search",
            ),
        )

        assertEquals("测试书名", valueOutcome.value)
    }

    @Test
    fun executeValue_fallsBackToCleanedResultWhenScriptFails() {
        val document = Jsoup.parse(
            """
            <html>
              <body>
                <div class="content">广告正文</div>
              </body>
            </html>
            """.trimIndent()
        )

        val outcome = engine.extractValue(
            target = RuleTarget.Html(document),
            expression = RuleExpression(
                engine = RuleEngine.HTML_CSS,
                selector = ".content",
                extractor = "text",
                cleaners = listOf(TextCleaner.RemoveRegex("广告")),
                postScript = "result.notAFunction()",
            ),
            context = RuleExecutionContext(
                baseUrl = "https://demo.test",
                pageUrl = "https://demo.test/chapter/1",
            ),
        )

        assertEquals("正文", outcome.value)
        assertEquals(SourceBridgeErrorCode.SCRIPT_POST_PROCESS_FAILED, outcome.diagnostics.single().code)
    }

    @Test
    fun executeValue_fallsBackWhenScriptReturnsUndefined() {
        val document = Jsoup.parse(
            """
            <html>
              <body>
                <div class="content">正文</div>
              </body>
            </html>
            """.trimIndent()
        )

        val outcome = engine.extractValue(
            target = RuleTarget.Html(document),
            expression = RuleExpression(
                engine = RuleEngine.HTML_CSS,
                selector = ".content",
                extractor = "text",
                postScript = "undefined",
            ),
            context = RuleExecutionContext(
                baseUrl = "https://demo.test",
                pageUrl = "https://demo.test/chapter/1",
            ),
        )

        assertEquals("正文", outcome.value)
        assertTrue(outcome.diagnostics.isEmpty())
    }

    @Test
    fun executeList_rejectsJsTemplateUnsupportedKind() {
        val error = try {
            engine.assertSupported(UnsupportedRuleKind.JS_TEMPLATE)
            fail("Expected unsupported rule exception")
        } catch (exception: SourceBridgeException) {
            exception
        }

        assertEquals(SourceBridgeErrorCode.UNSUPPORTED_RULE_KIND, error.code)
    }

    @Test
    fun executeList_returnsHtmlElementsForCssSelector() {
        val outcome = engine.extractList(
            target = RuleTarget.Html(Jsoup.parse(fixture("search-result.html"))),
            expression = RuleExpression(
                engine = RuleEngine.HTML_CSS,
                selector = ".result-item",
            ),
            context = RuleExecutionContext(
                baseUrl = "https://demo.test",
                pageUrl = "https://demo.test/search",
            ),
        )

        assertEquals(1, outcome.value.size)
        assertTrue(outcome.value.first() is RuleTarget.Html)
        assertFalse(outcome.value.isEmpty())
    }
}

private fun fixture(name: String): String =
    requireNotNull(RuleExecutionEngineTest::class.java.getResourceAsStream("/fixtures/$name")) {
        "Missing fixture $name"
    }.bufferedReader().use { it.readText() }
