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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

        val firstBookId = useCase(result)
        val secondBookId = useCase(result)

        assertEquals(1, bookRecordDao.records.size)
        assertEquals(1, remoteBindingDao.bindings.size)
        assertEquals(firstBookId, secondBookId)
    }

    @Test
    fun addRemoteBookToShelfStoresLatestKnownChapterRef() = runTest {
        val bookRecordDao = FakeBookRecordDao()
        val remoteBindingDao = FakeRemoteBindingDao()
        val useCase = AddRemoteBookToShelfUseCase(
            sourceBridgeRepository = FakeSourceBridgeRepository(
                detail = RemoteBookDetail(
                    title = "雪中悍刀行",
                    author = "烽火戏诸侯",
                    summary = "北凉刀，江湖雪。",
                    coverUrl = "https://example.com/cover.jpg",
                    lastChapter = "第三章",
                ),
                toc = listOf(
                    RemoteChapter(chapterRef = "chapter-1", title = "第一章"),
                    RemoteChapter(chapterRef = "chapter-2", title = "第二章"),
                    RemoteChapter(chapterRef = "chapter-3", title = "第三章"),
                ),
            ),
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

        val returnedBookId = useCase(result)

        val bookId = bookRecordDao.records.values.single().id
        val binding = remoteBindingDao.bindings.getValue(bookId)
        assertEquals(bookId, returnedBookId)
        // 目录定位仍然是“第一章”，不能偷换成最新章
        assertEquals("chapter-1", binding.tocRef)
        // 新增的 metadata: latestKnownChapterRef 应写入最新章
        assertEquals("chapter-3", binding.latestKnownChapterRef)
        // add-to-shelf 不应写 lastCatalogRefreshAt
        assertNull(binding.lastCatalogRefreshAt)
    }

    @Test
    fun addRemoteBookToShelf_matchesLastChapterAfterTitleNormalization() = runTest {
        val bookRecordDao = FakeBookRecordDao()
        val remoteBindingDao = FakeRemoteBindingDao()
        val useCase = AddRemoteBookToShelfUseCase(
            sourceBridgeRepository = FakeSourceBridgeRepository(
                detail = RemoteBookDetail(
                    title = "雪中悍刀行",
                    author = "烽火戏诸侯",
                    summary = "北凉刀，江湖雪。",
                    coverUrl = "https://example.com/cover.jpg",
                    // 常见噪音：前缀 + 全角冒号 + 全角空格/多余空白
                    lastChapter = "最新章节：　 第三章　",
                ),
                toc = listOf(
                    RemoteChapter(chapterRef = "chapter-1", title = "第一章"),
                    RemoteChapter(chapterRef = "chapter-2", title = "第二章"),
                    RemoteChapter(chapterRef = "chapter-3", title = "第三章"),
                ),
            ),
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

        val bookId = bookRecordDao.records.values.single().id
        val binding = remoteBindingDao.bindings.getValue(bookId)
        assertEquals("chapter-1", binding.tocRef)
        assertEquals("chapter-3", binding.latestKnownChapterRef)
        assertNull(binding.lastCatalogRefreshAt)
    }

    @Test
    fun addRemoteBookToShelf_returnsNullLatestKnownChapterRefWhenLastChapterNotInToc() = runTest {
        val bookRecordDao = FakeBookRecordDao()
        val remoteBindingDao = FakeRemoteBindingDao()
        val useCase = AddRemoteBookToShelfUseCase(
            sourceBridgeRepository = FakeSourceBridgeRepository(
                detail = RemoteBookDetail(
                    title = "雪中悍刀行",
                    author = "烽火戏诸侯",
                    summary = "北凉刀，江湖雪。",
                    coverUrl = "https://example.com/cover.jpg",
                    // 详情页有 lastChapter，但 TOC 并不包含
                    lastChapter = "不存在的章节",
                ),
                toc = listOf(
                    RemoteChapter(chapterRef = "chapter-1", title = "第一章"),
                    RemoteChapter(chapterRef = "chapter-2", title = "第二章"),
                    RemoteChapter(chapterRef = "chapter-3", title = "第三章"),
                ),
            ),
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

        val bookId = bookRecordDao.records.values.single().id
        val binding = remoteBindingDao.bindings.getValue(bookId)
        assertEquals("chapter-1", binding.tocRef)
        // 有 lastChapter 但无法匹配时，不要硬猜 toc.last()
        assertNull(binding.latestKnownChapterRef)
        assertNull(binding.lastCatalogRefreshAt)
    }
}

internal class FakeSourceBridgeRepository(
    private val detail: RemoteBookDetail = RemoteBookDetail(
        title = "雪中悍刀行",
        author = "烽火戏诸侯",
        summary = "北凉刀，江湖雪。",
        coverUrl = "https://example.com/cover.jpg",
    ),
    private val toc: List<RemoteChapter> = listOf(RemoteChapter(chapterRef = "chapter-1", title = "第一章")),
) : SourceBridgeRepository {
    override suspend fun search(query: String, sourceIds: List<String>): List<RemoteSearchResult> = emptyList()

    override suspend fun fetchBookDetail(sourceId: String, remoteBookId: String): RemoteBookDetail =
        detail

    override suspend fun fetchToc(sourceId: String, remoteBookId: String): List<RemoteChapter> =
        toc

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

internal class FakeRemoteBindingDao : RemoteBindingDao {
    val bindings = linkedMapOf<String, RemoteBindingEntity>()
    private val bindingsFlow = MutableStateFlow<Map<String, RemoteBindingEntity>>(emptyMap())

    override suspend fun upsert(entity: RemoteBindingEntity) {
        bindings[entity.bookId] = entity
        bindingsFlow.value = bindings.toMap()
    }

    override suspend fun upsertAll(entities: List<RemoteBindingEntity>) {
        for (entity in entities) {
            upsert(entity)
        }
    }

    override fun observeByBookId(bookId: String): Flow<RemoteBindingEntity?> =
        bindingsFlow.map { it[bookId] }

    override fun observeAll(): Flow<List<RemoteBindingEntity>> =
        bindingsFlow.map { it.values.toList() }

    override suspend fun getByRemoteBook(sourceId: String, remoteBookId: String): RemoteBindingEntity? =
        bindings.values.firstOrNull { it.sourceId == sourceId && it.remoteBookId == remoteBookId }

    override suspend fun getAll(): List<RemoteBindingEntity> = bindings.values.toList()

    override suspend fun clearAll() {
        bindings.clear()
        bindingsFlow.value = emptyMap()
    }
}
