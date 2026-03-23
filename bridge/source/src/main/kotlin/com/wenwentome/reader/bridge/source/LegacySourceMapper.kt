package com.wenwentome.reader.bridge.source

import com.wenwentome.reader.bridge.source.model.BookInfoRule
import com.wenwentome.reader.bridge.source.model.ContentRule
import com.wenwentome.reader.bridge.source.model.LegacyRuleDefinition
import com.wenwentome.reader.bridge.source.model.LegacySourceDefinition
import com.wenwentome.reader.bridge.source.model.NormalizedSourceDefinition
import com.wenwentome.reader.bridge.source.model.ResponseKind
import com.wenwentome.reader.bridge.source.model.RuleEngine
import com.wenwentome.reader.bridge.source.model.RuleExpression
import com.wenwentome.reader.bridge.source.model.SearchRule
import com.wenwentome.reader.bridge.source.model.TextCleaner
import com.wenwentome.reader.bridge.source.model.TocRule

class LegacySourceMapper(
    private val expressionParser: RuleExpressionParser = RuleExpressionParser(),
) {
    fun map(source: LegacySourceDefinition): NormalizedSourceDefinition =
        NormalizedSourceDefinition(
            id = source.bookSourceUrl,
            name = source.bookSourceName,
            group = source.bookSourceGroup?.takeIf { it.isNotBlank() },
            sourceType = source.bookSourceType,
            baseUrl = source.bookSourceUrl.toBaseUrl(),
            enabled = source.enabled,
            searchUrlTemplate = expressionParser.parseSearchTemplate(source.searchUrl),
            searchRule = source.ruleSearch?.toSearchRule(),
            bookInfoRule = source.ruleBookInfo?.toBookInfoRule(),
            tocRule = source.ruleToc?.toTocRule(),
            contentRule = source.ruleContent?.toContentRule(),
        )

    private fun LegacyRuleDefinition.toSearchRule(): SearchRule? {
        val bookList = this["bookList"] ?: return null
        val name = this["name"] ?: return null
        val bookUrl = this["bookUrl"] ?: return null
        val bookListExpression = expressionParser.parse(bookList)
        return SearchRule(
            responseKind = responseKindOf(bookListExpression.engine),
            bookList = bookListExpression,
            name = expressionParser.parse(name),
            bookUrl = expressionParser.parse(bookUrl),
            author = this["author"]?.let(expressionParser::parse),
            coverUrl = this["coverUrl"]?.let(expressionParser::parse),
            intro = this["intro"]?.let(expressionParser::parse),
        )
    }

    private fun LegacyRuleDefinition.toBookInfoRule(): BookInfoRule? {
        val name = this["name"]?.let(expressionParser::parse)
        val author = this["author"]?.let(expressionParser::parse)
        val intro = this["intro"]?.let(expressionParser::parse)
        val coverUrl = this["coverUrl"]?.let(expressionParser::parse)
        val kind = this["kind"]?.let(expressionParser::parse)
        val lastChapter = this["lastChapter"]?.let(expressionParser::parse)
        return BookInfoRule(
            responseKind = responseKindOf(name, author, intro, coverUrl, kind, lastChapter),
            name = name,
            author = author,
            intro = intro,
            coverUrl = coverUrl,
            kind = kind,
            lastChapter = lastChapter,
        ).takeIf {
            it.name != null || it.author != null || it.intro != null || it.coverUrl != null ||
                it.kind != null || it.lastChapter != null
        }
    }

    private fun LegacyRuleDefinition.toTocRule(): TocRule? {
        val chapterList = this["chapterList"] ?: return null
        val chapterName = this["chapterName"] ?: return null
        val chapterUrl = this["chapterUrl"] ?: return null
        val chapterListExpression = expressionParser.parse(chapterList)
        val chapterNameExpression = expressionParser.parse(chapterName)
        val chapterUrlExpression = expressionParser.parse(chapterUrl)
        return TocRule(
            responseKind = responseKindOf(chapterListExpression, chapterNameExpression, chapterUrlExpression),
            chapterList = chapterListExpression,
            chapterName = chapterNameExpression,
            chapterUrl = chapterUrlExpression,
            nextTocUrl = this["nextTocUrl"]?.let(expressionParser::parse),
        )
    }

    private fun LegacyRuleDefinition.toContentRule(): ContentRule? {
        val content = this["content"] ?: return null
        val contentExpression = expressionParser.parse(content)
        val mergedContentExpression = this["replaceRegex"]
            ?.takeIf { it.isNotBlank() }
            ?.let { regex ->
                contentExpression.copy(
                    cleaners = contentExpression.cleaners + TextCleaner.RemoveRegex(regex),
                )
            } ?: contentExpression
        val nextContentUrl = this["nextContentUrl"]?.let(expressionParser::parse)
        return ContentRule(
            responseKind = responseKindOf(mergedContentExpression, nextContentUrl),
            content = mergedContentExpression,
            nextContentUrl = nextContentUrl,
            sourceRegex = this["sourceRegex"],
        )
    }

    private fun responseKindOf(engine: RuleEngine): ResponseKind =
        if (engine == RuleEngine.JSON_PATH) ResponseKind.JSON else ResponseKind.HTML

    private fun responseKindOf(vararg expressions: RuleExpression?): ResponseKind =
        if (expressions.filterNotNull().any { it.engine == RuleEngine.JSON_PATH }) {
            ResponseKind.JSON
        } else {
            ResponseKind.HTML
        }

    private fun String.toBaseUrl(): String =
        substringBefore("##")
            .substringBefore('#')
            .trimEnd('/')
}
