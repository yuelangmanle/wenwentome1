package com.wenwentome.reader.feature.reader

import com.wenwentome.reader.core.model.BookFormat
import kotlin.math.roundToInt

internal fun formatProgressLabel(progressPercent: Float): String =
    "${(progressPercent.coerceIn(0f, 1f) * 100).roundToInt()}%"

internal fun buildLocatorForChapter(format: BookFormat?, chapterRef: String): String? =
    when (format) {
        BookFormat.TXT -> "0"
        BookFormat.EPUB -> "chapter:$chapterRef#paragraph:0"
        BookFormat.WEB -> chapterRef
        null -> chapterRef
    }
