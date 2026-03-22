package com.wenwentome.reader.bridge.source.model

data class RemoteSearchResult(
    val id: String,
    val sourceId: String,
    val title: String,
    val author: String? = null,
    val detailUrl: String,
)

data class RemoteBookDetail(
    val title: String,
    val author: String? = null,
    val summary: String? = null,
    val coverUrl: String? = null,
    /**
     * 对齐 Source rule 里的 BookInfoRule.lastChapter（通常是详情页显示的最新章节标题）。
     * 该值用于在 Discover 层匹配 TOC 条目，避免依赖站点目录顺序。
     */
    val lastChapter: String? = null,
)

data class RemoteChapter(
    val chapterRef: String,
    val title: String,
)

data class RemoteChapterContent(
    val chapterRef: String,
    val title: String,
    val content: String,
)
