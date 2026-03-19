package com.wenwentome.reader.sync.github

import com.wenwentome.reader.core.model.AssetRole
import com.wenwentome.reader.core.model.BookAsset
import com.wenwentome.reader.core.model.BookFormat
import com.wenwentome.reader.core.model.BookRecord
import com.wenwentome.reader.core.model.OriginType
import com.wenwentome.reader.core.model.ReadingBookmark
import com.wenwentome.reader.core.model.ReadingState
import com.wenwentome.reader.core.model.RuleFormat
import com.wenwentome.reader.core.model.SourceDefinition
import com.wenwentome.reader.core.model.SourceType
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.util.Base64

class GitHubSyncRepositoryTest {
    @Test
    fun pushAndPullSnapshot_roundTripsManifestAndAssets() = runTest {
        val server = MockWebServer()
        server.dispatcher = FakeGitHubDispatcher()
        server.start()
        try {
            val repository = GitHubSyncRepository(
                api = GitHubContentApi(server.url("/").toString()),
                serializer = SyncManifestSerializer(),
                bookRecordStore = FakeBookRecordStore(existing = listOf(sampleBookRecord())),
                readingStateStore = FakeReadingStateStore(existing = listOf(sampleReadingState())),
                remoteBindingStore = FakeRemoteBindingStore(existing = listOf(sampleRemoteBinding())),
                sourceDefinitionStore = FakeSourceDefinitionStore(existing = listOf(sampleSourceDefinition())),
                bookAssetStore = FakeBookAssetStore(existing = listOf(sampleAsset())),
                preferencesStore = FakeSyncPreferencesStore(samplePreferences()),
                fileStore = FakeSyncFileStore(existing = listOf(sampleAsset())),
                snapshotIdFactory = { "snapshot-1" },
                revisionFactory = { "2026-03-19T00:00:00Z" },
                mergedAtProvider = { 123456789L },
            )

            repository.pushSnapshot(
                auth = GitHubAuthConfig(owner = "me", repo = "books", branch = "main", token = "token"),
            )
            val restored = repository.pullLatestSnapshot(
                auth = GitHubAuthConfig(owner = "me", repo = "books", branch = "main", token = "token"),
            )

            assertTrue(server.requestCount >= 8)
            assertEquals("snapshot-1", restored.snapshot.snapshotId)
            assertEquals(1, restored.assets.size)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun pullLatestSnapshot_preservesLocalSourceDefinitionPayloadWhenRemoteSnapshotIsOlder() = runTest {
        val server = MockWebServer()
        server.dispatcher = FakeGitHubDispatcher()
        server.start()
        try {
            val oldRepository = GitHubSyncRepository(
                api = GitHubContentApi(server.url("/").toString()),
                serializer = SyncManifestSerializer(),
                bookRecordStore = FakeBookRecordStore(existing = listOf(sampleBookRecord())),
                readingStateStore = FakeReadingStateStore(existing = listOf(sampleReadingState())),
                remoteBindingStore = FakeRemoteBindingStore(existing = listOf(sampleRemoteBinding())),
                sourceDefinitionStore = FakeSourceDefinitionStore(
                    existing = listOf(
                        sampleSourceDefinition().copy(
                            sourceUrl = null,
                            rawDefinition = null,
                        ),
                    ),
                ),
                bookAssetStore = FakeBookAssetStore(existing = listOf(sampleAsset())),
                preferencesStore = FakeSyncPreferencesStore(samplePreferences()),
                fileStore = FakeSyncFileStore(existing = listOf(sampleAsset())),
                snapshotIdFactory = { "snapshot-old" },
                revisionFactory = { "2026-03-19T00:00:00Z" },
                mergedAtProvider = { 123456789L },
            )
            oldRepository.pushSnapshot(
                auth = GitHubAuthConfig(owner = "me", repo = "books", branch = "main", token = "token"),
            )

            val localSourceDefinitionStore = FakeSourceDefinitionStore(existing = listOf(sampleSourceDefinition()))
            val restoreRepository = GitHubSyncRepository(
                api = GitHubContentApi(server.url("/").toString()),
                serializer = SyncManifestSerializer(),
                bookRecordStore = FakeBookRecordStore(existing = listOf(sampleBookRecord())),
                readingStateStore = FakeReadingStateStore(existing = listOf(sampleReadingState())),
                remoteBindingStore = FakeRemoteBindingStore(existing = listOf(sampleRemoteBinding())),
                sourceDefinitionStore = localSourceDefinitionStore,
                bookAssetStore = FakeBookAssetStore(existing = listOf(sampleAsset())),
                preferencesStore = FakeSyncPreferencesStore(samplePreferences()),
                fileStore = FakeSyncFileStore(existing = listOf(sampleAsset())),
                snapshotIdFactory = { "snapshot-new" },
                revisionFactory = { "2026-03-19T00:00:01Z" },
                mergedAtProvider = { 123456790L },
            )

            restoreRepository.pullLatestSnapshot(
                auth = GitHubAuthConfig(owner = "me", repo = "books", branch = "main", token = "token"),
            )

            val restoredSource = localSourceDefinitionStore.getAll().single()
            assertEquals("https://example.com", restoredSource.sourceUrl)
            assertEquals("{\"bookSourceName\":\"示例源\"}", restoredSource.rawDefinition)
        } finally {
            server.shutdown()
        }
    }
}

private class FakeBookRecordStore(existing: List<BookRecord>) : BookRecordSyncStore {
    private var records: List<BookRecord> = existing

    override suspend fun getAll(): List<BookRecord> = records

    override suspend fun replaceAll(records: List<BookRecord>) {
        this.records = records
    }
}

private class FakeReadingStateStore(existing: List<ReadingState>) : ReadingStateSyncStore {
    private var states: List<ReadingState> = existing

    override suspend fun getAll(): List<ReadingState> = states

    override suspend fun replaceAll(states: List<ReadingState>) {
        this.states = states
    }
}

private class FakeRemoteBindingStore(existing: List<com.wenwentome.reader.core.model.RemoteBinding>) : RemoteBindingSyncStore {
    private var bindings: List<com.wenwentome.reader.core.model.RemoteBinding> = existing

    override suspend fun getAll(): List<com.wenwentome.reader.core.model.RemoteBinding> = bindings

    override suspend fun replaceAll(bindings: List<com.wenwentome.reader.core.model.RemoteBinding>) {
        this.bindings = bindings
    }
}

private class FakeSourceDefinitionStore(existing: List<SourceDefinition>) : SourceDefinitionSyncStore {
    private var definitions: List<SourceDefinition> = existing

    override suspend fun getAll(): List<SourceDefinition> = definitions

    override suspend fun replaceAll(definitions: List<SourceDefinition>) {
        this.definitions = definitions
    }
}

private class FakeBookAssetStore(existing: List<BookAsset>) : BookAssetSyncStore {
    private var assets: List<BookAsset> = existing

    override suspend fun getAll(): List<BookAsset> = assets

    override suspend fun replaceAll(assets: List<BookAsset>) {
        this.assets = assets
    }
}

private class FakeSyncPreferencesStore(
    private var snapshot: PreferencesSnapshot,
) : SyncPreferencesStore {
    override suspend fun exportSnapshot(): PreferencesSnapshot = snapshot

    override suspend fun importSnapshot(snapshot: PreferencesSnapshot) {
        this.snapshot = snapshot
    }

    override suspend fun getOrCreateDeviceId(): String = snapshot.deviceId
}

private class FakeSyncFileStore(existing: List<BookAsset>) : SyncFileStore {
    private val content = existing.associate { it.storageUri to "asset-body".encodeToByteArray() }.toMutableMap()

    override fun open(storageUri: String) = ByteArrayInputStream(content.getValue(storageUri))

    override fun persistOriginal(bookId: String, extension: String, bytes: ByteArray): String {
        val uri = "memory://$bookId/source.$extension"
        content[uri] = bytes
        return uri
    }
}

private class FakeGitHubDispatcher : Dispatcher() {
    private val files = linkedMapOf<String, ByteArray>()
    private var revision = 0

    override fun dispatch(request: RecordedRequest): MockResponse {
        val path = request.requestUrl?.encodedPath.orEmpty()
        return when {
            request.method == "PUT" && path.contains("/contents/") -> {
                val contentPath = path.substringAfter("/contents/")
                val encoded = request.body.readUtf8()
                    .substringAfter("\"content\":\"")
                    .substringBefore('"')
                files[contentPath] = Base64.getDecoder().decode(encoded)
                revision += 1
                MockResponse().setBody("""{"content":{"sha":"sha-$revision"}}""")
            }

            request.method == "GET" && path.contains("/contents/") -> {
                val contentPath = path.substringAfter("/contents/")
                val bytes = files[contentPath] ?: return MockResponse().setResponseCode(404)
                MockResponse().setBody(
                    """
                    {
                      "sha": "sha-$contentPath",
                      "content": "${Base64.getEncoder().encodeToString(bytes)}",
                      "download_url": null
                    }
                    """.trimIndent()
                )
            }

            else -> MockResponse().setResponseCode(404)
        }
    }
}

private fun sampleBookRecord(): BookRecord =
    BookRecord(
        id = "book-1",
        title = "悉达多",
        author = "黑塞",
        originType = OriginType.LOCAL,
        primaryFormat = BookFormat.EPUB,
    )

private fun sampleReadingState(): ReadingState =
    ReadingState(
        bookId = "book-1",
        locator = "chapter-3",
        chapterRef = "chapter-3",
        progressPercent = 0.31f,
        bookmarks = listOf(ReadingBookmark(chapterRef = "chapter-3", locator = "chapter-3", label = "重读这里")),
    )

private fun sampleAsset(): BookAsset =
    BookAsset(
        bookId = "book-1",
        assetRole = AssetRole.PRIMARY_TEXT,
        storageUri = "memory://book-1/source.epub",
        mime = "application/epub+zip",
        size = 10,
        hash = "hash-1",
        syncPath = "assets/book-1/source.epub",
    )

private fun sampleRemoteBinding(): com.wenwentome.reader.core.model.RemoteBinding =
    com.wenwentome.reader.core.model.RemoteBinding(
        bookId = "book-1",
        sourceId = "sample",
        remoteBookId = "remote-1",
        remoteBookUrl = "https://example.com/books/remote-1",
    )

private fun sampleSourceDefinition(): SourceDefinition =
    SourceDefinition(
        sourceId = "sample",
        sourceName = "示例源",
        sourceType = SourceType.IMPORTED,
        ruleFormat = RuleFormat.LEGACY,
        sourceUrl = "https://example.com",
        rawDefinition = "{\"bookSourceName\":\"示例源\"}",
    )

private fun samplePreferences(): PreferencesSnapshot =
    PreferencesSnapshot(
        owner = "me",
        repo = "books",
        branch = "main",
        token = "token",
        deviceId = "device-1",
    )
