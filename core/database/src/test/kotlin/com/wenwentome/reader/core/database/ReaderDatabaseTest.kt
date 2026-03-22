package com.wenwentome.reader.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.wenwentome.reader.core.database.entity.ApiAbilityCacheEntity
import com.wenwentome.reader.core.database.entity.ApiCapabilityBindingEntity
import com.wenwentome.reader.core.database.entity.ApiProviderEntity
import com.wenwentome.reader.core.database.entity.ApiUsageLogEntity
import com.wenwentome.reader.core.database.entity.BookAssetEntity
import com.wenwentome.reader.core.database.entity.BookRecordEntity
import com.wenwentome.reader.core.database.entity.ReadingStateEntity
import com.wenwentome.reader.core.database.entity.RemoteBindingEntity
import com.wenwentome.reader.core.database.entity.SourceDefinitionEntity
import com.wenwentome.reader.core.model.AssetRole
import com.wenwentome.reader.core.model.ProviderAuthScheme
import com.wenwentome.reader.core.model.ProviderKind
import com.wenwentome.reader.core.model.ProviderModelSource
import com.wenwentome.reader.core.model.ProviderSecretSyncMode
import com.wenwentome.reader.core.model.ProviderTransportStyle
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
    fun providerDao_roundTripsSecretModeModelSourceAndUpdatedAt() = runTest {
        val database = testDatabase()
        database.apiProviderDao()
            .upsert(
                ApiProviderEntity(
                    providerId = "openai-main",
                    displayName = "OpenAI 主线路",
                    providerKind = ProviderKind.OPENAI_COMPATIBLE,
                    apiStyle = ProviderTransportStyle.RESPONSES,
                    authScheme = ProviderAuthScheme.BEARER,
                    secretSyncMode = ProviderSecretSyncMode.LOCAL_ONLY,
                    modelSource = ProviderModelSource.MANUAL,
                    updatedAt = 1711000000000,
                ),
            )

        val stored = database.apiProviderDao().observeAll().first().single()
        assertEquals(ProviderSecretSyncMode.LOCAL_ONLY, stored.secretSyncMode)
        assertEquals(ProviderModelSource.MANUAL, stored.modelSource)
        assertEquals(1711000000000, stored.updatedAt)
    }

    @Test
    fun capabilityBindingDao_roundTripsPrimaryAndFallbackRoute() = runTest {
        val database = testDatabase()
        database.apiCapabilityBindingDao()
            .upsert(
                ApiCapabilityBindingEntity(
                    capabilityId = "reader.summary",
                    primaryProviderId = "openai-main",
                    primaryModelId = "gpt-4.1-mini",
                    fallbackProviderId = "openai-backup",
                    fallbackModelId = "gpt-4o-mini",
                    updatedAt = 1711000000000,
                ),
            )

        val stored = database.apiCapabilityBindingDao().observeAll().first().single()
        assertEquals("openai-main", stored.primaryProviderId)
        assertEquals("gpt-4o-mini", stored.fallbackModelId)
        assertEquals(1711000000000, stored.updatedAt)
    }

    @Test
    fun usageLogDao_roundTripsCreatedAtCostAndSuccess() = runTest {
        val database = testDatabase()
        database.apiUsageLogDao()
            .upsert(
                ApiUsageLogEntity(
                    callId = "call-1",
                    capabilityId = "reader.summary",
                    providerId = "openai-main",
                    modelId = "gpt-4.1-mini",
                    success = true,
                    estimatedCostMicros = 1234L,
                    createdAt = 1711000000000,
                ),
            )

        val stored = database.apiUsageLogDao().observeLatest().first().single()
        assertEquals(true, stored.success)
        assertEquals(1234L, stored.estimatedCostMicros)
        assertEquals(1711000000000, stored.createdAt)
    }

    @Test
    fun abilityCacheDao_roundTripsCacheKeyPayloadAndExpiresAt() = runTest {
        val database = testDatabase()
        database.apiAbilityCacheDao()
            .upsert(
                ApiAbilityCacheEntity(
                    cacheKey = "reader.summary:book-1:chapter-3:v1",
                    payloadJson = """{"summary":"cached"}""",
                    expiresAt = 1712000000000,
                    updatedAt = 1711000000000,
                ),
            )

        val cached = database.apiAbilityCacheDao().findByKey("reader.summary:book-1:chapter-3:v1")
        assertEquals("""{"summary":"cached"}""", cached?.payloadJson)
        assertEquals(1712000000000, cached?.expiresAt)
    }

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
    fun bookAssetDao_findAndDeleteByRole_roundTripsCoverAsset() = runTest {
        val database = testDatabase()
        database.bookAssetDao().upsert(
            BookAssetEntity(
                bookId = "book-1",
                assetRole = AssetRole.COVER,
                storageUri = "file:///manual-cover.png",
                mime = "image/png",
                size = 12L,
                hash = "cover-hash",
                syncPath = "books/book-1/manual-cover.png",
            )
        )

        val cover = database.bookAssetDao().findByRole("book-1", AssetRole.COVER)
        assertEquals("file:///manual-cover.png", cover?.storageUri)

        database.bookAssetDao().deleteByRole("book-1", AssetRole.COVER)
        val deleted = database.bookAssetDao().findByRole("book-1", AssetRole.COVER)
        assertEquals(null, deleted)
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
    fun remoteBinding_roundTripsLatestChapterMetadata() = runTest {
        val database = testDatabase()
        database.remoteBindingDao()
            .upsert(
                RemoteBindingEntity(
                    bookId = "book-1",
                    sourceId = "src",
                    remoteBookId = "remote",
                    remoteBookUrl = "https://example.com/book",
                    latestKnownChapterRef = "chapter-9",
                    lastCatalogRefreshAt = 123L,
                ),
            )

        val binding = database.remoteBindingDao().observeByBookId("book-1").first()
        assertEquals("chapter-9", binding?.latestKnownChapterRef)
        assertEquals(123L, binding?.lastCatalogRefreshAt)
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
                        sourceUrl = "https://example.com/b",
                        rawDefinition = "{\"bookSourceName\":\"B-Source\"}",
                        enabled = true,
                    ),
                    SourceDefinitionEntity(
                        sourceId = "s2",
                        sourceName = "A-Source",
                        sourceType = SourceType.IMPORTED,
                        ruleFormat = RuleFormat.CUSTOM,
                        sourceUrl = "https://example.com/a",
                        rawDefinition = "{\"bookSourceName\":\"A-Source\"}",
                        enabled = true,
                    ),
                ),
            )

        val ordered = database.sourceDefinitionDao().observeAll().first()
        assertEquals(listOf("A-Source", "B-Source"), ordered.map { it.sourceName })

        database.sourceDefinitionDao().toggleEnabled("s2")
        val toggled = database.sourceDefinitionDao().getAll().first { it.sourceId == "s2" }
        assertEquals(false, toggled.enabled)
        assertEquals("https://example.com/a", toggled.sourceUrl)
    }

    private fun testDatabase(): ReaderDatabase =
        Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ReaderDatabase::class.java,
        )
            .allowMainThreadQueries()
            .build()
}
