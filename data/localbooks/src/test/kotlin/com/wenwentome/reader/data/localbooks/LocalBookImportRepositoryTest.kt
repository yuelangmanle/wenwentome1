package com.wenwentome.reader.data.localbooks

import com.wenwentome.reader.core.database.dao.BookAssetDao
import com.wenwentome.reader.core.database.dao.BookRecordDao
import com.wenwentome.reader.core.database.dao.ReadingStateDao
import com.wenwentome.reader.core.database.entity.BookAssetEntity
import com.wenwentome.reader.core.database.entity.BookRecordEntity
import com.wenwentome.reader.core.database.entity.ReadingStateEntity
import com.wenwentome.reader.core.model.AssetRole
import com.wenwentome.reader.core.model.BookFormat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.net.URI
import java.util.zip.ZipInputStream

class LocalBookImportRepositoryTest {
    @Test
    fun importEpub_createsBookRecordAssetAndInitialReadingState() = runTest {
        val context = createRepositoryContext()

        val result = context.repository.import(fileName = "sample.epub", inputStream = fixture("sample.epub"))

        assertEquals(BookFormat.EPUB, result.book.primaryFormat)
        assertTrue(result.assets.isNotEmpty())
        assertEquals(0f, result.readingState.progressPercent)
        assertEquals(listOf(result.book.id), context.bookRecordDao.getAll().map { it.id })
        assertEquals(listOf(result.book.id), context.readingStateDao.getAll().map { it.bookId })
        assertEquals(setOf(result.book.id), context.bookAssetDao.getAll().map { it.bookId }.toSet())

        val persistedAsset = context.bookAssetDao.getAll().first { it.assetRole == AssetRole.PRIMARY_TEXT }
        val persistedBytes = context.fileStore.open(persistedAsset.storageUri).use { it.readBytes() }
        assertArrayEquals(fixtureBytes("sample.epub"), persistedBytes)
    }

    @Test
    fun import_whenDaoWriteFails_cleansPersistedFiles() = runTest {
        val context = createRepositoryContext(
            readingStateDao = FakeReadingStateDao(failOnUpsert = true),
        )

        try {
            context.repository.import(fileName = "sample.epub", inputStream = fixture("sample.epub"))
            fail("Expected import to fail when readingStateDao.upsert throws")
        } catch (_: IllegalStateException) {
            // expected
        }

        assertTrue(context.filesDir.walkTopDown().filter(File::isFile).toList().isEmpty())
        assertTrue(context.bookRecordDao.getAll().isEmpty())
        assertTrue(context.bookAssetDao.getAll().isEmpty())
        assertTrue(context.readingStateDao.getAll().isEmpty())
    }

    @Test
    fun import_epubPersistsCoverAssetSeparatelyFromPrimaryText() = runTest {
        val context = createRepositoryContext()

        val result = context.repository.import("sample-cover-first.epub", fixture("sample-cover-first.epub"))

        val coverAsset = result.assets.first { it.assetRole == AssetRole.COVER }
        val primaryAsset = result.assets.first { it.assetRole == AssetRole.PRIMARY_TEXT }
        val coverEntity = context.bookAssetDao.getAll().first { it.assetRole == AssetRole.COVER }
        val primaryEntity = context.bookAssetDao.getAll().first { it.assetRole == AssetRole.PRIMARY_TEXT }

        assertNotEquals(coverAsset.storageUri, primaryAsset.storageUri)

        val coverFile = File(URI(coverEntity.storageUri))
        val primaryFile = File(URI(primaryEntity.storageUri))
        assertTrue(coverFile.exists())
        assertTrue(primaryFile.exists())

        val coverBytes = coverFile.readBytes()
        val primaryBytes = primaryFile.readBytes()
        val expectedCoverBytes = fixtureZipEntryBytes("sample-cover-first.epub", "OEBPS/images/cover.jpg")

        assertArrayEquals(expectedCoverBytes, coverBytes)
        assertArrayEquals(fixtureBytes("sample-cover-first.epub"), primaryBytes)

        assertTrue(coverEntity.mime.startsWith("image/"))
        assertEquals("application/epub+zip", primaryEntity.mime)
        assertNotEquals(coverEntity.size, primaryEntity.size)
    }

