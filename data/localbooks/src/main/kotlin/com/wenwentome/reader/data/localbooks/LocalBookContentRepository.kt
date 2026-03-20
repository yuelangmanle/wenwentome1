package com.wenwentome.reader.data.localbooks

import com.wenwentome.reader.core.database.dao.BookAssetDao
import com.wenwentome.reader.core.model.ReaderChapter
import nl.siegmann.epublib.epub.EpubReader
import java.io.InputStream

class LocalBookContentRepository(
    private val bookAssetDao: BookAssetDao,
    private val fileStore: LocalBookFileStore,
    private val epubCatalogParser: EpubCatalogParser = EpubCatalogParser(),
) {
    // Locator 语义:
    // - TXT: 段落 index 字符串，比如 "0", "18"
    // - EPUB 新格式: "chapter:<chapterRef>#paragraph:<paragraphIndex>"
    // - EPUB 兼容旧格式: "<legacySpineIndex>:<paragraphIndex>"
    suspend fun load(bookId: String, locator: String?): ReaderContent {
        val asset = requireNotNull(bookAssetDao.findPrimaryAsset(bookId)) {
            "Primary asset missing for $bookId"
        }
        return fileStore.open(asset.storageUri).use { inputStream ->
            when (asset.mime) {
                "text/plain" -> renderTxt(inputStream, locator)
                "application/epub+zip" -> renderEpub(inputStream, locator)
                else -> error("Unsupported asset mime: ${asset.mime}")
            }
        }
    }

    private fun renderTxt(inputStream: InputStream, locator: String?): ReaderContent {
        val paragraphs = inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
            reader.readText()
                .lineSequence()
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .toList()
        }
        val startIndex = locator?.toIntOrNull()?.coerceAtLeast(0) ?: 0
        return ReaderContent(
            chapterTitle = "正文",
            paragraphs = paragraphs.drop(startIndex).take(60),
        )
    }

    private fun renderEpub(inputStream: InputStream, locator: String?): ReaderContent {
        val book = EpubReader().readEpub(inputStream)
        val catalog = epubCatalogParser.catalog(book)
        val fallbackChapter = catalog.firstOrNull() ?: error("EPUB has no readable chapter")
        val resolvedLocator = resolveEpubLocator(locator, catalog, fallbackChapter)
        val chapter = catalog.firstOrNull { sameChapterRef(it.chapterRef, resolvedLocator.chapterRef) } ?: fallbackChapter
        val resource = book.resources.getByHref(chapter.chapterRef)
            ?: book.resources.getByHref(normalizeChapterRef(chapter.chapterRef))
            ?: error("EPUB resource missing for chapterRef: ${chapter.chapterRef}")

        val html = resource.inputStream.use { resourceStream ->
            resourceStream.readBytes().decodeToString()
        }
        val allParagraphs = extractParagraphsFromHtml(html)

        return ReaderContent(
            chapterTitle = chapter.title,
            paragraphs = allParagraphs.drop(resolvedLocator.paragraphIndex).take(60),
        )
    }

    private fun resolveEpubLocator(
        locator: String?,
        catalog: List<ReaderChapter>,
        fallbackChapter: ReaderChapter,
    ): EpubLocator {
        parseStructuredEpubLocator(locator)?.let { structured ->
            val matchedChapter = catalog.firstOrNull { sameChapterRef(it.chapterRef, structured.chapterRef) } ?: fallbackChapter
            return structured.copy(chapterRef = matchedChapter.chapterRef)
        }

        parseLegacyEpubLocator(locator)?.let { (legacyChapterIndex, paragraphIndex) ->
            // 兼容 1.0：旧 locator 的 spineIndex 视为“过滤后可读章节索引”，防止升级后丢进度。
            val chapter = catalog.getOrElse(legacyChapterIndex.coerceIn(0, catalog.lastIndex)) { fallbackChapter }
            return EpubLocator(
                chapterRef = chapter.chapterRef,
                paragraphIndex = paragraphIndex,
            )
        }

        return EpubLocator(
            chapterRef = fallbackChapter.chapterRef,
            paragraphIndex = 0,
        )
    }

    private fun parseStructuredEpubLocator(locator: String?): EpubLocator? {
        val value = locator?.trim().orEmpty()
        if (value.isBlank()) {
            return null
        }
        val match = STRUCTURED_EPUB_LOCATOR.matchEntire(value) ?: return null
        val chapterRef = match.groupValues[1].trim()
        if (chapterRef.isBlank()) {
            return null
        }
        val paragraphIndex = match.groupValues[2].toIntOrNull()?.coerceAtLeast(0) ?: return null
        return EpubLocator(
            chapterRef = chapterRef,
            paragraphIndex = paragraphIndex,
        )
    }

    private fun parseLegacyEpubLocator(locator: String?): Pair<Int, Int>? {
        val value = locator?.trim().orEmpty()
        if (value.isBlank()) {
            return null
        }
        val parts = value.split(":")
        if (parts.size != 2) {
            return null
        }
        val chapterIndex = parts[0].toIntOrNull()?.coerceAtLeast(0) ?: return null
        val paragraphIndex = parts[1].toIntOrNull()?.coerceAtLeast(0) ?: return null
        return chapterIndex to paragraphIndex
    }

    private fun sameChapterRef(left: String, right: String): Boolean =
        normalizeChapterRef(left) == normalizeChapterRef(right)

    private fun normalizeChapterRef(chapterRef: String): String = chapterRef.substringBefore('#')

    private fun extractParagraphsFromHtml(html: String): List<String> {
        // 1) 去掉 <script>/<style> 块
        var s = html
            .replace(Regex("(?is)<script[^>]*>.*?</script>"), " ")
            .replace(Regex("(?is)<style[^>]*>.*?</style>"), " ")

        // 2) 把常见的块级分隔符转成换行
        s = s
            .replace(Regex("(?i)<br\\s*/?>"), "\n")
            .replace(Regex("(?i)</p\\s*>"), "\n")
            .replace(Regex("(?i)</div\\s*>"), "\n")
            .replace(Regex("(?i)</h\\d\\s*>"), "\n")

        // 3) 去掉剩余标签
        s = s.replace(Regex("(?s)<[^>]+>"), " ")

        // 4) 简单实体解码
        s = s
            .replace("&nbsp;", " ")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")

        val byNewline = s
            .replace("\r", "\n")
            .split("\n")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        // 如果 HTML 被清洗后只有一行，按中文句号再切一下，给阅读最小颗粒度。
        if (byNewline.size <= 1) {
            return byNewline
                .flatMap { it.split("。") }
                .map { it.trim() }
                .filter { it.isNotBlank() }
        }
        return byNewline
    }

    private companion object {
        private val STRUCTURED_EPUB_LOCATOR = Regex("^chapter:(.+)#paragraph:(\\d+)$")
    }
}
