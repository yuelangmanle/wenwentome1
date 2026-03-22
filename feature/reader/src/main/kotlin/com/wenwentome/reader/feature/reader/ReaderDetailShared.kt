package com.wenwentome.reader.feature.reader

import com.wenwentome.reader.core.model.BookFormat
import com.wenwentome.reader.core.model.buildReaderChapterLocator
import kotlin.math.roundToInt

internal fun formatProgressLabel(progressPercent: Float): String =
    "${(progressPercent.coerceIn(0f, 1f) * 100).roundToInt()}%"

internal fun buildLocatorForChapter(format: BookFormat?, chapterRef: String): String? =
    buildReaderChapterLocator(format, chapterRef)
