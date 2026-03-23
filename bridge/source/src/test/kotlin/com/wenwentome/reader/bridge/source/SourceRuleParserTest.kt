package com.wenwentome.reader.bridge.source

import com.wenwentome.reader.bridge.source.model.ResponseKind
import com.wenwentome.reader.bridge.source.model.RuleEngine
import com.wenwentome.reader.bridge.source.model.SearchRequestConfig
import com.wenwentome.reader.bridge.source.model.TextCleaner
import org.junit.Assert.assertEquals
import org.junit.Test

class SourceRuleParserTest {
    @Test
    fun legacySource_searchDetailTocAndContentRules_areNormalized() {
        val source = fixture("legacy-source.json").decodeToString()
        val parsed = SourceRuleParser().parse(source)

        assertEquals("测试源", parsed.name)
        assertEquals(
            "https://example.com/search?title={{key}}",
            parsed.searchUrlTemplate?.urlTemplate,
        )
        val searchRule = requireNotNull(parsed.searchRule)
        assertEquals(ResponseKind.HTML, searchRule.responseKind)
        assertEquals(RuleEngine.HTML_CSS, searchRule.bookList.engine)
        assertEquals(".result-item", searchRule.bookList.selector)
        assertEquals("h3", searchRule.name.selector)
        assertEquals("text", searchRule.name.extractor)
        assertEquals("a", searchRule.bookUrl.selector)
        assertEquals("href", searchRule.bookUrl.extractor)

        val bookInfoRule = requireNotNull(parsed.bookInfoRule)
        assertEquals(ResponseKind.HTML, bookInfoRule.responseKind)
        assertEquals("h1", bookInfoRule.name?.selector)
        assertEquals("text", bookInfoRule.name?.extractor)

        val tocRule = requireNotNull(parsed.tocRule)
        assertEquals(ResponseKind.HTML, tocRule.responseKind)
        assertEquals(".chapter-item", tocRule.chapterList.selector)

        val contentRule = requireNotNull(parsed.contentRule)
        assertEquals(ResponseKind.HTML, contentRule.responseKind)
        assertEquals("#content", contentRule.content.selector)
        assertEquals("html", contentRule.content.extractor)
    }

    @Test
    fun legacySource_withJsonBookList_marksSearchResponseAsJson() {
        val source = """
            {
              "bookSourceName": "JSON测试源",
              "bookSourceUrl": "https://json.example.com",
              "searchUrl": "https://json.example.com/search?title={{key}}",
              "ruleSearch": {
                "bookList": "${'$'}.data[*]",
                "name": "${'$'}.name",
                "bookUrl": "${'$'}.url"
              }
            }
        """.trimIndent()
        val parsed = SourceRuleParser().parse(source)

        val searchRule = requireNotNull(parsed.searchRule)
        assertEquals(ResponseKind.JSON, searchRule.responseKind)
        assertEquals(RuleEngine.JSON_PATH, searchRule.bookList.engine)
        assertEquals("\$.data[*]", searchRule.bookList.selector)
    }

    @Test
    fun legacySource_mapsSearchTemplateConfigAndContentCleaner() {
        val source = """
            {
              "bookSourceName": "POST测试源",
              "bookSourceUrl": "https://post.example.com",
              "searchUrl": "https://post.example.com/search,{\"method\":\"post\",\"body\":\"q={{key}}&page={{page}}\"}",
              "ruleSearch": {
                "bookList": ".result-item",
                "name": "h3@text",
                "bookUrl": "a@href"
              },
              "ruleContent": {
                "content": "#content@html",
                "replaceRegex": "(?s)广告.*"
              }
            }
        """.trimIndent()

        val parsed = SourceRuleParser().parse(source)

        assertEquals(
            SearchRequestConfig(
                method = "POST",
                bodyTemplate = "q={{key}}&page={{page}}",
                rawJson = "{\"method\":\"post\",\"body\":\"q={{key}}&page={{page}}\"}",
            ),
            parsed.searchUrlTemplate?.requestConfig,
        )
        assertEquals(
            listOf(TextCleaner.RemoveRegex("(?s)广告.*")),
            parsed.contentRule?.content?.cleaners,
        )
    }

    @Test
    fun legacySource_preservesBookInfoRuleWhenOnlyKindIsDefined() {
        val source = """
            {
              "bookSourceName": "分类测试源",
              "bookSourceUrl": "https://kind.example.com",
              "ruleBookInfo": {
                "kind": ".meta .kind@text"
              }
            }
        """.trimIndent()

        val parsed = SourceRuleParser().parse(source)

        assertEquals(".meta .kind", parsed.bookInfoRule?.kind?.selector)
        assertEquals("text", parsed.bookInfoRule?.kind?.extractor)
    }
}

private fun fixture(name: String): ByteArray =
    requireNotNull(SourceRuleParserTest::class.java.getResourceAsStream("/fixtures/$name")) {
        "Missing fixture $name"
    }.readBytes()
