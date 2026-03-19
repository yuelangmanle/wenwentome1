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
