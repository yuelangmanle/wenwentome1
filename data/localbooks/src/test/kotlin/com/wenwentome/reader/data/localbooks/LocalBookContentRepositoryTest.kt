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
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.io.path.createTempDirectory

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

    @Test
    fun load_epubOutOfRangeParagraphLocator_fallsBackToChapterStart() = runTest {
        val repository = createRepository(epubFixture = "sample-cover-first.epub")

        val content = repository.load(
            bookId = "epub-book",
            locator = "chapter:OEBPS/chapter1.xhtml#paragraph:99",
        )

        assertEquals("第一章", content.chapterTitle)
        assertTrue(content.paragraphs.isNotEmpty())
        assertTrue(content.paragraphs.any { it.contains("第一章-第一段") })
    }

    @Test
    fun load_epubLaterParagraph_exposesWindowMetadataForProgressCalculation() = runTest {
        val repository = createRepository(epubFixture = "sample-cover-first.epub")

        val content = repository.load(
            bookId = "epub-book",
            locator = "chapter:OEBPS/chapter1.xhtml#paragraph:1",
        )

        assertEquals(1, content.windowStartParagraphIndex)
        assertTrue(content.totalParagraphCount >= content.paragraphs.size)
    }

    @Test
    fun load_epubWithoutToc_skipsTitlePageFrontMatterAndOpensFirstReadableChapter() = runTest {
        val repository = createRepository(
            generatedEpubBytes = createTitlePageFirstEpubBytes(),
        )

        val content = repository.load(bookId = "epub-book", locator = null)

        assertEquals("第一章", content.chapterTitle)
        assertEquals("OEBPS/chapter1.xhtml", content.chapterRef)
        assertTrue(content.paragraphs.any { it.contains("第一章-第一段") })
        assertFalse(content.paragraphs.any { it.contains("Cover") })
    }

    @Test
    fun loadChapters_epubWithoutToc_skipsTitlePageFrontMatterInCatalog() = runTest {
        val repository = createRepository(
            generatedEpubBytes = createTitlePageFirstEpubBytes(),
        )

        val chapters = repository.loadChapters(bookId = "epub-book")

        assertEquals(2, chapters.size)
        assertEquals("第一章", chapters.first().title)
        assertEquals("OEBPS/chapter1.xhtml", chapters.first().chapterRef)
        assertEquals("第二章", chapters[1].title)
        assertEquals("OEBPS/chapter2.xhtml", chapters[1].chapterRef)
        assertFalse(chapters.any { it.chapterRef.contains("titlepage") })
    }

    private fun createRepository(
        epubFixture: String? = null,
        generatedEpubBytes: ByteArray? = null,
    ): LocalBookContentRepository {
        val filesDir = createTempDirectory(prefix = "localbooks-content-test-").toFile()
        val fileStore = LocalBookFileStore(filesDir = filesDir)
        val bytes = generatedEpubBytes ?: fixtureBytes(requireNotNull(epubFixture))
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

    private fun createTitlePageFirstEpubBytes(): ByteArray {
        val entries = unzipEntries(fixtureBytes("sample-cover-first.epub")).toMutableMap()
        val coverPage = requireNotNull(entries.remove("OEBPS/cover.xhtml")) {
            "sample-cover-first.epub missing OEBPS/cover.xhtml"
        }
        entries["OEBPS/titlepage.xhtml"] = coverPage
        entries["OEBPS/content.opf"] =
            entries.getValue("OEBPS/content.opf")
                .decodeToString()
                .replace("cover.xhtml", "titlepage.xhtml")
                .replace(
                    Regex("""<itemref idref="chapter2"/>\s*<itemref idref="chapter1"/>"""),
                    "<itemref idref=\"chapter1\"/>\n    <itemref idref=\"chapter2\"/>",
                )
                .replace(Regex("""\s*<reference type="cover" title="Cover" href="titlepage.xhtml"/>\s*"""), "")
                .toByteArray(Charsets.UTF_8)
        entries["OEBPS/nav.xhtml"] =
            entries.getValue("OEBPS/nav.xhtml")
                .decodeToString()
                .replace("epub:type=\"toc\"", "epub:type=\"landmarks\"")
                .toByteArray(Charsets.UTF_8)

        return zipEntries(entries)
    }

    private fun unzipEntries(bytes: ByteArray): LinkedHashMap<String, ByteArray> {
        val entries = LinkedHashMap<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entries[entry.name] = zip.readBytes()
                zip.closeEntry()
            }
        }
        return entries
    }

    private fun zipEntries(entries: Map<String, ByteArray>): ByteArray =
        java.io.ByteArrayOutputStream().use { output ->
            ZipOutputStream(output).use { zip ->
                val mimeBytes = requireNotNull(entries["mimetype"]) { "EPUB missing mimetype entry" }
                val crc = CRC32().apply { update(mimeBytes) }.value
                zip.putNextEntry(
                    ZipEntry("mimetype").apply {
                        method = ZipEntry.STORED
                        size = mimeBytes.size.toLong()
                        compressedSize = mimeBytes.size.toLong()
                        this.crc = crc
                    }
                )
                zip.write(mimeBytes)
                zip.closeEntry()

                entries.forEach { (path, bytes) ->
                    if (path == "mimetype") return@forEach
                    zip.putNextEntry(ZipEntry(path))
                    zip.write(bytes)
                    zip.closeEntry()
                }
            }
            output.toByteArray()
        }

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
