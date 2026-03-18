package com.wenwentome.reader.data.localbooks

import com.wenwentome.reader.core.model.AssetRole
import com.wenwentome.reader.core.model.BookFormat
import nl.siegmann.epublib.epub.EpubReader
import java.io.ByteArrayInputStream
import java.io.InputStream

class EpubBookParser {
    fun parse(name: String, inputStream: InputStream): ParsedLocalBook {
        val bytes = inputStream.readBytes()
        val epubBook = EpubReader().readEpub(ByteArrayInputStream(bytes))
        return ParsedLocalBook(
            title = epubBook.title ?: name.removeSuffix(".epub"),
            author = epubBook.metadata.authors.firstOrNull()?.toString(),
            format = BookFormat.EPUB,
            assets = listOf(
                ParsedAsset(
                    assetRole = AssetRole.PRIMARY_TEXT,
                    bytes = bytes,
                    mime = "application/epub+zip",
                    extension = "epub",
                )
            ),
        )
    }
}

