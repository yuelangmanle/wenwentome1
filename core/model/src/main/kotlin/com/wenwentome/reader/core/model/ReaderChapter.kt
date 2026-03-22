package com.wenwentome.reader.core.model

data class ReaderChapter(
    val chapterRef: String,
    val title: String,
    val orderIndex: Int,
    val sourceType: BookFormat,
    val locatorHint: String? = null,
    val isLatest: Boolean = false,
)
