package com.wenwentome.reader.data.localbooks

import com.wenwentome.reader.core.database.dao.BookAssetDao
import com.wenwentome.reader.core.database.dao.BookRecordDao
import com.wenwentome.reader.core.database.dao.ReadingStateDao
import com.wenwentome.reader.core.database.entity.BookAssetEntity
import com.wenwentome.reader.core.database.entity.BookRecordEntity
import com.wenwentome.reader.core.database.entity.ReadingStateEntity
import com.wenwentome.reader.core.model.AssetRole
import com.wenwentome.reader.core.model.BookFormat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.InputStream

class LocalBookImportRepositoryTest {
    @Test
    fun importEpub_createsBookRecordAssetAndInitialReadingState() = runTest {
        val repository = LocalBookImportRepository(
            txtParser = TxtBookParser(),
            epubParser = EpubBookParser(),
            fileStore = fakeLocalBookFileStore(),
            bookRecordDao = fakeBookRecordDao(),
            readingStateDao = fakeReadingStateDao(),
            bookAssetDao = fakeBookAssetDao(),
        )

        val result = repository.import(fileName = "sample.epub", inputStream = fixture("sample.epub"))

        assertEquals(BookFormat.EPUB, result.book.primaryFormat)
        assertTrue(result.assets.isNotEmpty())
        assertEquals(0f, result.readingState.progressPercent)
    }

    private fun fixture(name: String): InputStream =
        requireNotNull(javaClass.classLoader?.getResourceAsStream("fixtures/$name")) {
            "Missing fixture: fixtures/$name"
        }

    private fun fakeLocalBookFileStore(): LocalBookFileStore =
        LocalBookFileStore(filesDir = createTempDir(prefix = "localbooks-test-"))

    private fun fakeBookRecordDao(): BookRecordDao = object : BookRecordDao {
        private val items = MutableStateFlow<Map<String, BookRecordEntity>>(emptyMap())

        override suspend fun upsert(entity: BookRecordEntity) {
            items.value = items.value + (entity.id to entity)
        }

        override suspend fun upsertAll(entities: List<BookRecordEntity>) {
            items.value = items.value + entities.associateBy { it.id }
        }

        override fun observeById(id: String): Flow<BookRecordEntity?> =
            items.asStateFlow().map { map -> map[id] }

        override fun observeAll(): Flow<List<BookRecordEntity>> =
            items.asStateFlow().map { map -> map.values.toList() }

        override suspend fun getAll(): List<BookRecordEntity> = items.value.values.toList()

        override suspend fun clearAll() {
            items.value = emptyMap()
        }

        override suspend fun replaceAll(entities: List<BookRecordEntity>) {
            clearAll()
            upsertAll(entities)
        }
    }

    private fun fakeReadingStateDao(): ReadingStateDao = object : ReadingStateDao {
        private val items = MutableStateFlow<Map<String, ReadingStateEntity>>(emptyMap())

        override suspend fun upsert(entity: ReadingStateEntity) {
            items.value = items.value + (entity.bookId to entity)
        }

        override suspend fun upsertAll(entities: List<ReadingStateEntity>) {
            items.value = items.value + entities.associateBy { it.bookId }
        }

        override fun observeByBookId(bookId: String): Flow<ReadingStateEntity?> =
            items.asStateFlow().map { map -> map[bookId] }

        override suspend fun getAll(): List<ReadingStateEntity> = items.value.values.toList()

        override suspend fun clearAll() {
            items.value = emptyMap()
        }

        override suspend fun replaceAll(entities: List<ReadingStateEntity>) {
            clearAll()
            upsertAll(entities)
        }
    }

    private fun fakeBookAssetDao(): BookAssetDao = object : BookAssetDao {
        private val items = MutableStateFlow<List<BookAssetEntity>>(emptyList())

        override suspend fun upsert(entity: BookAssetEntity) {
            items.value = items.value.filterNot { it.bookId == entity.bookId && it.assetRole == entity.assetRole } + entity
        }

        override suspend fun upsertAll(entities: List<BookAssetEntity>) {
            entities.forEach { upsert(it) }
        }

        override suspend fun findPrimaryAsset(bookId: String): BookAssetEntity? =
            items.value.firstOrNull { it.bookId == bookId && it.assetRole == AssetRole.PRIMARY_TEXT }

        override suspend fun getAll(): List<BookAssetEntity> = items.value.toList()

        override fun observeByBookId(bookId: String): Flow<List<BookAssetEntity>> =
            items.asStateFlow().map { list -> list.filter { it.bookId == bookId } }

        override suspend fun clearAll() {
            items.value = emptyList()
        }

        override suspend fun replaceAll(entities: List<BookAssetEntity>) {
            clearAll()
            upsertAll(entities)
        }
    }
}
