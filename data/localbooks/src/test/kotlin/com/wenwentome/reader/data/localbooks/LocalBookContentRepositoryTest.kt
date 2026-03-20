package com.wenwentome.reader.data.localbooks

import com.wenwentome.reader.core.database.dao.BookAssetDao
import com.wenwentome.reader.core.database.entity.BookAssetEntity
import com.wenwentome.reader.core.model.AssetRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.InputStream

class LocalBookContentRepositoryTest {
    @Test
    fun load_epubSkipsCoverAndOpensFirstReadableChapter() = runTest {
        val repository = createRepository(epubFixture = "sample-cover-first.epub")

        val content = repository.load(bookId = "epub-book", locator = null)

        assertEquals("第一章", content.chapterTitle)
        assertFalse(content.paragraphs.first().contains("Cover"))
        assertTrue(content.paragraphs.any { it.contains("第一章-第一段") })
    }

    @Test
    fun load_epubRestoresLegacySpineLocatorFrom10WithoutLosingProgress() = runTest {
        val repository = createRepository(epubFixture = "sample-cover-first.epub")

        val content = repository.load(bookId = "epub-book", locator = "3:1")

        assertEquals("第一章", content.chapterTitle)
        assertEquals("第一章-第一段。", content.paragraphs.first())
    }

    private fun createRepository(epubFixture: String): LocalBookContentRepository {
        val filesDir = createTempDir(prefix = "localbooks-content-test-")
        val fileStore = LocalBookFileStore(filesDir = filesDir)
        val bytes = fixtureBytes(epubFixture)
        val storageUri = fileStore.persistOriginal(
            bookId = "epub-book",
            extension = "epub",
            bytes = bytes,
        )
        val dao = FakeBookAssetDao(
            BookAssetEntity(
                bookId = "epub-book",
                assetRole = AssetRole.PRIMARY_TEXT,
                storageUri = storageUri,
                mime = "application/epub+zip",
                size = bytes.size.toLong(),
                hash = "fixture-hash",
                syncPath = "books/epub-book/source.epub",
            )
        )
        return LocalBookContentRepository(
            bookAssetDao = dao,
            fileStore = fileStore,
        )
    }

    private fun fixture(name: String): InputStream =
        requireNotNull(javaClass.classLoader?.getResourceAsStream("fixtures/$name")) {
            "Missing fixture: fixtures/$name"
        }

    private fun fixtureBytes(name: String): ByteArray = fixture(name).use { it.readBytes() }

    private class FakeBookAssetDao(
        primaryAsset: BookAssetEntity,
    ) : BookAssetDao {
        private val items = MutableStateFlow(listOf(primaryAsset))

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

        override suspend fun deleteByBookId(bookId: String) {
            items.value = items.value.filterNot { it.bookId == bookId }
        }

        override suspend fun replaceAll(entities: List<BookAssetEntity>) {
            clearAll()
            upsertAll(entities)
        }
    }
}
