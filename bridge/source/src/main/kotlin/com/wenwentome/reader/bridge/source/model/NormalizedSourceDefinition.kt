package com.wenwentome.reader.bridge.source.model

data class NormalizedSourceDefinition(
    val id: String,
    val name: String,
    val group: String? = null,
    val sourceType: Int = 0,
    val baseUrl: String,
    val enabled: Boolean = true,
    val searchUrlTemplate: String? = null,
    val searchRule: SearchRule? = null,
    val bookInfoRule: BookInfoRule? = null,
    val tocRule: TocRule? = null,
    val contentRule: ContentRule? = null,
)

data class SearchRule(
    val bookList: String,
    val name: String,
    val bookUrl: String,
    val author: String? = null,
    val coverUrl: String? = null,
    val intro: String? = null,
)

data class BookInfoRule(
    val name: String? = null,
    val author: String? = null,
    val intro: String? = null,
    val coverUrl: String? = null,
    val kind: String? = null,
    val lastChapter: String? = null,
)

data class TocRule(
    val chapterList: String,
    val chapterName: String,
    val chapterUrl: String,
    val nextTocUrl: String? = null,
)

data class ContentRule(
    val content: String,
    val nextContentUrl: String? = null,
    val replaceRegex: String? = null,
    val sourceRegex: String? = null,
)
