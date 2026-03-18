package com.wenwentome.reader.data.localbooks

import com.wenwentome.reader.core.database.dao.BookAssetDao
import nl.siegmann.epublib.epub.EpubReader
import java.io.InputStream

class LocalBookContentRepository(
    private val bookAssetDao: BookAssetDao,
    private val fileStore: LocalBookFileStore,
) {
    // Locator 语义（Phase 1）:
    // - TXT: 段落 index 字符串，比如 "0", "18"
    // - EPUB: "<spineIndex>:<paragraphIndex>"，比如 "3:12"
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
        val parts = locator?.split(":").orEmpty()
        val spineIndex = parts.getOrNull(0)?.toIntOrNull()?.coerceAtLeast(0) ?: 0
        val paragraphIndex = parts.getOrNull(1)?.toIntOrNull()?.coerceAtLeast(0) ?: 0

        val book = EpubReader().readEpub(inputStream)
        val refs = book.spine?.spineReferences.orEmpty()
        val clampedSpineIndex = spineIndex.coerceIn(0, (refs.size - 1).coerceAtLeast(0))
        val resource = refs.getOrNull(clampedSpineIndex)?.resource
            ?: error("EPUB spine is empty or spine index out of bounds: $spineIndex")

        // 不引入 jsoup，用极简 HTML 清洗，满足 MVP 读取能力即可。
        val html = resource.inputStream.use { resourceStream ->
            resourceStream.readBytes().decodeToString()
        }
        val allParagraphs = extractParagraphsFromHtml(html)

        return ReaderContent(
            chapterTitle = resource.title ?: "章节 ${clampedSpineIndex + 1}",
            paragraphs = allParagraphs.drop(paragraphIndex).take(60),
        )
    }

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
}
