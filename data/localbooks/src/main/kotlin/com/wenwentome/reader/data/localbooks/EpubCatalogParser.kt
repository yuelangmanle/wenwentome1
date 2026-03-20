package com.wenwentome.reader.data.localbooks

import com.wenwentome.reader.core.model.BookFormat
import com.wenwentome.reader.core.model.ReaderChapter
import nl.siegmann.epublib.domain.Book
import nl.siegmann.epublib.domain.Resource
import nl.siegmann.epublib.domain.SpineReference
import nl.siegmann.epublib.domain.TOCReference
import java.util.LinkedHashMap

class EpubCatalogParser {
    fun firstReadableChapter(book: Book): ReaderChapter =
        catalog(book).firstOrNull() ?: error("EPUB has no readable chapters")

    fun catalog(book: Book): List<ReaderChapter> =
        resolveReadableEntries(book).mapIndexed { index, entry ->
            val chapterRef = normalizeHref(entry.resource.href)
            ReaderChapter(
                chapterRef = chapterRef,
                title = resolveChapterTitle(entry, index),
                orderIndex = index,
                sourceType = BookFormat.EPUB,
                locatorHint = "chapter:$chapterRef#paragraph:0",
            )
        }

    private fun resolveReadableEntries(book: Book): List<ReadableEntry> {
        val tocEntries = resolveReadableEntriesFromToc(book)
        if (tocEntries.isNotEmpty()) {
            return tocEntries
        }
        val navEntries = resolveReadableEntriesFromNav(book)
        if (navEntries.isNotEmpty()) {
            return navEntries
        }
        return resolveReadableEntriesFromSpine(book)
    }

    private fun resolveReadableEntriesFromToc(book: Book): List<ReadableEntry> {
        val topLevelReferences = book.tableOfContents?.tocReferences
            .orEmpty()
            .filterIsInstance<TOCReference>()

        val entriesByHref = LinkedHashMap<String, ReadableEntry>()
        collectTocResources(topLevelReferences, entriesByHref, book)
        return entriesByHref.values.toList()
    }

    private fun collectTocResources(
        references: List<TOCReference>,
        entriesByHref: LinkedHashMap<String, ReadableEntry>,
        book: Book,
    ) {
        references.forEach { tocReference ->
            val resource = tocReference.resource
            if (resource != null && isReadableResource(resource, book)) {
                val href = normalizeHref(resource.href)
                if (href.isNotBlank()) {
                    entriesByHref.putIfAbsent(
                        href,
                        ReadableEntry(
                            resource = resource,
                            title = tocReference.title,
                        )
                    )
                }
            }
            val children = tocReference.children.filterIsInstance<TOCReference>()
            if (children.isNotEmpty()) {
                collectTocResources(children, entriesByHref, book)
            }
        }
    }

    private fun resolveReadableEntriesFromNav(book: Book): List<ReadableEntry> {
        val navResource = resolveNavResource(book) ?: return emptyList()
        val navHtml = runCatching {
            navResource.inputStream.use { inputStream ->
                inputStream.readBytes().decodeToString()
            }
        }.getOrNull() ?: return emptyList()

        val navBlocks = extractTocNavBlocks(navHtml)
        if (navBlocks.isEmpty()) {
            return emptyList()
        }

        val entriesByHref = LinkedHashMap<String, ReadableEntry>()
        navBlocks.forEach { block ->
            ANCHOR_TAG_REGEX.findAll(block).forEach { anchor ->
                val href = anchor.groupValues[1].trim()
                if (href.isBlank()) {
                    return@forEach
                }
                val title = stripTags(anchor.groupValues[2]).takeIf { it.isNotBlank() }
                val resource = resolveResourceFromNavHref(book, navResource.href, href) ?: return@forEach
                if (!isReadableResource(resource, book)) {
                    return@forEach
                }
                val normalizedHref = normalizeHref(resource.href)
                if (normalizedHref.isBlank()) {
                    return@forEach
                }
                entriesByHref.putIfAbsent(
                    normalizedHref,
                    ReadableEntry(
                        resource = resource,
                        title = title,
                    )
                )
            }
        }
        return entriesByHref.values.toList()
    }

    private fun resolveReadableEntriesFromSpine(book: Book): List<ReadableEntry> {
        val entriesByHref = LinkedHashMap<String, ReadableEntry>()
        val spineReferences = book.spine?.spineReferences
            .orEmpty()
            .filterIsInstance<SpineReference>()

        spineReferences.forEach { spineReference ->
            val resource = spineReference.resource ?: return@forEach
            if (!spineReference.isLinear) {
                return@forEach
            }
            if (!isReadableResource(resource, book)) {
                return@forEach
            }
            val href = normalizeHref(resource.href)
            if (href.isNotBlank()) {
                entriesByHref.putIfAbsent(
                    href,
                    ReadableEntry(
                        resource = resource,
                        title = null,
                    )
                )
            }
        }
        return entriesByHref.values.toList()
    }

