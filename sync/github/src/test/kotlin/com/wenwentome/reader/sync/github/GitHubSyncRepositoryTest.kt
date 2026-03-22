package com.wenwentome.reader.sync.github

import com.wenwentome.reader.core.model.AssetRole
import com.wenwentome.reader.core.model.ApiCapabilityBinding
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.util.Base64

class GitHubSyncRepositoryTest {
    @Test
    fun pushAndPullSnapshot_roundTripsManifestAndAssets() = runTest {
        val server = MockWebServer()
        val dispatcher = FakeGitHubDispatcher()
        server.dispatcher = dispatcher
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
                secretStore = FakeSyncSecretStore(),
                capabilityBindingStore = FakeCapabilityBindingSyncStore(),
                fileStore = FakeSyncFileStore(existing = listOf(sampleAsset())),
                snapshotIdFactory = { "snapshot-1" },
                revisionFactory = { "2026-03-19T00:00:00Z" },
                mergedAtProvider = { 123456789L },
            )

            repository.pushSnapshot(
                auth = GitHubAuthConfig(owner = "me", repo = "books", branch = "main", token = "token", syncPassword = "sync-pass"),
            )
            val restored = repository.pullLatestSnapshot(
                auth = GitHubAuthConfig(owner = "me", repo = "books", branch = "main", token = "token", syncPassword = "sync-pass"),
            )

