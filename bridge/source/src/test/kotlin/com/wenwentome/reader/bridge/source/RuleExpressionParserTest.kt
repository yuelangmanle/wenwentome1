package com.wenwentome.reader.bridge.source

import com.wenwentome.reader.bridge.source.model.ResponseKind
import com.wenwentome.reader.bridge.source.model.RuleEngine
import com.wenwentome.reader.bridge.source.model.SearchRequestConfig
import com.wenwentome.reader.bridge.source.model.TextCleaner
import com.wenwentome.reader.bridge.source.model.UnsupportedRuleKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleExpressionParserTest {
    private val parser = RuleExpressionParser()

    @Test
    fun parseExpression_splitsCleanerAndPostScript() {
        val expression = parser.parse(
            ".content@text##(?s)广告.*##@js:result.replace(\"\\u00a0\", \" \")"
        )

        assertEquals(RuleEngine.HTML_CSS, expression.engine)
        assertEquals(".content", expression.selector)
        assertEquals("text", expression.extractor)
        assertEquals(listOf(TextCleaner.RemoveRegex("(?s)广告.*")), expression.cleaners)
        assertEquals("result.replace(\"\\u00a0\", \" \")", expression.postScript)
    }

    @Test
    fun parseExpression_detectsJsonPathWithDollarPrefix() {
        val expression = parser.parse("\$.data[*]")

        assertEquals(RuleEngine.JSON_PATH, expression.engine)
        assertEquals("\$.data[*]", expression.selector)
    }

    @Test
    fun parseExpression_detectsXPathWithDoubleSlashPrefix() {
        val expression = parser.parse("//div[@class='item']/a")

        assertEquals(RuleEngine.HTML_XPATH, expression.engine)
        assertEquals("//div[@class='item']/a", expression.selector)
    }

    @Test
    fun parseExpression_buildsReplaceCleanerWhenReplacementProvided() {
        val expression = parser.parse(".title@text##广告##正文")

        assertEquals(
            listOf(TextCleaner.ReplaceRegex("广告", "正文")),
            expression.cleaners,
        )
    }

    @Test
    fun parseExpression_treatsJsTemplateAsUnsupportedBeforeEngineDetection() {
        val expression = parser.parse("<js>\$.data[0]")

        assertEquals(RuleEngine.HTML_CSS, expression.engine)
        assertEquals("\$.data[0]", expression.selector)
        assertEquals(UnsupportedRuleKind.JS_TEMPLATE, expression.unsupportedKind)
    }

    @Test
    fun parseSearchTemplate_extractsRequestConfig() {
        val template = requireNotNull(
            parser.parseSearchTemplate(
                """https://example.com/search,{ "method": "post", "body": "q={{key}}&page={{page}}" }"""
            )
        )

        assertEquals("https://example.com/search", template.urlTemplate)
        assertEquals(
            SearchRequestConfig(
                method = "POST",
                bodyTemplate = "q={{key}}&page={{page}}",
                rawJson = """{ "method": "post", "body": "q={{key}}&page={{page}}" }""",
            ),
            template.requestConfig,
        )
    }

    @Test
    fun parseSearchTemplate_marksJsTemplatesUnsupportedForPhase1() {
        val template = requireNotNull(parser.parseSearchTemplate("<js>java.ajax('https://example.com')"))

        assertTrue(template.requiresJsTemplate)
        assertEquals(ResponseKind.HTML, template.responseKind)
        assertEquals(UnsupportedRuleKind.JS_TEMPLATE, template.unsupportedKind)
    }
}
