package com.wenwentome.reader.feature.discover

import com.wenwentome.reader.bridge.source.SourceBridgeRepository
import com.wenwentome.reader.bridge.source.model.RemoteBookDetail
import com.wenwentome.reader.bridge.source.model.RemoteChapter
import com.wenwentome.reader.bridge.source.model.RemoteChapterContent
import com.wenwentome.reader.bridge.source.model.RemoteSearchResult
import com.wenwentome.reader.core.database.dao.ReadingStateDao
import com.wenwentome.reader.core.database.dao.RemoteBindingDao
import com.wenwentome.reader.core.database.entity.ReadingStateEntity
import com.wenwentome.reader.core.database.entity.RemoteBindingEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RefreshRemoteBookUseCaseTest {
    @Test
    fun refreshRemoteBookMarksHasUpdatesWhenLatestChapterAdvanced() = runTest {
        val remoteBindingDao = FakeRemoteBindingDao()
        val readingStateDao = FakeReadingStateDao()
        val useCase = RefreshRemoteBookUseCase(
            sourceBridgeRepository = FakeSourceBridgeRepository(
                detail = RemoteBookDetail(
                    title = "雪中悍刀行",
                    lastChapter = "第三章",
                ),
                toc = listOf(
                    RemoteChapter(chapterRef = "chapter-1", title = "第一章"),
                    RemoteChapter(chapterRef = "chapter-2", title = "第二章"),
                    RemoteChapter(chapterRef = "chapter-3", title = "第三章"),
                ),
            ),
            remoteBindingDao = remoteBindingDao,
            readingStateDao = readingStateDao,
            now = { 123L },
        )
        remoteBindingDao.upsert(
            RemoteBindingEntity(
                bookId = "book-1",
                sourceId = "source-1",
                remoteBookId = "remote-1",
                remoteBookUrl = "https://example.com/books/1",
                tocRef = "chapter-1",
                latestKnownChapterRef = "chapter-2",
                lastCatalogRefreshAt = null,
            ),
        )
        readingStateDao.upsert(
            ReadingStateEntity(
                bookId = "book-1",
                chapterRef = "chapter-2",
            ),
        )

        val result = useCase("book-1")

        assertEquals("chapter-3", result.latestKnownChapterRef)
        assertEquals(true, result.hasUpdates)

        val updatedBinding = remoteBindingDao.bindings.getValue("book-1")
        assertEquals("chapter-3", updatedBinding.latestKnownChapterRef)
        assertEquals(123L, updatedBinding.lastCatalogRefreshAt)
    }

    @Test
    fun refreshRemoteBook_keepsExistingLatestKnownChapterRefWhenTocIsEmpty() = runTest {
        val remoteBindingDao = FakeRemoteBindingDao()
        val readingStateDao = FakeReadingStateDao()
        val useCase = RefreshRemoteBookUseCase(
            sourceBridgeRepository = FakeSourceBridgeRepository(
                detail = RemoteBookDetail(
                    title = "雪中悍刀行",
                    lastChapter = "第三章",
                ),
                toc = emptyList(),
            ),
            remoteBindingDao = remoteBindingDao,
            readingStateDao = readingStateDao,
            now = { 999L },
        )
        remoteBindingDao.upsert(
            RemoteBindingEntity(
                bookId = "book-1",
                sourceId = "source-1",
                remoteBookId = "remote-1",
                remoteBookUrl = "https://example.com/books/1",
                tocRef = "chapter-1",
                latestKnownChapterRef = "chapter-2",
                lastCatalogRefreshAt = 1L,
            ),
        )
        // readingState 恰好停在已知最新章，刷新失败时不应把 hasUpdates 清空成 false-positive/false-negative。
        readingStateDao.upsert(
            ReadingStateEntity(
                bookId = "book-1",
                chapterRef = "chapter-2",
            ),
        )

        val result = useCase("book-1")

        // TOC 空导致本轮无法解析新的 latest ref，应保留旧值
        assertEquals("chapter-2", result.latestKnownChapterRef)
        assertEquals(false, result.hasUpdates)

        val updatedBinding = remoteBindingDao.bindings.getValue("book-1")
        assertEquals("chapter-2", updatedBinding.latestKnownChapterRef)
        // 即便解析失败，也算一次用户手动刷新，仍然要更新 refresh timestamp
        assertEquals(999L, updatedBinding.lastCatalogRefreshAt)
    }
}

private class FakeSourceBridgeRepository(
    private val detail: RemoteBookDetail,
    private val toc: List<RemoteChapter>,
) : SourceBridgeRepository {
    override suspend fun search(query: String, sourceIds: List<String>): List<RemoteSearchResult> = emptyList()

    override suspend fun fetchBookDetail(sourceId: String, remoteBookId: String): RemoteBookDetail = detail

    override suspend fun fetchToc(sourceId: String, remoteBookId: String): List<RemoteChapter> = toc

    override suspend fun fetchChapterContent(sourceId: String, chapterRef: String): RemoteChapterContent =
        RemoteChapterContent(
            chapterRef = chapterRef,
            title = chapterRef,
            content = "",
        )
}

private class FakeRemoteBindingDao : RemoteBindingDao {
    val bindings = linkedMapOf<String, RemoteBindingEntity>()
    private val bindingsFlow = MutableStateFlow<Map<String, RemoteBindingEntity>>(emptyMap())

    override suspend fun upsert(entity: RemoteBindingEntity) {
        bindings[entity.bookId] = entity
        bindingsFlow.value = bindings.toMap()
    }

    override suspend fun upsertAll(entities: List<RemoteBindingEntity>) {
        for (entity in entities) upsert(entity)
    }

    override fun observeByBookId(bookId: String): Flow<RemoteBindingEntity?> =
        bindingsFlow.map { it[bookId] }

    override suspend fun getByRemoteBook(sourceId: String, remoteBookId: String): RemoteBindingEntity? =
        bindings.values.firstOrNull { it.sourceId == sourceId && it.remoteBookId == remoteBookId }

    override suspend fun getAll(): List<RemoteBindingEntity> = bindings.values.toList()

    override suspend fun clearAll() {
        bindings.clear()
        bindingsFlow.value = emptyMap()
    }
}

private class FakeReadingStateDao : ReadingStateDao {
    private val states = linkedMapOf<String, ReadingStateEntity>()
    private val statesFlow = MutableStateFlow<Map<String, ReadingStateEntity>>(emptyMap())

    override suspend fun upsert(entity: ReadingStateEntity) {
        states[entity.bookId] = entity
        statesFlow.value = states.toMap()
    }

    override suspend fun upsertAll(entities: List<ReadingStateEntity>) {
        for (entity in entities) upsert(entity)
    }

    override fun observeByBookId(bookId: String): Flow<ReadingStateEntity?> =
        statesFlow.map { it[bookId] }

    override suspend fun getAll(): List<ReadingStateEntity> = states.values.toList()

    override suspend fun deleteByBookId(bookId: String) {
        states.remove(bookId)
        statesFlow.value = states.toMap()
    }

    override suspend fun clearAll() {
        states.clear()
        statesFlow.value = emptyMap()
    }
}
