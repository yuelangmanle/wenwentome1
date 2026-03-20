package com.wenwentome.reader.data.localbooks

import com.wenwentome.reader.core.model.AssetRole
import com.wenwentome.reader.core.model.BookFormat
import nl.siegmann.epublib.domain.Resource
import nl.siegmann.epublib.epub.EpubReader
import java.io.ByteArrayInputStream
import java.io.InputStream

class EpubBookParser {
    fun parse(name: String, inputStream: InputStream): ParsedLocalBook {
        val bytes = inputStream.readBytes()
        val epubBook = EpubReader().readEpub(ByteArrayInputStream(bytes))
        val coverAsset = epubBook.coverImage?.let { coverResource ->
            ParsedAsset(
                assetRole = AssetRole.COVER,
                bytes = coverResource.data,
                mime = coverResource.mediaType?.name ?: "image/jpeg",
                extension = resolveExtension(coverResource, fallback = "jpg"),
            )
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

        return ParsedLocalBook(
            title = epubBook.title ?: name.removeSuffix(".epub"),
            author = epubBook.metadata.authors.firstOrNull()?.toString(),
            format = BookFormat.EPUB,
            assets = assets,
        )
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
}
