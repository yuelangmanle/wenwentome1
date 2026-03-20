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

    private fun normalizeHref(href: String?): String = href?.substringBefore('#').orEmpty()

    private data class ReadableEntry(
        val resource: Resource,
        val title: String?,
    )
}
