package com.wenwentome.reader.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.wenwentome.reader.core.database.entity.BookAssetEntity
import com.wenwentome.reader.core.database.entity.BookRecordEntity
import com.wenwentome.reader.core.database.entity.ReadingStateEntity
import com.wenwentome.reader.core.database.entity.RemoteBindingEntity
import com.wenwentome.reader.core.database.entity.SourceDefinitionEntity
import com.wenwentome.reader.core.model.AssetRole
import com.wenwentome.reader.core.model.RuleFormat
import com.wenwentome.reader.core.model.SourceType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ReaderDatabaseTest {
    @Test
    fun upsertBookAndReadingState_areReturnedTogether() = runTest {
        val database = testDatabase()
        database.bookRecordDao().upsert(BookRecordEntity(id = "book-1", title = "三体"))
        database.readingStateDao()
            .upsert(
                ReadingStateEntity(
                    bookId = "book-1",
                    locator = "cfi(/6/2)",
                    progressPercent = 0.42f,
                ),
            )

        val book = database.bookRecordDao().observeById("book-1").first()
        val state = database.readingStateDao().observeByBookId("book-1").first()

        assertEquals("三体", book?.title)
        assertEquals(0.42f, state?.progressPercent)
    }

    @Test
    fun bookRecordDao_getAll_returnsInserted() = runTest {
        val database = testDatabase()
        database.bookRecordDao()
            .upsert(
                BookRecordEntity(
                    id = "book-1",
                    title = "三体",
                    createdAt = 1L,
                    updatedAt = 2L,
                ),
            )

        val all = database.bookRecordDao().getAll()
        assertEquals(1, all.size)
        assertEquals("book-1", all.first().id)
    }

    @Test
    fun bookAssetDao_findPrimaryAsset_returnsPrimaryText() = runTest {
        val database = testDatabase()
        database.bookAssetDao()
            .upsertAll(
                listOf(
                    BookAssetEntity(
                        bookId = "book-1",
                        assetRole = AssetRole.COVER,
                        storageUri = "content://cover",
                        mime = "image/png",
                        size = 1L,
                        hash = "h1",
                        syncPath = "covers/book-1.png",
                    ),
                    BookAssetEntity(
                        bookId = "book-1",
                        assetRole = AssetRole.PRIMARY_TEXT,
                        storageUri = "content://text",
                        mime = "text/plain",
                        size = 2L,
                        hash = "h2",
                        syncPath = "books/book-1.txt",
                    ),
                ),
            )

        val primary = database.bookAssetDao().findPrimaryAsset("book-1")
        assertNotNull(primary)
        assertEquals(AssetRole.PRIMARY_TEXT, primary?.assetRole)
        assertEquals("content://text", primary?.storageUri)
    }

    @Test
    fun readingStateDao_getAll_returnsInserted() = runTest {
        val database = testDatabase()
        database.readingStateDao().upsert(ReadingStateEntity(bookId = "book-1", progressPercent = 0.1f))
        database.readingStateDao().upsert(ReadingStateEntity(bookId = "book-2", progressPercent = 0.2f))

        val all = database.readingStateDao().getAll()
        assertEquals(2, all.size)
    }

    @Test
    fun remoteBindingDao_getAll_returnsInserted() = runTest {
        val database = testDatabase()
        database.remoteBindingDao()
            .upsert(
                RemoteBindingEntity(
                    bookId = "book-1",
                    sourceId = "src-1",
                    remoteBookId = "r-1",
                    remoteBookUrl = "https://example.com/books/1",
                ),
            )

        val all = database.remoteBindingDao().getAll()
        assertEquals(1, all.size)
        assertEquals("book-1", all.first().bookId)
    }

    @Test
    fun sourceDefinitionDao_observeAll_isOrderedBySourceName_andToggleEnabledWorks() = runTest {
        val database = testDatabase()
        database.sourceDefinitionDao()
            .upsertAll(
                listOf(
                    SourceDefinitionEntity(
                        sourceId = "s1",
                        sourceName = "B-Source",
                        sourceType = SourceType.IMPORTED,
                        ruleFormat = RuleFormat.CUSTOM,
                        enabled = true,
                    ),
                    SourceDefinitionEntity(
                        sourceId = "s2",
                        sourceName = "A-Source",
                        sourceType = SourceType.IMPORTED,
                        ruleFormat = RuleFormat.CUSTOM,
                        enabled = true,
                    ),
                ),
            )

        val ordered = database.sourceDefinitionDao().observeAll().first()
        assertEquals(listOf("A-Source", "B-Source"), ordered.map { it.sourceName })

        database.sourceDefinitionDao().toggleEnabled("s2")
        val toggled = database.sourceDefinitionDao().getAll().first { it.sourceId == "s2" }
        assertEquals(false, toggled.enabled)
    }

    private fun testDatabase(): ReaderDatabase =
        Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ReaderDatabase::class.java,
        )
            .allowMainThreadQueries()
            .build()
}