    @Test
    fun loadTxt_returnsParagraphsStartingFromLocator() = runTest {
        val context = createRepositoryContext()
        val imported = context.repository.import(fileName = "sample.txt", inputStream = fixture("sample.txt"))
        val contentRepository = LocalBookContentRepository(
            bookAssetDao = context.bookAssetDao,
            fileStore = context.fileStore,
        )

        val content = contentRepository.load(bookId = imported.book.id, locator = "1")

        assertEquals("正文", content.chapterTitle)
        assertEquals(listOf("第二段。", "第三段。"), content.paragraphs)
    }

    private fun fixture(name: String): InputStream =
        requireNotNull(javaClass.classLoader?.getResourceAsStream("fixtures/$name")) {
            "Missing fixture: fixtures/$name"
        }

    private fun fixtureBytes(name: String): ByteArray = fixture(name).use { it.readBytes() }

    private fun fixtureZipEntryBytes(name: String, entryPath: String): ByteArray {
        val bytes = fixtureBytes(name)
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.name == entryPath) {
                    return zip.readBytes()
                }
            }
        }
        error("Missing zip entry $entryPath in fixture $name")
    }

    private fun createRepositoryContext(
        bookRecordDao: FakeBookRecordDao = FakeBookRecordDao(),
        readingStateDao: FakeReadingStateDao = FakeReadingStateDao(),
        bookAssetDao: FakeBookAssetDao = FakeBookAssetDao(),
    ): RepositoryContext {
        val filesDir = createTempDir(prefix = "localbooks-test-")
        val fileStore = LocalBookFileStore(filesDir = filesDir)
        return RepositoryContext(
            filesDir = filesDir,
            fileStore = fileStore,
            bookRecordDao = bookRecordDao,
            readingStateDao = readingStateDao,
            bookAssetDao = bookAssetDao,
            repository = LocalBookImportRepository(
                txtParser = TxtBookParser(),
                epubParser = EpubBookParser(),
                fileStore = fileStore,
                bookRecordDao = bookRecordDao,
                readingStateDao = readingStateDao,
                bookAssetDao = bookAssetDao,
            ),
        )
    }

    private data class RepositoryContext(
        val filesDir: File,
        val fileStore: LocalBookFileStore,
        val bookRecordDao: FakeBookRecordDao,
        val readingStateDao: FakeReadingStateDao,
        val bookAssetDao: FakeBookAssetDao,
        val repository: LocalBookImportRepository,
    )

    private class FakeBookRecordDao : BookRecordDao {
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

        override suspend fun deleteById(id: String) {
            items.value = items.value - id
        }

        override suspend fun replaceAll(entities: List<BookRecordEntity>) {
            clearAll()
            upsertAll(entities)
        }
    }

    private class FakeReadingStateDao(
        private val failOnUpsert: Boolean = false,
    ) : ReadingStateDao {
        private val items = MutableStateFlow<Map<String, ReadingStateEntity>>(emptyMap())

        override suspend fun upsert(entity: ReadingStateEntity) {
            if (failOnUpsert) {
                throw IllegalStateException("Injected reading state failure")
            }
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

        override suspend fun deleteByBookId(bookId: String) {
            items.value = items.value - bookId
        }

        override suspend fun replaceAll(entities: List<ReadingStateEntity>) {
            clearAll()
            upsertAll(entities)
        }
    }

    private class FakeBookAssetDao : BookAssetDao {
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

        override suspend fun findByRole(bookId: String, assetRole: AssetRole): BookAssetEntity? =
            items.value.firstOrNull { it.bookId == bookId && it.assetRole == assetRole }

        override suspend fun clearAll() {
            items.value = emptyList()
        }

        override suspend fun deleteByBookId(bookId: String) {
            items.value = items.value.filterNot { it.bookId == bookId }
        }

        override suspend fun deleteByRole(bookId: String, assetRole: AssetRole) {
            items.value = items.value.filterNot { it.bookId == bookId && it.assetRole == assetRole }
        }

        override suspend fun replaceAll(entities: List<BookAssetEntity>) {
            clearAll()
            upsertAll(entities)
        }
    }
}
