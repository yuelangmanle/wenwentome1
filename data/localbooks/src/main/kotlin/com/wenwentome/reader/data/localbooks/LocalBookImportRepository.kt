package com.wenwentome.reader.data.localbooks

import com.wenwentome.reader.core.database.dao.BookAssetDao
import com.wenwentome.reader.core.database.dao.BookRecordDao
import com.wenwentome.reader.core.database.dao.ReadingStateDao
import com.wenwentome.reader.core.database.toEntity
import com.wenwentome.reader.core.model.AssetRole
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
    suspend fun importBatch(requests: List<LocalBookImportRequest>): ImportedLocalBookBatch {
        val importedBooks = mutableListOf<ImportedLocalBook>()
        try {
            requests.forEach { request ->
                importedBooks += importSingle(
                    fileName = request.fileName,
                    inputStream = request.openInputStream(),
                )
            }
        } catch (error: Throwable) {
            importedBooks
                .asReversed()
                .forEach { importedBook ->
                    cleanupImportedBook(importedBook.book.id)
                }
            throw error
        }
        return ImportedLocalBookBatch(books = importedBooks)
    }

    suspend fun import(fileName: String, inputStream: InputStream): ImportedLocalBook =
        importBatch(
            requests = listOf(LocalBookImportRequest(fileName = fileName) { inputStream }),
        ).books.single()

    private suspend fun importSingle(fileName: String, inputStream: InputStream): ImportedLocalBook {
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
                val assetBaseName = resolveAssetBaseName(asset.assetRole, index)
                val storageUri = fileStore.persistOriginal(
                    bookId = book.id,
                    baseName = assetBaseName,
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
                    syncPath = "books/${book.id}/$assetBaseName.${asset.extension}",
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
            cleanupImportedBook(book.id)
            throw error
        }
    }

    private suspend fun cleanupImportedBook(bookId: String) {
        // 当前仓库层没有跨 DAO 的事务封装时，至少清理同一本书的半成品记录与落盘文件。
        runCatching { readingStateDao.deleteByBookId(bookId) }
        runCatching { bookAssetDao.deleteByBookId(bookId) }
        runCatching { bookRecordDao.deleteById(bookId) }
        runCatching { fileStore.deleteBook(bookId) }
    }

    private fun sha256Hex(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { b -> "%02x".format(b.toInt() and 0xff) }

    private fun resolveAssetBaseName(assetRole: AssetRole, index: Int): String =
        when (assetRole) {
            AssetRole.PRIMARY_TEXT -> "source"
            AssetRole.COVER -> "cover"
            AssetRole.CACHE_PACKAGE -> "cache-$index"
        }
}
