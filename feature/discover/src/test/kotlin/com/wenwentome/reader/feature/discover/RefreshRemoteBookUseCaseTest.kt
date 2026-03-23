package com.wenwentome.reader.feature.discover

import com.wenwentome.reader.bridge.source.SourceBridgeErrorCode
import com.wenwentome.reader.bridge.source.SourceBridgeException
import com.wenwentome.reader.bridge.source.model.RemoteBookDetail
import com.wenwentome.reader.bridge.source.model.RemoteChapter
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
        assertEquals("source-1", result.activeSourceId)
        assertEquals("remote-1", result.activeRemoteBookId)
        assertEquals("https://example.com/books/1", result.activeRemoteBookUrl)
        assertEquals(false, result.autoSwitched)
        assertEquals("source-1", result.primarySourceId)
        assertEquals(false, result.primarySourceFailed)

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
        assertEquals("source-1", result.activeSourceId)
        assertEquals("remote-1", result.activeRemoteBookId)
        assertEquals("https://example.com/books/1", result.activeRemoteBookUrl)
        assertEquals(false, result.autoSwitched)
        assertEquals("source-1", result.primarySourceId)
        assertEquals(false, result.primarySourceFailed)

        val updatedBinding = remoteBindingDao.bindings.getValue("book-1")
        assertEquals("chapter-2", updatedBinding.latestKnownChapterRef)
        // 即便解析失败，也算一次用户手动刷新，仍然要更新 refresh timestamp
        assertEquals(999L, updatedBinding.lastCatalogRefreshAt)
    }

    @Test
    fun refreshRemoteBook_switchesSourceWhenPrimarySourceFails() = runTest {
        val remoteBindingDao = FakeRemoteBindingDao()
        val readingStateDao = FakeReadingStateDao()
        val useCase = RefreshRemoteBookUseCase(
            sourceBridgeRepository = SwitchingSourceBridgeRepository(
                searchResults = listOf(
                    RemoteSearchResult(
                        id = "remote-backup",
                        sourceId = "backup-source",
                        title = "雪中悍刀行",
                        detailUrl = "https://backup.example.com/books/1",
                    ),
                ),
                detailByKey = mapOf(
                    "source-1|remote-1" to RemoteBookDetail(
                        title = "雪中悍刀行",
                        lastChapter = "第三章",
                    ),
                    "backup-source|remote-backup" to RemoteBookDetail(
                        title = "雪中悍刀行",
                        lastChapter = "第三章",
                    ),
                ),
                tocByKey = mapOf(
                    "backup-source|remote-backup" to listOf(
                        RemoteChapter(chapterRef = "chapter-1", title = "第一章"),
                        RemoteChapter(chapterRef = "chapter-2", title = "第二章"),
                        RemoteChapter(chapterRef = "chapter-3", title = "第三章"),
                    ),
                ),
                tocErrorsBySource = mapOf(
                    "source-1" to SourceBridgeException(
                        code = SourceBridgeErrorCode.REQUEST_FAILED,
                        message = "Toc failed for sourceId=source-1",
                    ),
                ),
            ),
            remoteBindingDao = remoteBindingDao,
            readingStateDao = readingStateDao,
            now = { 888L },
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

        assertEquals("backup-source", result.activeSourceId)
        assertEquals("remote-backup", result.activeRemoteBookId)
        assertEquals("https://backup.example.com/books/1", result.activeRemoteBookUrl)
        assertEquals(true, result.autoSwitched)
        assertEquals(true, result.primarySourceFailed)
        assertEquals("chapter-3", result.latestKnownChapterRef)
        assertEquals(true, result.hasUpdates)

        val updatedBinding = remoteBindingDao.bindings.getValue("book-1")
        assertEquals("backup-source", updatedBinding.sourceId)
        assertEquals("remote-backup", updatedBinding.remoteBookId)
        assertEquals("https://backup.example.com/books/1", updatedBinding.remoteBookUrl)
        assertEquals("chapter-1", updatedBinding.tocRef)
        assertEquals(888L, updatedBinding.lastCatalogRefreshAt)
    }

    @Test
    fun refreshRemoteBook_doesNotFallbackWhenPrimarySourceIsUnsupported() = runTest {
        val remoteBindingDao = FakeRemoteBindingDao()
        val readingStateDao = FakeReadingStateDao()
        val useCase = RefreshRemoteBookUseCase(
            sourceBridgeRepository = SwitchingSourceBridgeRepository(
                searchResults = listOf(
                    RemoteSearchResult(
                        id = "remote-backup",
                        sourceId = "backup-source",
                        title = "雪中悍刀行",
                        detailUrl = "https://backup.example.com/books/1",
                    ),
                ),
                detailByKey = emptyMap(),
                tocByKey = emptyMap(),
                detailErrorsBySource = mapOf(
                    "source-1" to SourceBridgeException(
                        code = SourceBridgeErrorCode.UNSUPPORTED_RULE_KIND,
                        message = "JS_TEMPLATE",
                    ),
                ),
            ),
            remoteBindingDao = remoteBindingDao,
            readingStateDao = readingStateDao,
            now = { 777L },
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

        val error = try {
            useCase("book-1")
            error("expected unsupported error")
        } catch (exception: SourceBridgeException) {
            exception
        }

        assertEquals(SourceBridgeErrorCode.UNSUPPORTED_RULE_KIND, error.code)
        val binding = remoteBindingDao.bindings.getValue("book-1")
        assertEquals("source-1", binding.sourceId)
        assertEquals("remote-1", binding.remoteBookId)
        assertEquals("https://example.com/books/1", binding.remoteBookUrl)
        assertEquals(null, binding.lastCatalogRefreshAt)
    }
}

private class SwitchingSourceBridgeRepository(
    private val searchResults: List<RemoteSearchResult>,
    private val detailByKey: Map<String, RemoteBookDetail>,
    private val tocByKey: Map<String, List<RemoteChapter>>,
    private val detailErrorsBySource: Map<String, Throwable> = emptyMap(),
    private val tocErrorsBySource: Map<String, Throwable> = emptyMap(),
) : com.wenwentome.reader.bridge.source.SourceBridgeRepository {
    override suspend fun search(query: String, sourceIds: List<String>): List<RemoteSearchResult> =
        searchResults

    override suspend fun fetchBookDetail(sourceId: String, remoteBookId: String): RemoteBookDetail {
        detailErrorsBySource[sourceId]?.let { throw it }
        return detailByKey.getValue("$sourceId|$remoteBookId")
    }

    override suspend fun fetchToc(sourceId: String, remoteBookId: String): List<RemoteChapter> {
        tocErrorsBySource[sourceId]?.let { throw it }
        return tocByKey.getValue("$sourceId|$remoteBookId")
    }

    override suspend fun fetchChapterContent(sourceId: String, chapterRef: String) =
        throw IllegalStateException("Not needed in this test")
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

    override fun observeAll(): Flow<List<ReadingStateEntity>> =
        statesFlow.map { it.values.toList() }

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
