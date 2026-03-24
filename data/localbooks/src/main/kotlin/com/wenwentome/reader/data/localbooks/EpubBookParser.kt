package com.wenwentome.reader.data.localbooks

import com.wenwentome.reader.core.model.AssetRole
import com.wenwentome.reader.core.model.BookFormat
import nl.siegmann.epublib.domain.Book
import nl.siegmann.epublib.domain.Resource
import nl.siegmann.epublib.epub.EpubReader
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

class EpubBookParser {
    private val epubCatalogParser = EpubCatalogParser()

    fun parse(name: String, inputStream: InputStream): ParsedLocalBook {
        val bytes = inputStream.readBytes()
        val epubBook = runCatching { EpubReader().readEpub(ByteArrayInputStream(bytes)) }
            .getOrElse { error ->
                throw IllegalStateException("EPUB 文件无效或已损坏", error)
            }
        validateReadableEpub(book = epubBook)
        val opfMetadata = extractOpfMetadata(bytes)

        val coverAsset = epubBook?.coverImage?.let { coverResource ->
            runCatching {
                ParsedAsset(
                    assetRole = AssetRole.COVER,
                    bytes = coverResource.data,
                    mime = coverResource.mediaType?.name ?: "image/jpeg",
                    extension = resolveExtension(coverResource, fallback = "jpg"),
                )
            }.getOrNull()
        }

        val assets = buildList {
            add(
                ParsedAsset(
                    assetRole = AssetRole.PRIMARY_TEXT,
                    bytes = bytes,
                    mime = "application/epub+zip",
                    extension = "epub",
                )
            )
            if (coverAsset != null) {
                add(coverAsset)
            }
        }

        val filenameBase = name
            .substringAfterLast('/')
            .substringAfterLast('\\')
            .substringBeforeLast('.')
            .trim()

        return ParsedLocalBook(
            title = resolveTitle(
                filenameBase = filenameBase,
                opfTitle = opfMetadata?.title,
                epublibTitle = epubBook?.title,
            ),
            author = resolveAuthor(
                filenameBase = filenameBase,
                opfCreator = opfMetadata?.creator,
                epublibAuthors = epubBook?.metadata?.authors.orEmpty(),
            ),
            format = BookFormat.EPUB,
            assets = assets,
        )
    }

    private fun validateReadableEpub(book: Book) {
        val hasReadableCatalog = runCatching { epubCatalogParser.catalog(book).isNotEmpty() }.getOrDefault(false)
        if (hasReadableCatalog) {
            return
        }

        val hasHtmlLikeResources = book.resources.getAll().any(::isHtmlLike)
        if (!hasHtmlLikeResources) {
            throw IllegalStateException("EPUB 文件无效或已损坏")
        }

        throw IllegalStateException("EPUB 文件结构不完整，暂时无法读取")
    }

    private fun isHtmlLike(resource: Resource): Boolean {
        val mediaType = resource.mediaType?.name?.lowercase().orEmpty()
        if (mediaType == "application/xhtml+xml" || mediaType == "text/html") {
            return true
        }
        val href = resource.href?.substringBefore('#')?.lowercase().orEmpty()
        return href.endsWith(".xhtml") || href.endsWith(".html") || href.endsWith(".htm")
    }

    private fun resolveTitle(
        filenameBase: String,
        opfTitle: String?,
        epublibTitle: String?,
    ): String {
        val fromOpf = opfTitle?.trim().takeIf { !it.isNullOrBlank() }
        if (fromOpf != null) return fromOpf

        val fromEpublib = epublibTitle?.trim().takeIf { !it.isNullOrBlank() }
        if (fromEpublib != null) return fromEpublib

        return filenameBase.ifBlank { "未命名" }
    }

    private fun resolveAuthor(
        filenameBase: String,
        opfCreator: String?,
        epublibAuthors: List<nl.siegmann.epublib.domain.Author>,
    ): String? {
        val fromOpf = opfCreator?.trim().takeIf { !it.isNullOrBlank() }
        if (fromOpf != null) return fromOpf

        val fromEpublib = epublibAuthors
            .mapNotNull(::formatAuthor)
            .joinToString(" / ")
            .trim()
            .takeIf { it.isNotBlank() }
        if (fromEpublib != null) return fromEpublib

        return parseAuthorFromFilename(filenameBase)
    }

