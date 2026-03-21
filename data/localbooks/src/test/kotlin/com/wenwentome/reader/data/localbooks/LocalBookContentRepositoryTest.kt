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
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

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

        assertEquals(1, chapters.size)
        assertEquals("第一章", chapters.single().title)
        assertEquals("OEBPS/chapter1.xhtml", chapters.single().chapterRef)
    }

    private fun createRepository(
        epubFixture: String? = null,
        generatedEpubBytes: ByteArray? = null,
    ): LocalBookContentRepository {
        val filesDir = createTempDir(prefix = "localbooks-content-test-")
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
        val entries = linkedMapOf(
            "META-INF/container.xml" to CONTAINER_XML.toByteArray(Charsets.UTF_8),
            "OEBPS/content.opf" to TITLE_PAGE_FIRST_CONTENT_OPF.toByteArray(Charsets.UTF_8),
            "OEBPS/toc.ncx" to EMPTY_TOC_NCX.toByteArray(Charsets.UTF_8),
            "OEBPS/titlepage.xhtml" to TITLE_PAGE_XHTML.toByteArray(Charsets.UTF_8),
            "OEBPS/chapter1.xhtml" to CHAPTER_ONE_XHTML.toByteArray(Charsets.UTF_8),
            "OEBPS/images/cover.jpg" to byteArrayOf(1, 2, 3, 4),
        )

        return java.io.ByteArrayOutputStream().use { output ->
            ZipOutputStream(output).use { zip ->
                val mimeBytes = "application/epub+zip".toByteArray(Charsets.UTF_8)
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
                    zip.putNextEntry(ZipEntry(path))
                    zip.write(bytes)
                    zip.closeEntry()
                }
            }
            output.toByteArray()
        }
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

    private companion object {
        const val CONTAINER_XML =
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
              <rootfiles>
                <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
              </rootfiles>
            </container>
            """

        const val TITLE_PAGE_FIRST_CONTENT_OPF =
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <package xmlns="http://www.idpf.org/2007/opf" version="2.0" unique-identifier="BookId">
              <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
                <dc:title>Title Page First</dc:title>
                <dc:identifier id="BookId">book-1</dc:identifier>
                <dc:language>zh-CN</dc:language>
              </metadata>
              <manifest>
                <item id="ncx" href="toc.ncx" media-type="application/x-dtbncx+xml"/>
                <item id="titlepage" href="titlepage.xhtml" media-type="application/xhtml+xml"/>
                <item id="chapter1" href="chapter1.xhtml" media-type="application/xhtml+xml"/>
                <item id="cover-image" href="images/cover.jpg" media-type="image/jpeg"/>
              </manifest>
              <spine toc="ncx">
                <itemref idref="titlepage"/>
                <itemref idref="chapter1"/>
              </spine>
            </package>
            """

        const val EMPTY_TOC_NCX =
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <ncx xmlns="http://www.daisy.org/z3986/2005/ncx/" version="2005-1">
              <head>
                <meta name="dtb:uid" content="book-1"/>
              </head>
              <docTitle>
                <text>Title Page First</text>
              </docTitle>
              <navMap/>
            </ncx>
            """

        const val TITLE_PAGE_XHTML =
            """
            <?xml version="1.0" encoding="utf-8"?>
            <!DOCTYPE html>
            <html xmlns="http://www.w3.org/1999/xhtml">
              <head><title>Cover</title></head>
              <body>
                <div class="hero"><img src="images/cover.jpg" alt="Cover"/></div>
                <p>Cover</p>
              </body>
            </html>
            """

        const val CHAPTER_ONE_XHTML =
            """
            <?xml version="1.0" encoding="utf-8"?>
            <!DOCTYPE html>
            <html xmlns="http://www.w3.org/1999/xhtml">
              <head><title>第一章</title></head>
              <body>
                <h1>第一章</h1>
                <p>第一章-第一段。</p>
                <p>第一章-第二段。</p>
              </body>
            </html>
            """
    }
}
