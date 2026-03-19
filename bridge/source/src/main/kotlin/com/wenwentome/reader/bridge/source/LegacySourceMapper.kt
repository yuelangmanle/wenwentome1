package com.wenwentome.reader.bridge.source

import com.wenwentome.reader.bridge.source.model.BookInfoRule
import com.wenwentome.reader.bridge.source.model.ContentRule
import com.wenwentome.reader.bridge.source.model.LegacyRuleDefinition
import com.wenwentome.reader.bridge.source.model.LegacySourceDefinition
import com.wenwentome.reader.bridge.source.model.NormalizedSourceDefinition
import com.wenwentome.reader.bridge.source.model.SearchRule
import com.wenwentome.reader.bridge.source.model.TocRule

class LegacySourceMapper {
    fun map(source: LegacySourceDefinition): NormalizedSourceDefinition =
        NormalizedSourceDefinition(
            id = source.bookSourceUrl,
            name = source.bookSourceName,
            group = source.bookSourceGroup?.takeIf { it.isNotBlank() },
            sourceType = source.bookSourceType,
            baseUrl = source.bookSourceUrl.toBaseUrl(),
            enabled = source.enabled,
            searchUrlTemplate = source.searchUrl,
            searchRule = source.ruleSearch?.toSearchRule(),
            bookInfoRule = source.ruleBookInfo?.toBookInfoRule(),
            tocRule = source.ruleToc?.toTocRule(),
            contentRule = source.ruleContent?.toContentRule(),
        )

    private fun LegacyRuleDefinition.toSearchRule(): SearchRule? {
        val bookList = this["bookList"] ?: return null
        val name = this["name"] ?: return null
        val bookUrl = this["bookUrl"] ?: return null
        return SearchRule(
            bookList = bookList,
            name = name,
            bookUrl = bookUrl,
            author = this["author"],
            coverUrl = this["coverUrl"],
            intro = this["intro"],
        )
    }

    private fun LegacyRuleDefinition.toBookInfoRule(): BookInfoRule? =
        BookInfoRule(
            name = this["name"],
            author = this["author"],
            intro = this["intro"],
            coverUrl = this["coverUrl"],
            kind = this["kind"],
            lastChapter = this["lastChapter"],
        ).takeIf {
            it.name != null || it.author != null || it.intro != null || it.coverUrl != null
        }

    private fun LegacyRuleDefinition.toTocRule(): TocRule? {
        val chapterList = this["chapterList"] ?: return null
        val chapterName = this["chapterName"] ?: return null
        val chapterUrl = this["chapterUrl"] ?: return null
        return TocRule(
            chapterList = chapterList,
            chapterName = chapterName,
            chapterUrl = chapterUrl,
            nextTocUrl = this["nextTocUrl"],
        )
    }

    private fun LegacyRuleDefinition.toContentRule(): ContentRule? {
        val content = this["content"] ?: return null
        return ContentRule(
            content = content,
            nextContentUrl = this["nextContentUrl"],
            replaceRegex = this["replaceRegex"],
            sourceRegex = this["sourceRegex"],
        )
    }

    private fun String.toBaseUrl(): String =
        substringBefore("##")
            .substringBefore('#')
            .trimEnd('/')
}
