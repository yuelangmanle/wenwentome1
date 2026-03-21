package com.wenwentome.reader.core.model

import java.net.URLDecoder
import java.net.URLEncoder

private val structuredEpubLocator = Regex("^chapter:(.+)#paragraph:(\\d+)$")
private val structuredWebLocator = Regex("^web:chapter=(.+)#paragraph:(\\d+)$")

fun buildReaderChapterLocator(format: BookFormat?, chapterRef: String): String? =
    when (format) {
        BookFormat.TXT -> "0"
        BookFormat.EPUB -> "chapter:$chapterRef#paragraph:0"
        BookFormat.WEB -> buildStructuredWebLocator(chapterRef = chapterRef, paragraphIndex = 0)
        null -> chapterRef
    }

fun buildReaderParagraphLocator(
    format: BookFormat?,
    chapterRef: String,
    paragraphIndex: Int,
): String? =
    when (format) {
        BookFormat.TXT -> paragraphIndex.coerceAtLeast(0).toString()
        BookFormat.EPUB -> "chapter:$chapterRef#paragraph:${paragraphIndex.coerceAtLeast(0)}"
        BookFormat.WEB -> buildStructuredWebLocator(chapterRef = chapterRef, paragraphIndex = paragraphIndex)
        null -> chapterRef
    }

fun resolveReaderChapterRef(format: BookFormat?, locator: String?): String? {
    val value = locator?.trim().orEmpty()
    if (value.isBlank()) return null

    return when (format) {
        BookFormat.EPUB ->
            structuredEpubLocator.matchEntire(value)?.groupValues?.getOrNull(1)

        BookFormat.WEB ->
            structuredWebLocator.matchEntire(value)
                ?.groupValues
                ?.getOrNull(1)
                ?.decodeReaderLocatorComponent()
                ?: value

        else -> null
    }?.takeIf { it.isNotBlank() }
}

fun resolveReaderParagraphIndex(format: BookFormat?, locator: String?): Int {
    val value = locator?.trim().orEmpty()
    if (value.isBlank()) return 0

    return when (format) {
        BookFormat.TXT ->
            value.toIntOrNull()?.coerceAtLeast(0) ?: 0

        BookFormat.EPUB ->
            structuredEpubLocator.matchEntire(value)
                ?.groupValues
                ?.getOrNull(2)
                ?.toIntOrNull()
                ?.coerceAtLeast(0)
                ?: 0

        BookFormat.WEB ->
            structuredWebLocator.matchEntire(value)
                ?.groupValues
                ?.getOrNull(2)
                ?.toIntOrNull()
                ?.coerceAtLeast(0)
                ?: 0

        null -> 0
    }
}

private fun buildStructuredWebLocator(
    chapterRef: String,
    paragraphIndex: Int,
): String =
    "web:chapter=${chapterRef.encodeReaderLocatorComponent()}#paragraph:${paragraphIndex.coerceAtLeast(0)}"

private fun String.encodeReaderLocatorComponent(): String =
    URLEncoder.encode(this, Charsets.UTF_8.name())

private fun String.decodeReaderLocatorComponent(): String =
    URLDecoder.decode(this, Charsets.UTF_8.name())
