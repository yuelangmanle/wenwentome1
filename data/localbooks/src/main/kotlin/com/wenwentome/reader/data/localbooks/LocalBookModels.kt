package com.wenwentome.reader.data.localbooks

import com.wenwentome.reader.core.model.AssetRole
import com.wenwentome.reader.core.model.BookAsset
import com.wenwentome.reader.core.model.BookFormat
import com.wenwentome.reader.core.model.BookRecord
import com.wenwentome.reader.core.model.ReadingState

data class ParsedLocalBook(
    val title: String,
    val author: String?,
    val format: BookFormat,
    val assets: List<ParsedAsset>,
)

data class ParsedAsset(
    val assetRole: AssetRole,
    val bytes: ByteArray,
    val mime: String,
    val extension: String,
)

data class ImportedLocalBook(
    val book: BookRecord,
    val assets: List<BookAsset>,
    val readingState: ReadingState,
)

data class ReaderContent(
    val chapterTitle: String,
    val paragraphs: List<String>,
    val chapterRef: String? = null,
)

data class EpubLocator(
    val chapterRef: String,
    val paragraphIndex: Int,
) {
    fun encode(): String = "chapter:$chapterRef#paragraph:$paragraphIndex"
}
