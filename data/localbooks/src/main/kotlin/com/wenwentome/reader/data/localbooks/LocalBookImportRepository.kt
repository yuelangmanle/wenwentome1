package com.wenwentome.reader.data.localbooks

import com.wenwentome.reader.core.database.dao.BookAssetDao
import com.wenwentome.reader.core.database.dao.BookRecordDao
import com.wenwentome.reader.core.database.dao.ReadingStateDao
import com.wenwentome.reader.core.database.toEntity
import com.wenwentome.reader.core.model.BookAsset
import com.wenwentome.reader.core.model.BookRecord
import com.wenwentome.reader.core.model.ReadingState
import java.io.InputStream
import java.security.MessageDigest

class LocalBookImportRepository(
    private val txtParser: TxtBookParser,
    private val epubParser: EpubBookParser,
    private val fileStore: LocalBookFileStore,
    private val bookRecordDao: BookRecordDao,
    private val readingStateDao: ReadingStateDao,
    private val bookAssetDao: BookAssetDao,
) {
    suspend fun import(fileName: String, inputStream: InputStream): ImportedLocalBook {
        val parsed = inputStream.use { stream ->
            when {
                fileName.endsWith(".txt", ignoreCase = true) -> txtParser.parse(fileName, stream.readBytes())
                fileName.endsWith(".epub", ignoreCase = true) -> epubParser.parse(fileName, stream)
                else -> error("Unsupported file format: $fileName")
            }
        }
        val book: BookRecord = BookRecord.newLocal(parsed.title, parsed.author, parsed.format)

        try {
            val assets: List<BookAsset> = parsed.assets.mapIndexed { index, asset ->
                val hash = sha256Hex(asset.bytes)
                val storageUri = fileStore.persistOriginal(
                    bookId = book.id,
                    extension = asset.extension,
                    bytes = asset.bytes,
                )
                BookAsset(
                    bookId = book.id,
                    assetRole = asset.assetRole,
                    storageUri = storageUri,
                    mime = asset.mime,
                    size = asset.bytes.size.toLong(),
                    hash = hash,
                    syncPath = "books/${book.id}/asset-$index.${asset.extension}",
                )
            }

            val readingState: ReadingState = ReadingState.initial(book.id)

            bookRecordDao.upsert(book.toEntity())
            bookAssetDao.upsertAll(assets.map(BookAsset::toEntity))
            readingStateDao.upsert(readingState.toEntity())

            return ImportedLocalBook(
                book = book,
                assets = assets,
                readingState = readingState,
            )
        } catch (error: Throwable) {
            // 当前模块没有可复用的数据库事务边界；至少保证 DAO 写入失败时不会留下孤儿文件。
            runCatching { fileStore.deleteBook(book.id) }
            throw error
        }
    }

    private fun sha256Hex(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { b -> "%02x".format(b.toInt() and 0xff) }
}
