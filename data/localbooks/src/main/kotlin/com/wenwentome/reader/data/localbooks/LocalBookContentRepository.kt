package com.wenwentome.reader.data.localbooks

import com.wenwentome.reader.core.database.dao.BookAssetDao
import com.wenwentome.reader.core.model.ReaderChapter
import nl.siegmann.epublib.domain.Book
import nl.siegmann.epublib.domain.SpineReference
import nl.siegmann.epublib.epub.EpubReader
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.util.concurrent.ConcurrentHashMap

class LocalBookContentRepository(
    private val bookAssetDao: BookAssetDao,
    private val fileStore: LocalBookFileStore,
    private val epubCatalogParser: EpubCatalogParser = EpubCatalogParser(),
) {
    private val txtParsedBookCache = ConcurrentHashMap<String, TxtParsedBook>()

    // Locator 语义:
    // - TXT 兼容旧格式: 段落 index 字符串，比如 "0", "18"（按整本书的全局段落序号）
    // - TXT 新格式: "chapter:<chapterRef>#paragraph:<paragraphIndex>"
    // - EPUB 新格式: "chapter:<chapterRef>#paragraph:<paragraphIndex>"
    // - EPUB 兼容旧格式: "<legacySpineIndex>:<paragraphIndex>"
    suspend fun load(bookId: String, locator: String?): ReaderContent {
        val asset = requireNotNull(bookAssetDao.findPrimaryAsset(bookId)) {
            "Primary asset missing for $bookId"
        }
        return when (asset.mime) {
            "text/plain" -> renderTxt(loadParsedTxtBook(asset.storageUri), locator)
            "application/epub+zip" ->
                fileStore.open(asset.storageUri).use { inputStream ->
                    renderEpub(inputStream, locator)
                }

            else -> error("Unsupported asset mime: ${asset.mime}")
        }
    }

    suspend fun loadChapters(bookId: String): List<ReaderChapter> {
        val asset = requireNotNull(bookAssetDao.findPrimaryAsset(bookId)) {
            "Primary asset missing for $bookId"
        }
        return when (asset.mime) {
            "text/plain" -> {
                val parsed = loadParsedTxtBook(asset.storageUri)
                parsed.chapters.map { chapter ->
                    ReaderChapter(
                        chapterRef = chapter.chapterRef,
                        title = chapter.title,
                        orderIndex = chapter.orderIndex,
                        sourceType = com.wenwentome.reader.core.model.BookFormat.TXT,
                        locatorHint = "chapter:${chapter.chapterRef}#paragraph:0",
                    )
                }
            }

            "application/epub+zip" ->
                fileStore.open(asset.storageUri).use { inputStream ->
                    EpubReader().readEpub(inputStream).let(epubCatalogParser::catalog)
                }

            else -> emptyList()
        }
    }

    private fun renderTxt(parsed: TxtParsedBook, locator: String?): ReaderContent {
        val totalParagraphCount = parsed.chapters.sumOf { it.paragraphs.size }

        // Structured locator: chapter:<chapterRef>#paragraph:<paragraphIndex>
        parseStructuredLocator(locator)?.let { structured ->
            val chapter = parsed.chapters.firstOrNull { it.chapterRef == structured.chapterRef } ?: parsed.chapters.first()
            val startIndex = structured.paragraphIndex.coerceIn(0, chapter.paragraphs.lastIndex.coerceAtLeast(0))
            val globalStartIndex = resolveTxtGlobalParagraphIndex(parsed, chapter, startIndex)
            return ReaderContent(
                chapterTitle = chapter.title,
                paragraphs = chapter.paragraphs.drop(startIndex).take(60),
                chapterRef = chapter.chapterRef,
                windowStartParagraphIndex = globalStartIndex,
                totalParagraphCount = totalParagraphCount,
            )
        }

        // Legacy locator: global paragraph index (across the whole book)
        val globalStartIndex = locator?.toIntOrNull()?.coerceAtLeast(0) ?: 0
        val resolved = resolveTxtLegacyParagraphIndex(parsed, globalStartIndex)
        val chapter = resolved.chapter
        val startIndex = resolved.paragraphIndex
        val resolvedGlobalStartIndex = resolveTxtGlobalParagraphIndex(parsed, chapter, startIndex)
        return ReaderContent(
            chapterTitle = chapter.title,
            paragraphs = chapter.paragraphs.drop(startIndex).take(60),
            chapterRef = chapter.chapterRef,
            windowStartParagraphIndex = resolvedGlobalStartIndex,
            totalParagraphCount = totalParagraphCount,
        )
    }

    private fun loadParsedTxtBook(storageUri: String): TxtParsedBook {
        txtParsedBookCache[storageUri]?.let { return it }

        val parsed = fileStore.open(storageUri).use { inputStream ->
            parseTxtBook(inputStream.readBytes())
        }
        txtParsedBookCache[storageUri] = parsed
        return parsed
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
        val match = STRUCTURED_LOCATOR.matchEntire(value) ?: return null
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
        private val STRUCTURED_LOCATOR = Regex("^chapter:(.+)#paragraph:(\\d+)$")
        private val TXT_CHAPTER_TITLE = Regex(
            // Examples:
            // - 第一章 起始
            // - 第1章: 起始
            // - 第十卷 终章
            // - 序章 / 楔子 / 后记 / 尾声 / 番外
            "^(?:" +
                "(第\\s*[0-9零一二三四五六七八九十百千万两〇○０-９]+\\s*[章节回卷篇节集话])" +
                "(?:\\s*[:：\\-—_]+\\s*|\\s+)?(.*)" +
                "|" +
                "((?:序章|序|楔子|引子|前言|后记|尾声|番外))(?:\\s*[:：\\-—_]+\\s*|\\s+)?(.*)" +
                ")$"
        )
    }

    private data class StructuredLocator(
        val chapterRef: String,
        val paragraphIndex: Int,
    )

    private data class TxtChapter(
        val chapterRef: String,
        val title: String,
        val orderIndex: Int,
        val paragraphs: List<String>,
    )

    private data class TxtParsedBook(
        val charset: Charset,
        val chapters: List<TxtChapter>,
    )

    private data class TxtResolvedLocation(
        val chapter: TxtChapter,
        val paragraphIndex: Int,
    )

    private fun parseStructuredLocator(locator: String?): StructuredLocator? {
        val value = locator?.trim().orEmpty()
        if (value.isBlank()) return null
        val match = STRUCTURED_LOCATOR.matchEntire(value) ?: return null
        val chapterRef = match.groupValues[1].trim()
        if (chapterRef.isBlank()) return null
        val paragraphIndex = match.groupValues[2].toIntOrNull()?.coerceAtLeast(0) ?: return null
        return StructuredLocator(
            chapterRef = chapterRef,
            paragraphIndex = paragraphIndex,
        )
    }

    private fun resolveTxtLegacyParagraphIndex(parsed: TxtParsedBook, globalParagraphIndex: Int): TxtResolvedLocation {
        var remaining = globalParagraphIndex.coerceAtLeast(0)
        parsed.chapters.forEach { chapter ->
            val size = chapter.paragraphs.size
            if (remaining < size) {
                return TxtResolvedLocation(chapter = chapter, paragraphIndex = remaining)
            }
            remaining -= size
        }
        // Out of range: fall back to last chapter start.
        val last = parsed.chapters.last()
        return TxtResolvedLocation(chapter = last, paragraphIndex = 0)
    }

    private fun resolveTxtGlobalParagraphIndex(
        parsed: TxtParsedBook,
        targetChapter: TxtChapter,
        paragraphIndex: Int,
    ): Int {
        var offset = 0
        parsed.chapters.forEach { chapter ->
            if (chapter.chapterRef == targetChapter.chapterRef) {
                return offset + paragraphIndex.coerceIn(0, chapter.paragraphs.lastIndex.coerceAtLeast(0))
            }
            offset += chapter.paragraphs.size
        }
        return 0
    }

    private fun parseTxtBook(bytes: ByteArray): TxtParsedBook {
        val charset = sniffTxtCharset(bytes)
        val text = decodeTxt(bytes, charset)
        val normalized = normalizeTxt(text)
        val chapters = splitTxtChapters(normalized)
        val finalChapters =
            if (chapters.isEmpty()) {
                listOf(
                    TxtChapter(
                        chapterRef = "txt:0",
                        title = "正文",
                        orderIndex = 0,
                        paragraphs = extractTxtParagraphs(normalized).ifEmpty { fallbackTxtParagraphs(normalized) },
                    )
                )
            } else {
                chapters.mapIndexed { index, chapter ->
                    chapter.copy(
                        chapterRef = "txt:$index",
                        orderIndex = index,
                    )
                }
            }

        return TxtParsedBook(
            charset = charset,
            chapters = finalChapters.ifEmpty {
                listOf(
                    TxtChapter(
                        chapterRef = "txt:0",
                        title = "正文",
                        orderIndex = 0,
                        paragraphs = emptyList(),
                    )
                )
            },
        )
    }

    private fun sniffTxtCharset(bytes: ByteArray): Charset {
        if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
            return Charsets.UTF_16BE
        }
        if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) {
            return Charsets.UTF_16LE
        }
        if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
            return Charsets.UTF_8
        }
        if (looksLikeUtf16(bytes, zeroOnOddBytes = true)) {
            return Charsets.UTF_16LE
        }
        if (looksLikeUtf16(bytes, zeroOnOddBytes = false)) {
            return Charsets.UTF_16BE
        }

        val utf8Decoder = Charsets.UTF_8
            .newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        return try {
            utf8Decoder.decode(ByteBuffer.wrap(bytes))
            Charsets.UTF_8
        } catch (_: CharacterCodingException) {
            Charset.forName("GB18030")
        }
    }

    private fun looksLikeUtf16(bytes: ByteArray, zeroOnOddBytes: Boolean): Boolean {
        if (bytes.size < 4) {
            return false
        }

        var zeroCount = 0
        var sampleCount = 0
        var oppositeZeroCount = 0
        val startIndex = if (zeroOnOddBytes) 1 else 0
        val oppositeStartIndex = if (zeroOnOddBytes) 0 else 1
        val upperBound = bytes.size.coerceAtMost(200)

        for (index in startIndex until upperBound step 2) {
            sampleCount++
            if (bytes[index] == 0.toByte()) {
                zeroCount++
            }
        }
        for (index in oppositeStartIndex until upperBound step 2) {
            if (bytes[index] == 0.toByte()) {
                oppositeZeroCount++
            }
        }

        if (sampleCount == 0) {
            return false
        }

        val zeroRatio = zeroCount.toFloat() / sampleCount.toFloat()
        val oppositeZeroRatio = oppositeZeroCount.toFloat() / sampleCount.toFloat()
        return zeroRatio >= 0.6f && oppositeZeroRatio <= 0.2f
    }

    private fun decodeTxt(bytes: ByteArray, charset: Charset): String {
        val payload =
            if (charset == Charsets.UTF_8 &&
                bytes.size >= 3 &&
                bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()
            ) {
                bytes.copyOfRange(3, bytes.size)
            } else {
                bytes
            }
        return payload.toString(charset)
    }

    private fun normalizeTxt(text: String): String =
        text
            .removePrefix("\uFEFF")
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .trim()

    private fun splitTxtChapters(text: String): List<TxtChapter> {
        val lines = text.split('\n')
        val chapters = mutableListOf<TxtChapter>()

        var currentTitle: String? = null
        val currentParagraphs = mutableListOf<String>()
        val paragraphLines = mutableListOf<String>()

        fun flushParagraph() {
            if (paragraphLines.isEmpty()) return
            val paragraph = joinTxtLines(paragraphLines)
            paragraphLines.clear()
            if (paragraph.isNotBlank()) {
                currentParagraphs += paragraph
            }
        }

        fun flushChapter() {
            flushParagraph()
            val title = currentTitle?.takeIf { it.isNotBlank() } ?: return
            chapters += TxtChapter(
                chapterRef = "", // filled later
                title = title,
                orderIndex = -1, // filled later
                paragraphs = currentParagraphs.toList(),
            )
            currentTitle = null
            currentParagraphs.clear()
        }

        var seenHeading = false
        val prefaceParagraphs = mutableListOf<String>()
        val prefaceParagraphLines = mutableListOf<String>()

        fun flushPrefaceParagraph() {
            if (prefaceParagraphLines.isEmpty()) return
            val paragraph = joinTxtLines(prefaceParagraphLines)
            prefaceParagraphLines.clear()
            if (paragraph.isNotBlank()) {
                prefaceParagraphs += paragraph
            }
        }

        lines.forEach { rawLine ->
            val line = rawLine.trim().trim('\u3000')
            if (line.isBlank()) {
                // Blank lines are treated as visual separators; we keep paragraphs line-based for stability.
                return@forEach
            }

            val title = extractChapterTitle(line)
            if (title != null) {
                if (!seenHeading) {
                    flushPrefaceParagraph()
                    if (prefaceParagraphs.isNotEmpty()) {
                        chapters += TxtChapter(
                            chapterRef = "",
                            title = "前言",
                            orderIndex = -1,
                            paragraphs = prefaceParagraphs.toList(),
                        )
                        prefaceParagraphs.clear()
                    }
                    seenHeading = true
                }

                // heading line itself is not a paragraph
                if (currentTitle != null) {
                    flushChapter()
                } else {
                    // start new chapter after preface: nothing to flush, just clear any pending paragraph lines
                    paragraphLines.clear()
                    currentParagraphs.clear()
                }
                currentTitle = title
                return@forEach
            }

            if (!seenHeading) {
                prefaceParagraphLines += line
                flushPrefaceParagraph()
            } else {
                if (currentTitle == null) {
                    // Content without an explicit heading: treat as "正文" to keep navigation workable.
                    currentTitle = "正文"
                }
                paragraphLines += line
                flushParagraph()
            }
        }

        if (!seenHeading) {
            flushPrefaceParagraph()
            return emptyList()
        }

        flushChapter()
        return chapters
    }

    private fun extractTxtParagraphs(text: String): List<String> {
        val lines = text.split('\n')
        val paragraphs = mutableListOf<String>()
        val paragraphLines = mutableListOf<String>()
        fun flush() {
            if (paragraphLines.isEmpty()) return
            val paragraph = joinTxtLines(paragraphLines)
            paragraphLines.clear()
            if (paragraph.isNotBlank()) paragraphs += paragraph
        }
        lines.forEach { raw ->
            val line = raw.trim().trim('\u3000')
            if (line.isBlank()) {
                // Keep paragraphs line-based; blank lines are separators only.
            } else {
                paragraphLines += line
                flush()
            }
        }
        flush()
        return paragraphs
    }

    private fun fallbackTxtParagraphs(text: String): List<String> {
        val normalized = text
            .split('\n')
            .map { it.trim().trim('\u3000') }
            .filter { it.isNotBlank() }
        if (normalized.isEmpty()) {
            return listOf("正文解析失败，请检查文件编码")
        }
        return listOf(normalized.joinToString("\n"))
    }

    private fun joinTxtLines(lines: List<String>): String {
        val cleaned = lines
            .asSequence()
            .map { it.trim().trim('\u3000') }
            .filter { it.isNotBlank() }
            .toList()
        if (cleaned.isEmpty()) return ""

        val sb = StringBuilder()
        cleaned.forEachIndexed { idx, part ->
            if (idx == 0) {
                sb.append(part)
                return@forEachIndexed
            }

            val prev = sb.lastOrNull()
            val needsSpace =
                (prev != null && prev.isLetterOrDigit()) &&
                    (part.firstOrNull()?.isLetterOrDigit() == true)
            if (needsSpace) sb.append(' ')
            sb.append(part)
        }

        return sb.toString()
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun extractChapterTitle(line: String): String? {
        val candidate = line
            .trim()
            .trim('\u3000')
            .trim('【', '】', '[', ']', '(', ')', '（', '）')
            .replace(Regex("\\s+"), " ")
            .trim()
        if (candidate.length !in 2..60) return null
        if (candidate.contains("http", ignoreCase = true) || candidate.contains("www.", ignoreCase = true)) return null
        if (candidate.endsWith("。") || candidate.endsWith("！") || candidate.endsWith("？")) return null

        val match = TXT_CHAPTER_TITLE.matchEntire(candidate) ?: return null
        val main = match.groupValues[1].ifBlank { match.groupValues[3] }
        if (main.isBlank()) return null

        val suffix = (match.groupValues[2].ifBlank { match.groupValues[4] }).trim()
        val title = if (suffix.isBlank()) main.trim() else "${main.trim()} ${suffix}"
        return title.replace(Regex("\\s+"), " ").trim()
    }
}
