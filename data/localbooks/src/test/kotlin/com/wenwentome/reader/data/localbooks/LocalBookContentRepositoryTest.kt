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
        assertEquals("OEBPS/chapter1.xhtml", content.chapterRef)
        assertFalse(content.paragraphs.first().contains("Cover"))
        assertTrue(content.paragraphs.any { it.contains("第一章-第一段") })
    }

    @Test
    fun loadChapters_epubReturnsReadableCatalogWithLocatorHints() = runTest {
        val repository = createRepository(epubFixture = "sample-cover-first.epub")

        val chapters = repository.loadChapters(bookId = "epub-book")

        assertEquals("第一章", chapters.first().title)
        assertEquals("OEBPS/chapter1.xhtml", chapters.first().chapterRef)
        assertEquals("chapter:OEBPS/chapter1.xhtml#paragraph:0", chapters.first().locatorHint)
    }

    @Test
    fun load_epubRestoresLegacySpineLocatorFrom10WithoutLosingProgress() = runTest {
        val repository = createRepository(epubFixture = "sample-cover-first.epub")

        val content = repository.load(bookId = "epub-book", locator = "3:1")

        assertEquals("第一章", content.chapterTitle)
        assertEquals("第一章-第一段。", content.paragraphs.first())
    }

    @Test
    fun load_epubWithTocMissingAndNavToc_usesNavCatalogBeforeSpine() = runTest {
        val repository = createRepository(epubFixture = "sample-cover-first.epub")

        val content = repository.load(bookId = "epub-book", locator = null)

        // sample-cover-first 的 spine 可读首章是“第二章”，若这里是“第一章”说明走了 nav toc 而非 spine。
        assertEquals("第一章", content.chapterTitle)
        assertTrue(content.paragraphs.first().contains("第一章"))
    }

    @Test
    fun load_epubWithNonTocNav_fallsBackToFilteredSpine() = runTest {
        val repository = createRepository(epubFixture = "sample-nav-landmarks-first.epub")

        val content = repository.load(bookId = "epub-book", locator = null)

        assertEquals("第二章", content.chapterTitle)
        assertTrue(content.paragraphs.first().contains("第二章"))
    }

    @Test
    fun load_epubStructuredLocator_recoversViaRawSpineMappingWhenChapterNotInCatalog() = runTest {
        val repository = createRepository(epubFixture = "sample-cover-first.epub")

        val content = repository.load(
            bookId = "epub-book",
            locator = "chapter:OEBPS/nav.xhtml#paragraph:1",
        )

        // nav.xhtml 不在可读 catalog 中，应映射到其在 raw spine 附近的可读章节（第二章），而不是回退首章。
        assertEquals("第二章", content.chapterTitle)
        assertEquals("第二章-第一段。", content.paragraphs.first())
    }

    @Test
    fun load_epubWhenTocAndNavTocBothExist_prefersTocOrder() = runTest {
        val repository = createRepository(epubFixture = "sample-toc-nav-conflict.epub")

        val content = repository.load(bookId = "epub-book", locator = null)

        // TOC 首章为第二章；nav toc 首章为第一章。应优先取 TOC。
        assertEquals("目录第二章", content.chapterTitle)
        assertTrue(content.paragraphs.any { it.contains("第二章-来自TOC首章") })
    }

    @Test
    fun load_epubWhenObviousNavIsLandmarks_stillFindsActualTocNavResource() = runTest {
        val repository = createRepository(epubFixture = "sample-nav-secondary-toc.epub")

        val content = repository.load(bookId = "epub-book", locator = null)

        // 若错误命中 landmarks nav 并直接回退 spine，会落到第二章；正确行为是扫描到 secondary toc nav，落到第一章。
        assertEquals("第一章-secondary-nav", content.chapterTitle)
        assertTrue(content.paragraphs.any { it.contains("第一章-secondary-nav-第一段") })
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

        override fun observeAll(): Flow<List<BookAssetEntity>> = items.asStateFlow()

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
