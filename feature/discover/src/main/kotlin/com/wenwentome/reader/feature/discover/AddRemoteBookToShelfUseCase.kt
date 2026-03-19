package com.wenwentome.reader.feature.discover

import com.wenwentome.reader.bridge.source.SourceBridgeRepository
import com.wenwentome.reader.bridge.source.model.RemoteSearchResult
import com.wenwentome.reader.core.database.dao.BookRecordDao
import com.wenwentome.reader.core.database.dao.RemoteBindingDao
import com.wenwentome.reader.core.database.toEntity
import com.wenwentome.reader.core.model.BookFormat
import com.wenwentome.reader.core.model.BookRecord
import com.wenwentome.reader.core.model.OriginType
import com.wenwentome.reader.core.model.RemoteBinding
import java.util.UUID

fun interface AddRemoteBookToShelf {
    suspend operator fun invoke(result: RemoteSearchResult)
}

class AddRemoteBookToShelfUseCase(
    private val sourceBridgeRepository: SourceBridgeRepository,
    private val bookRecordDao: BookRecordDao,
    private val remoteBindingDao: RemoteBindingDao,
) : AddRemoteBookToShelf {
    override suspend fun invoke(result: RemoteSearchResult) {
        val detail = sourceBridgeRepository.fetchBookDetail(result.sourceId, result.id)
        val toc = sourceBridgeRepository.fetchToc(result.sourceId, result.id)
        val bookId = UUID.randomUUID().toString()
        val book = BookRecord(
            id = bookId,
            title = detail.title.ifBlank { result.title },
            author = detail.author ?: result.author,
            originType = OriginType.WEB,
            primaryFormat = BookFormat.WEB,
            cover = detail.coverUrl,
            summary = detail.summary,
        )
        bookRecordDao.upsert(book.toEntity())
        remoteBindingDao.upsert(
            RemoteBinding(
                bookId = book.id,
                sourceId = result.sourceId,
                remoteBookId = result.id,
                remoteBookUrl = result.detailUrl,
                tocRef = toc.firstOrNull()?.chapterRef,
            ).toEntity()
        )
    }
}