    private fun resolveChapterTitle(entry: ReadableEntry, index: Int): String {
        val tocTitle = entry.title?.trim().takeUnless { it.isNullOrEmpty() }
        if (tocTitle != null) {
            return tocTitle
        }
        return entry.resource.title?.trim().takeUnless { it.isNullOrEmpty() } ?: "章节 ${index + 1}"
    }

    private fun isReadableResource(resource: Resource, book: Book): Boolean {
        if (!isHtmlLike(resource)) {
            return false
        }

        val resourceHref = normalizeHref(resource.href)
        val coverImageHref = book.coverImage?.href?.let(::normalizeHref)
        val coverPageHref = book.coverPage?.href?.let(::normalizeHref)
        if (resourceHref == coverImageHref || resourceHref == coverPageHref) {
            return false
        }

        val fileName = resourceHref.substringAfterLast('/').substringBeforeLast('.').lowercase()
        if (fileName == "cover" || fileName == "nav" || fileName == "toc") {
            return false
        }
        return true
    }

    private fun isHtmlLike(resource: Resource): Boolean {
        val mediaType = resource.mediaType?.name?.lowercase().orEmpty()
        if (mediaType == "application/xhtml+xml" || mediaType == "text/html") {
            return true
        }

        val href = resource.href?.substringBefore('#')?.lowercase().orEmpty()
        return href.endsWith(".xhtml") || href.endsWith(".html") || href.endsWith(".htm")
    }

    private fun resolveNavResource(book: Book): Resource? {
        val htmlResources = book.resources.getAll().filter { resource -> isHtmlLike(resource) }
        if (htmlResources.isEmpty()) {
            return null
        }

        val obviousNavResources = htmlResources.filter(::isObviousNavResource)
        val remainingResources = htmlResources.filterNot(::isObviousNavResource)
        val candidates = obviousNavResources + remainingResources

        return candidates.firstOrNull(::containsTocNavBlock)
    }

    private fun extractTocNavBlocks(html: String): List<String> {
        return TOC_NAV_BLOCK_REGEX.findAll(html).map { it.value }.toList()
    }

    private fun resolveResourceFromNavHref(book: Book, navHref: String?, navLinkHref: String): Resource? {
        val normalizedLinkHref = normalizeHref(navLinkHref)
        if (normalizedLinkHref.isBlank()) {
            return null
        }
        book.resources.getByHref(normalizedLinkHref)?.let { return it }

        val resolvedRelativeHref = resolveRelativeHref(
            baseHref = normalizeHref(navHref),
            relativeHref = normalizedLinkHref,
        )
        if (resolvedRelativeHref.isBlank()) {
            return null
        }
        return book.resources.getByHref(resolvedRelativeHref)
    }

    private fun resolveRelativeHref(baseHref: String, relativeHref: String): String {
        if (relativeHref.startsWith("/")) {
            return relativeHref.removePrefix("/")
        }
        val baseDirectory = baseHref.substringBeforeLast('/', missingDelimiterValue = "")
        if (baseDirectory.isBlank()) {
            return normalizePath(relativeHref)
        }
        return normalizePath("$baseDirectory/$relativeHref")
    }

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

    private fun stripTags(value: String): String {
        val withoutTags = value.replace(Regex("(?s)<[^>]+>"), " ")
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

    private fun isObviousNavResource(resource: Resource): Boolean {
        val id = resource.id?.trim()?.lowercase().orEmpty()
        val fileName = normalizeHref(resource.href).substringAfterLast('/').substringBeforeLast('.').lowercase()
        return id == "nav" || fileName == "nav"
    }

    private fun containsTocNavBlock(resource: Resource): Boolean {
        val content = runCatching {
            resource.inputStream.use { inputStream ->
                inputStream.readBytes().decodeToString()
            }
        }.getOrNull() ?: return false
        return TOC_NAV_BLOCK_REGEX.containsMatchIn(content)
    }

    private fun normalizeHref(href: String?): String = href?.substringBefore('#').orEmpty()

    private companion object {
        private val TOC_NAV_BLOCK_REGEX = Regex(
            "(?is)<nav\\b[^>]*(?:epub:type|type|role)\\s*=\\s*['\"][^'\"]*(?:toc|doc-toc)[^'\"]*['\"][^>]*>.*?</nav>",
        )
        private val ANCHOR_TAG_REGEX = Regex("(?is)<a\\b[^>]*href\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>(.*?)</a>")
    }

    private data class ReadableEntry(
        val resource: Resource,
        val title: String?,
    )
}
