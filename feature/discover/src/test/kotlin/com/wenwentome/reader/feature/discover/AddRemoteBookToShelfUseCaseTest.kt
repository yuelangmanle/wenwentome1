package com.wenwentome.reader.feature.discover

import com.wenwentome.reader.bridge.source.SourceBridgeRepository
import com.wenwentome.reader.bridge.source.model.RemoteBookDetail
import com.wenwentome.reader.bridge.source.model.RemoteChapter
import com.wenwentome.reader.bridge.source.model.RemoteChapterContent
import com.wenwentome.reader.bridge.source.model.RemoteSearchResult
import com.wenwentome.reader.core.database.dao.BookRecordDao
import com.wenwentome.reader.core.database.dao.RemoteBindingDao
import com.wenwentome.reader.core.database.entity.BookRecordEntity
import com.wenwentome.reader.core.database.entity.RemoteBindingEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AddRemoteBookToShelfUseCaseTest {
    @Test
    fun addingSameRemoteBookTwice_keepsSingleBookshelfEntry() = runTest {
        val bookRecordDao = FakeBookRecordDao()
        val remoteBindingDao = FakeRemoteBindingDao()
        val useCase = AddRemoteBookToShelfUseCase(
            sourceBridgeRepository = FakeSourceBridgeRepository(),
            bookRecordDao = bookRecordDao,
            remoteBindingDao = remoteBindingDao,
        )
        val result = RemoteSearchResult(
            id = "remote-1",
            sourceId = "source-1",
            title = "雪中悍刀行",
            author = "烽火戏诸侯",
            detailUrl = "https://example.com/books/1",
        )

        useCase(result)
        useCase(result)

        assertEquals(1, bookRecordDao.records.size)
        assertEquals(1, remoteBindingDao.bindings.size)
    }
}

private class FakeSourceBridgeRepository : SourceBridgeRepository {
    override suspend fun search(query: String, sourceIds: List<String>): List<RemoteSearchResult> = emptyList()

    override suspend fun fetchBookDetail(sourceId: String, remoteBookId: String): RemoteBookDetail =
        RemoteBookDetail(
            title = "雪中悍刀行",
            author = "烽火戏诸侯",
            summary = "北凉刀，江湖雪。",
            coverUrl = "https://example.com/cover.jpg",
        )

    override suspend fun fetchToc(sourceId: String, remoteBookId: String): List<RemoteChapter> =
        listOf(RemoteChapter(chapterRef = "chapter-1", title = "第一章"))

    override suspend fun fetchChapterContent(sourceId: String, chapterRef: String): RemoteChapterContent =
        RemoteChapterContent(
            chapterRef = chapterRef,
            title = "第一章",
            content = "正文",
        )
}

private class FakeBookRecordDao : BookRecordDao {
    val records = linkedMapOf<String, BookRecordEntity>()

    override suspend fun upsert(entity: BookRecordEntity) {
        records[entity.id] = entity
    }

    override suspend fun upsertAll(entities: List<BookRecordEntity>) {
        for (entity in entities) {
            upsert(entity)
        }
    }

    override fun observeById(id: String): Flow<BookRecordEntity?> = emptyFlow()

    override fun observeAll(): Flow<List<BookRecordEntity>> = emptyFlow()

    override suspend fun getAll(): List<BookRecordEntity> = records.values.toList()

    override suspend fun deleteById(id: String) {
        records.remove(id)
    }

    override suspend fun clearAll() {
        records.clear()
    }
}

private class FakeRemoteBindingDao : RemoteBindingDao {
    val bindings = linkedMapOf<String, RemoteBindingEntity>()

    override suspend fun upsert(entity: RemoteBindingEntity) {
        bindings[entity.bookId] = entity
    }

    override suspend fun upsertAll(entities: List<RemoteBindingEntity>) {
        for (entity in entities) {
            upsert(entity)
        }
    }

    override fun observeByBookId(bookId: String): Flow<RemoteBindingEntity?> = emptyFlow()

    override suspend fun getByRemoteBook(sourceId: String, remoteBookId: String): RemoteBindingEntity? =
        bindings.values.firstOrNull { it.sourceId == sourceId && it.remoteBookId == remoteBookId }

    override suspend fun getAll(): List<RemoteBindingEntity> = bindings.values.toList()

    override suspend fun clearAll() {
        bindings.clear()
    }
}
