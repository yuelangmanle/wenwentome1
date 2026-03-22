package com.wenwentome.reader.data.localbooks

import com.wenwentome.reader.core.database.dao.BookAssetDao
import com.wenwentome.reader.core.model.ReaderChapter
import nl.siegmann.epublib.domain.Book
import nl.siegmann.epublib.domain.SpineReference
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

    suspend fun loadChapters(bookId: String): List<ReaderChapter> {
        val asset = requireNotNull(bookAssetDao.findPrimaryAsset(bookId)) {
            "Primary asset missing for $bookId"
        }
        return fileStore.open(asset.storageUri).use { inputStream ->
            when (asset.mime) {
                "text/plain" ->
                    listOf(
                        ReaderChapter(
                            chapterRef = "txt-body",
                            title = "正文",
                            orderIndex = 0,
                            sourceType = com.wenwentome.reader.core.model.BookFormat.TXT,
                            locatorHint = "0",
                        )
                    )

                "application/epub+zip" -> EpubReader().readEpub(inputStream).let(epubCatalogParser::catalog)
                else -> emptyList()
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
        val startIndex =
            if (paragraphs.isEmpty()) {
                0
            } else {
                val resolvedIndex = locator?.toIntOrNull() ?: 0
                resolvedIndex.takeIf { it in paragraphs.indices } ?: 0
            }
        return ReaderContent(
            chapterTitle = "正文",
            paragraphs = paragraphs.drop(startIndex).take(60),
            windowStartParagraphIndex = startIndex,
            totalParagraphCount = paragraphs.size,
        )
    }

    private fun renderEpub(inputStream: InputStream, locator: String?): ReaderContent {
        val book = EpubReader().readEpub(inputStream)
        val catalog = epubCatalogParser.catalog(book)
        val fallbackChapter = catalog.firstOrNull() ?: error("EPUB has no readable chapter")
        val resolvedLocator = resolveEpubLocator(locator, book, catalog, fallbackChapter)
        val chapter = catalog.firstOrNull { sameChapterRef(book, it.chapterRef, resolvedLocator.chapterRef) } ?: fallbackChapter
        val resource = resolveResourceByChapterRef(book, chapter.chapterRef)
            ?: error("EPUB resource missing for chapterRef: ${chapter.chapterRef}")

        val html = resource.inputStream.use { resourceStream ->
            resourceStream.readBytes().decodeToString()
        }
        val allParagraphs = extractParagraphsFromHtml(html)
        val startParagraphIndex =
            if (allParagraphs.isEmpty()) {
                0
            } else {
                resolvedLocator.paragraphIndex.takeIf { it in allParagraphs.indices } ?: 0
            }
        val visibleParagraphs = allParagraphs
            .drop(startParagraphIndex)
            .take(60)
            .ifEmpty { allParagraphs.take(60) }

        return ReaderContent(
            chapterTitle = chapter.title,
            paragraphs = visibleParagraphs,
            chapterRef = chapter.chapterRef,
            windowStartParagraphIndex = startParagraphIndex,
            totalParagraphCount = allParagraphs.size,
        )
    }

    private fun resolveEpubLocator(
        locator: String?,
        book: Book,
        catalog: List<ReaderChapter>,
        fallbackChapter: ReaderChapter,
    ): EpubLocator {
        parseStructuredEpubLocator(locator)?.let { structured ->
            val matchedChapter = catalog.firstOrNull { sameChapterRef(book, it.chapterRef, structured.chapterRef) }
            if (matchedChapter != null) {
                return structured.copy(chapterRef = matchedChapter.chapterRef)
            }

            val recoveredChapter = mapStructuredChapterRefToReadableChapter(
                book = book,
                catalog = catalog,
                chapterRef = structured.chapterRef,
            ) ?: fallbackChapter
            return structured.copy(chapterRef = recoveredChapter.chapterRef)
        }

        parseLegacyEpubLocator(locator)?.let { (legacyChapterIndex, paragraphIndex) ->
            // 兼容 1.0：先在原始 spine 上定位 resource，再映射到过滤后的可读章节，避免升级后错位。
            val chapter = mapLegacySpineIndexToReadableChapter(
                book = book,
                catalog = catalog,
                legacySpineIndex = legacyChapterIndex,
            ) ?: fallbackChapter
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

    private fun mapStructuredChapterRefToReadableChapter(
        book: Book,
        catalog: List<ReaderChapter>,
        chapterRef: String,
    ): ReaderChapter? {
        val normalizedChapterRef = normalizeChapterRef(chapterRef)
        if (normalizedChapterRef.isBlank()) {
            return null
        }

        val spineIndexByRef = findSpineIndexByChapterRef(book, normalizedChapterRef)
        if (spineIndexByRef >= 0) {
            return mapLegacySpineIndexToReadableChapter(book, catalog, spineIndexByRef)
        }

        val resource = resolveResourceByChapterRef(book, normalizedChapterRef) ?: return null
        val spineIndexByResource = findSpineIndexByChapterRef(book, resource.href)
        if (spineIndexByResource < 0) {
            return null
        }
        return mapLegacySpineIndexToReadableChapter(book, catalog, spineIndexByResource)
    }

    private fun mapLegacySpineIndexToReadableChapter(
        book: Book,
        catalog: List<ReaderChapter>,
        legacySpineIndex: Int,
    ): ReaderChapter? {
        val spineReferences = spineReferences(book)
        if (spineReferences.isEmpty()) {
            return null
        }
        val clampedIndex = legacySpineIndex.coerceIn(0, spineReferences.lastIndex)

        chapterForSpineReference(book, spineReferences.getOrNull(clampedIndex), catalog)?.let { return it }
        for (index in (clampedIndex + 1)..spineReferences.lastIndex) {
            chapterForSpineReference(book, spineReferences.getOrNull(index), catalog)?.let { return it }
        }
        for (index in (clampedIndex - 1) downTo 0) {
            chapterForSpineReference(book, spineReferences.getOrNull(index), catalog)?.let { return it }
        }
        return null
    }

    private fun findSpineIndexByChapterRef(book: Book, chapterRef: String): Int {
        val normalizedChapterRef = normalizeChapterRef(chapterRef)
        if (normalizedChapterRef.isBlank()) {
            return -1
        }

        val spineReferences = spineReferences(book)
        spineReferences.forEachIndexed { index, spineReference ->
            val resource = spineReference.resource ?: return@forEachIndexed
            if (sameChapterRef(book, resource.href, normalizedChapterRef)) {
                return index
            }
        }
        return -1
    }

    private fun spineReferences(book: Book): List<SpineReference> =
        book.spine?.spineReferences
            .orEmpty()
            .filterIsInstance<SpineReference>()

    private fun chapterForSpineReference(
        book: Book,
        spineReference: SpineReference?,
        catalog: List<ReaderChapter>,
    ): ReaderChapter? {
        val resource = spineReference?.resource ?: return null
        return catalog.firstOrNull { chapter ->
            sameChapterRef(book, chapter.chapterRef, resource.href)
        }
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

    private fun resolveResourceByChapterRef(book: Book, chapterRef: String): nl.siegmann.epublib.domain.Resource? {
        val normalizedChapterRef = normalizeChapterRef(chapterRef)
        if (normalizedChapterRef.isBlank()) {
            return null
        }
        book.resources.getByHref(normalizedChapterRef)?.let { return it }
        val canonicalChapterRef = canonicalizeChapterRef(book, normalizedChapterRef)
        return book.resources.getAll().firstOrNull { resource ->
            canonicalizeChapterRef(book, resource.href) == canonicalChapterRef
        }
    }

    private fun sameChapterRef(book: Book, left: String, right: String): Boolean =
        canonicalizeChapterRef(book, left) == canonicalizeChapterRef(book, right)

    private fun canonicalizeChapterRef(book: Book, chapterRef: String?): String {
        val normalizedChapterRef = normalizePath(normalizeChapterRef(chapterRef))
        if (normalizedChapterRef.isBlank()) {
            return ""
        }
        if (normalizedChapterRef.contains("://")) {
            return normalizedChapterRef
        }
        val opfHref = normalizePath(normalizeChapterRef(book.opfResource?.href))
        val opfDir = opfHref.substringBeforeLast('/', missingDelimiterValue = "")
        if (opfDir.isBlank()) {
            return normalizedChapterRef
        }
        return if (normalizedChapterRef == opfDir || normalizedChapterRef.startsWith("$opfDir/")) {
            normalizedChapterRef
        } else {
            normalizePath("$opfDir/$normalizedChapterRef")
        }
    }

    private fun normalizeChapterRef(chapterRef: String?): String = chapterRef?.substringBefore('#').orEmpty().removePrefix("/")

    private fun normalizePath(path: String): String {
        val segments = mutableListOf<String>()
        path.split('/').forEach { segment ->
            when (segment) {
                "", "." -> Unit
                ".." -> if (segments.isNotEmpty()) segments.removeAt(segments.lastIndex)
                else -> segments += segment
            }
        }
        return segments.joinToString("/")
    }

    private fun extractParagraphsFromHtml(html: String): List<String> {
        // 1) 去掉不参与正文定位的 head / script / style 块
        var s = html
            .replace(Regex("(?is)<head[^>]*>.*?</head>"), " ")
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
