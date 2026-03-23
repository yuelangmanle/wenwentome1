package com.wenwentome.reader.bridge.source.model

data class NormalizedSourceDefinition(
    val id: String,
    val name: String,
    val group: String? = null,
    val sourceType: Int = 0,
    val baseUrl: String,
    val enabled: Boolean = true,
    val searchUrlTemplate: ParsedSearchUrlTemplate? = null,
    val searchRule: SearchRule? = null,
    val bookInfoRule: BookInfoRule? = null,
    val tocRule: TocRule? = null,
    val contentRule: ContentRule? = null,
)

data class SearchRule(
    val responseKind: ResponseKind = ResponseKind.HTML,
    val bookList: RuleExpression,
    val name: RuleExpression,
    val bookUrl: RuleExpression,
    val author: RuleExpression? = null,
    val coverUrl: RuleExpression? = null,
    val intro: RuleExpression? = null,
)

data class BookInfoRule(
    val responseKind: ResponseKind = ResponseKind.HTML,
    val name: RuleExpression? = null,
    val author: RuleExpression? = null,
    val intro: RuleExpression? = null,
    val coverUrl: RuleExpression? = null,
    val kind: RuleExpression? = null,
    val lastChapter: RuleExpression? = null,
)

data class TocRule(
    val responseKind: ResponseKind = ResponseKind.HTML,
    val chapterList: RuleExpression,
    val chapterName: RuleExpression,
    val chapterUrl: RuleExpression,
    val nextTocUrl: RuleExpression? = null,
)

data class ContentRule(
    val responseKind: ResponseKind = ResponseKind.HTML,
    val content: RuleExpression,
    val nextContentUrl: RuleExpression? = null,
    val sourceRegex: String? = null,
)