            assertTrue(server.requestCount >= 8)
            assertEquals("snapshot-1", restored.snapshot.snapshotId)
            assertEquals(1, restored.assets.size)
            assertFalse(dispatcher.readText("data/preferences.json").contains("\"token\""))
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun pullLatestSnapshot_restoresNonSecretFieldsBeforeUnlockingEncryptedSecrets() = runTest {
        val server = MockWebServer()
        val dispatcher = FakeGitHubDispatcher()
        server.dispatcher = dispatcher
        server.start()
        try {
            val seedRepository = GitHubSyncRepository(
                api = GitHubContentApi(server.url("/").toString()),
                serializer = SyncManifestSerializer(),
                bookRecordStore = FakeBookRecordStore(existing = listOf(sampleBookRecord())),
                readingStateStore = FakeReadingStateStore(existing = listOf(sampleReadingState())),
                remoteBindingStore = FakeRemoteBindingStore(existing = listOf(sampleRemoteBinding())),
                sourceDefinitionStore = FakeSourceDefinitionStore(existing = listOf(sampleSourceDefinition())),
                bookAssetStore = FakeBookAssetStore(existing = listOf(sampleAsset())),
                preferencesStore = FakeSyncPreferencesStore(samplePreferences()),
                secretStore =
                    FakeSyncSecretStore(
                        exported =
                            listOf(
                                sampleSecretEnvelope(secretId = "provider/openai"),
                            ),
                    ),
                capabilityBindingStore = FakeCapabilityBindingSyncStore(),
                fileStore = FakeSyncFileStore(existing = listOf(sampleAsset())),
                snapshotIdFactory = { "snapshot-1" },
                revisionFactory = { "2026-03-19T00:00:00Z" },
                mergedAtProvider = { 123456789L },
            )
            seedRepository.pushSnapshot(
                auth = GitHubAuthConfig(owner = "me", repo = "books", branch = "main", token = "token", syncPassword = "sync-pass"),
            )

            val preferencesStore = FakeSyncPreferencesStore(samplePreferences().copy(owner = "", repo = "", branch = "", bootstrapToken = ""))
            val secretStore = FakeSyncSecretStore()
            val restoreRepository = GitHubSyncRepository(
                api = GitHubContentApi(server.url("/").toString()),
                serializer = SyncManifestSerializer(),
                bookRecordStore = FakeBookRecordStore(existing = listOf(sampleBookRecord())),
                readingStateStore = FakeReadingStateStore(existing = listOf(sampleReadingState())),
                remoteBindingStore = FakeRemoteBindingStore(existing = listOf(sampleRemoteBinding())),
                sourceDefinitionStore = FakeSourceDefinitionStore(existing = listOf(sampleSourceDefinition())),
                bookAssetStore = FakeBookAssetStore(existing = listOf(sampleAsset())),
                preferencesStore = preferencesStore,
                secretStore = secretStore,
                capabilityBindingStore = FakeCapabilityBindingSyncStore(),
                fileStore = FakeSyncFileStore(existing = listOf(sampleAsset())),
                snapshotIdFactory = { "snapshot-2" },
                revisionFactory = { "2026-03-19T00:00:01Z" },
                mergedAtProvider = { 123456790L },
            )

            val restored = restoreRepository.pullLatestSnapshot(
                auth = GitHubAuthConfig(owner = "me", repo = "books", branch = "main", token = "token"),
            )

            assertEquals("books", restored.preferences.repo)
            assertTrue(restored.pendingSecretRestore.isNotEmpty())
            assertEquals("", restored.preferences.bootstrapToken)
            assertEquals("books", preferencesStore.snapshot.repo)
            assertEquals(1, secretStore.cachedPending.size)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun pullLatestSnapshot_mergesApiCapabilityBindingsAndReturnsConflictPayloads() = runTest {
        val server = MockWebServer()
        val dispatcher = FakeGitHubDispatcher()
        server.dispatcher = dispatcher
        server.start()
        try {
            val seedRepository = GitHubSyncRepository(
                api = GitHubContentApi(server.url("/").toString()),
                serializer = SyncManifestSerializer(),
                bookRecordStore = FakeBookRecordStore(existing = listOf(sampleBookRecord())),
                readingStateStore = FakeReadingStateStore(existing = listOf(sampleReadingState())),
                remoteBindingStore = FakeRemoteBindingStore(existing = listOf(sampleRemoteBinding())),
                sourceDefinitionStore = FakeSourceDefinitionStore(existing = listOf(sampleSourceDefinition())),
                bookAssetStore = FakeBookAssetStore(existing = listOf(sampleAsset())),
                preferencesStore = FakeSyncPreferencesStore(samplePreferences()),
                secretStore = FakeSyncSecretStore(),
                capabilityBindingStore =
                    FakeCapabilityBindingSyncStore(
                        exported =
                            listOf(
                                ApiCapabilityBinding(
                                    capabilityId = "reader.summary",
                                    primaryProviderId = "provider-remote",
                                    primaryModelId = "model-remote",
                                    updatedAt = 12_000L,
                                ),
                            ),
                    ),
                fileStore = FakeSyncFileStore(existing = listOf(sampleAsset())),
            )
            seedRepository.pushSnapshot(
                auth = GitHubAuthConfig(owner = "me", repo = "books", branch = "main", token = "token"),
            )

            val localBindingStore =
                FakeCapabilityBindingSyncStore(
                    existing =
                        listOf(
                            ApiCapabilityBinding(
                                capabilityId = "reader.summary",
                                primaryProviderId = "provider-local",
                                primaryModelId = "model-local",
                                updatedAt = 10_000L,
                            ),
                        ),
                )
            val restoreRepository = GitHubSyncRepository(
                api = GitHubContentApi(server.url("/").toString()),
                serializer = SyncManifestSerializer(),
                bookRecordStore = FakeBookRecordStore(existing = listOf(sampleBookRecord())),
                readingStateStore = FakeReadingStateStore(existing = listOf(sampleReadingState())),
                remoteBindingStore = FakeRemoteBindingStore(existing = listOf(sampleRemoteBinding())),
                sourceDefinitionStore = FakeSourceDefinitionStore(existing = listOf(sampleSourceDefinition())),
                bookAssetStore = FakeBookAssetStore(existing = listOf(sampleAsset())),
                preferencesStore = FakeSyncPreferencesStore(samplePreferences()),
                secretStore = FakeSyncSecretStore(),
                capabilityBindingStore = localBindingStore,
                fileStore = FakeSyncFileStore(existing = listOf(sampleAsset())),
            )

            val restored = restoreRepository.pullLatestSnapshot(
                auth = GitHubAuthConfig(owner = "me", repo = "books", branch = "main", token = "token"),
            )

            assertEquals(1, restored.pendingConflicts.size)
            assertEquals("provider-local", localBindingStore.current.single().primaryProviderId)
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
                secretStore = FakeSyncSecretStore(),
                capabilityBindingStore = FakeCapabilityBindingSyncStore(),
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
                secretStore = FakeSyncSecretStore(),
                capabilityBindingStore = FakeCapabilityBindingSyncStore(),
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
    var snapshot: PreferencesSnapshot,
) : SyncPreferencesStore {
    override suspend fun exportSnapshot(): PreferencesSnapshot = snapshot

    override suspend fun importSnapshot(snapshot: PreferencesSnapshot) {
        this.snapshot = snapshot
    }

    override suspend fun getOrCreateDeviceId(): String = snapshot.deviceId
}

private class FakeSyncSecretStore(
    private val exported: List<SecretEnvelopePayload> = emptyList(),
) : SyncSecretStore {
    var cachedPending: List<SecretEnvelopePayload> = emptyList()

    override suspend fun exportEncryptedSecrets(syncPassword: String): List<SecretEnvelopePayload> = exported

    override suspend fun cachePendingSecretRestore(secretEnvelopes: List<SecretEnvelopePayload>) {
        cachedPending = secretEnvelopes
    }

    override suspend fun restorePendingSecrets(syncPassword: String): List<SecretEnvelopePayload> =
        if (syncPassword == "sync-pass") {
            emptyList()
        } else {
            cachedPending
        }
}

private class FakeCapabilityBindingSyncStore(
    existing: List<ApiCapabilityBinding> = emptyList(),
    private val exported: List<ApiCapabilityBinding> = existing,
) : ApiCapabilityBindingSyncStore {
    var current: List<ApiCapabilityBinding> = existing

    override suspend fun exportBindings(): List<ApiCapabilityBinding> = exported

    override suspend fun mergeIncomingBindings(incoming: List<ApiCapabilityBinding>): List<CapabilityBindingConflict> {
        if (current.isEmpty()) {
            current = incoming
            return emptyList()
        }
        val local = current.single()
        val remote = incoming.single()
        return if (kotlin.math.abs(local.updatedAt - remote.updatedAt) <= 5_000L) {
            current = listOf(local)
            listOf(
                CapabilityBindingConflict(
                    capabilityId = local.capabilityId,
                    local = local,
                    remote = remote,
                ),
            )
        } else {
            current = listOf(if (local.updatedAt >= remote.updatedAt) local else remote)
            emptyList()
        }
    }
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

    fun readText(path: String): String = files.getValue(path).decodeToString()

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
        deviceId = "device-1",
        bootstrapToken = "",
    )

private fun sampleSecretEnvelope(secretId: String): SecretEnvelopePayload =
    SecretEnvelopePayload(
        secretId = secretId,
        scope = SecretScopePayload.SYNC_ENCRYPTED,
        version = 1,
        kdf = "PBKDF2WithHmacSHA256",
        iterations = 120_000,
        saltBase64 = "c2FsdA==",
        ivBase64 = "aXY=",
        cipherTextBase64 = "Y2lwaGVy",
        checksumBase64 = "Y2hlY2tzdW0=",
        updatedAt = 1711000000000,
    )