    private fun formatAuthor(author: nl.siegmann.epublib.domain.Author): String? {
        val first = author.firstname?.trim().takeUnless { it.isNullOrBlank() }
        val last = author.lastname?.trim().takeUnless { it.isNullOrBlank() }
        val combined = listOfNotNull(first, last).joinToString(" ").trim()
        if (combined.isNotBlank()) return combined

        // Some EPUBs only populate one field, and epublib's toString() may include "null".
        val raw = author.toString()
            .replace("null", "", ignoreCase = true)
            .replace(",", " ")
            .trim()
            .replace(Regex("\\s+"), " ")
        return raw.takeIf { it.isNotBlank() }
    }

    private fun parseAuthorFromFilename(filenameBase: String): String? {
        // Very small heuristic: "作者 - 书名" or "书名 - 作者"
        val parts = filenameBase.split(" - ", " — ", " – ", "-", "—", "–")
            .map { it.trim() }
            .filter { it.isNotBlank() }
        if (parts.size < 2) return null

        // If one side looks much shorter, treat it as author.
        val left = parts.first()
        val right = parts.last()
        val leftIsAuthor = left.length in 2..20 && right.length > left.length
        val rightIsAuthor = right.length in 2..20 && left.length > right.length
        return when {
            leftIsAuthor -> left
            rightIsAuthor -> right
            else -> null
        }
    }

    private fun extractOpfMetadata(epubBytes: ByteArray): OpfMetadata? {
        val containerXml = readZipEntry(epubBytes, "META-INF/container.xml")
            ?.decodeToString()
            ?.trim()
            .orEmpty()
        if (containerXml.isBlank()) return null

        val opfPath = Regex("(?is)full-path\\s*=\\s*['\\\"]([^'\\\"]+)['\\\"]")
            .find(containerXml)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
            .orEmpty()
        if (opfPath.isBlank()) return null

        val opfXml = readZipEntry(epubBytes, opfPath)
            ?.decodeToString()
            ?.trim()
            .orEmpty()
        if (opfXml.isBlank()) return null

        fun extract(tagRegex: Regex): String? =
            tagRegex.find(opfXml)
                ?.groupValues
                ?.getOrNull(1)
                ?.let(::cleanupXmlText)
                ?.takeIf { it.isNotBlank() }

        val title = extract(Regex("(?is)<dc:title\\b[^>]*>(.*?)</dc:title>"))
        val creator = extract(Regex("(?is)<dc:creator\\b[^>]*>(.*?)</dc:creator>"))
        if (title.isNullOrBlank() && creator.isNullOrBlank()) return null
        return OpfMetadata(title = title, creator = creator)
    }

    private fun cleanupXmlText(value: String): String {
        val withoutCdata = value
            .replace(Regex("(?is)<!\\[CDATA\\[(.*?)\\]\\]>"), "$1")
        val withoutTags = withoutCdata.replace(Regex("(?s)<[^>]+>"), " ")
        return withoutTags
            .replace("&nbsp;", " ")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun readZipEntry(epubBytes: ByteArray, entryName: String): ByteArray? {
        return runCatching {
            ZipInputStream(ByteArrayInputStream(epubBytes)).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (entry.name == entryName) {
                        return zip.readBytes()
                    }
                }
            }
            null
        }.getOrNull()
    }

    private fun resolveExtension(resource: Resource, fallback: String): String {
        val mediaTypeExtension = resource.mediaType?.defaultExtension
            ?.trimStart('.')
            ?.takeIf { it.isNotBlank() }
        if (mediaTypeExtension != null) {
            return mediaTypeExtension
        }

        val hrefExtension = resource.href
            .substringBefore('#')
            .substringAfterLast('.', "")
            .takeIf { it.isNotBlank() }
        return hrefExtension ?: fallback
    }

    private data class OpfMetadata(
        val title: String?,
        val creator: String?,
    )
}
