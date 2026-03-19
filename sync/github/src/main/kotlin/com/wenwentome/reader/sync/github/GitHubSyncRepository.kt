package com.wenwentome.reader.sync.github

import com.wenwentome.reader.core.model.BookAsset
import com.wenwentome.reader.core.model.BookRecord
import com.wenwentome.reader.core.model.ReadingState
import com.wenwentome.reader.core.model.RemoteBinding
import com.wenwentome.reader.core.model.SourceDefinition
import com.wenwentome.reader.core.model.SyncSnapshot
import java.io.InputStream
import java.time.Instant
import java.util.UUID

interface GitHubSyncService {
    suspend fun pushSnapshot(auth: GitHubAuthConfig)
    suspend fun pullLatestSnapshot(auth: GitHubAuthConfig): RestoredSnapshot
}

interface BookRecordSyncStore {
    suspend fun getAll(): List<BookRecord>
    suspend fun replaceAll(records: List<BookRecord>)
}

interface ReadingStateSyncStore {
    suspend fun getAll(): List<ReadingState>
    suspend fun replaceAll(states: List<ReadingState>)
}

interface RemoteBindingSyncStore {
    suspend fun getAll(): List<RemoteBinding>
    suspend fun replaceAll(bindings: List<RemoteBinding>)
}

interface SourceDefinitionSyncStore {
    suspend fun getAll(): List<SourceDefinition>
    suspend fun replaceAll(definitions: List<SourceDefinition>)
}

interface BookAssetSyncStore {
    suspend fun getAll(): List<BookAsset>
    suspend fun replaceAll(assets: List<BookAsset>)
}

interface SyncPreferencesStore {
    suspend fun exportSnapshot(): PreferencesSnapshot
    suspend fun importSnapshot(snapshot: PreferencesSnapshot)
    suspend fun getOrCreateDeviceId(): String
}

interface SyncFileStore {
    fun open(storageUri: String): InputStream
    fun persistOriginal(bookId: String, extension: String, bytes: ByteArray): String
}

class GitHubSyncRepository(
    private val api: GitHubContentApi,
    private val serializer: SyncManifestSerializer,
    private val bookRecordStore: BookRecordSyncStore,
    private val readingStateStore: ReadingStateSyncStore,
    private val remoteBindingStore: RemoteBindingSyncStore,
    private val sourceDefinitionStore: SourceDefinitionSyncStore,
    private val bookAssetStore: BookAssetSyncStore,
    private val preferencesStore: SyncPreferencesStore,
    private val fileStore: SyncFileStore,
    private val snapshotIdFactory: () -> String = { UUID.randomUUID().toString() },
    private val revisionFactory: () -> String = { Instant.now().toString() },
    private val mergedAtProvider: () -> Long = { System.currentTimeMillis() },
) : GitHubSyncService {
    override suspend fun pushSnapshot(auth: GitHubAuthConfig) {
        val manifest = SyncManifest()
        val bookRecords = bookRecordStore.getAll()
        val readingStates = readingStateStore.getAll()
        val remoteBindings = remoteBindingStore.getAll()
        val sourceDefinitions = sourceDefinitionStore.getAll()
        val preferences = preferencesStore.exportSnapshot()
        val assets = bookAssetStore.getAll()
        val existingSha = buildList {
            add(manifest.bookRecordsPath)
            add(manifest.readingStatesPath)
            add(manifest.remoteBindingsPath)
            add(manifest.sourceDefinitionsPath)
            add(manifest.preferencesPath)
            add(manifest.assetIndexPath)
            add(manifest.snapshotPath)
            addAll(assets.map { it.syncPath })
        }.associateWith { path ->
            api.findShaOrNull(auth, path)
        }

        api.putJson(auth, manifest.bookRecordsPath, serializer.encodeBookRecords(bookRecords), existingSha[manifest.bookRecordsPath])
        api.putJson(auth, manifest.readingStatesPath, serializer.encodeReadingStates(readingStates), existingSha[manifest.readingStatesPath])
        api.putJson(auth, manifest.remoteBindingsPath, serializer.encodeRemoteBindings(remoteBindings), existingSha[manifest.remoteBindingsPath])
        api.putJson(auth, manifest.sourceDefinitionsPath, serializer.encodeSourceDefinitions(sourceDefinitions), existingSha[manifest.sourceDefinitionsPath])
        api.putJson(auth, manifest.preferencesPath, serializer.encodePreferences(preferences), existingSha[manifest.preferencesPath])
        api.putJson(auth, manifest.assetIndexPath, serializer.encodeAssets(assets), existingSha[manifest.assetIndexPath])

        assets.forEach { asset ->
            val bytes = fileStore.open(asset.storageUri).use { input ->
                input.readBytes()
            }
            api.putBinary(auth, asset.syncPath, bytes, existingSha[asset.syncPath])
        }

        val snapshot = SyncSnapshot(
            snapshotId = snapshotIdFactory(),
            revision = revisionFactory(),
            deviceId = preferencesStore.getOrCreateDeviceId(),
            mergedAt = mergedAtProvider(),
            manifestJson = serializer.encodeManifest(manifest),
        )
        api.putJson(auth, manifest.snapshotPath, serializer.encodeSnapshot(snapshot), existingSha[manifest.snapshotPath])
    }

    override suspend fun pullLatestSnapshot(auth: GitHubAuthConfig): RestoredSnapshot {
        val (snapshotJson, _) = api.getJson(auth, SyncManifest().snapshotPath)
        val snapshot = serializer.decodeSnapshot(snapshotJson)
        val manifest = serializer.decodeManifest(snapshot.manifestJson)

        val bookRecords = serializer.decodeBookRecords(api.getJson(auth, manifest.bookRecordsPath).first)
        val readingStates = serializer.decodeReadingStates(api.getJson(auth, manifest.readingStatesPath).first)
        val remoteBindings = serializer.decodeRemoteBindings(api.getJson(auth, manifest.remoteBindingsPath).first)
        val sourceDefinitions = serializer.decodeSourceDefinitions(api.getJson(auth, manifest.sourceDefinitionsPath).first)
        val preferences = serializer.decodePreferences(api.getJson(auth, manifest.preferencesPath).first)
        val assetIndex = serializer.decodeAssets(api.getJson(auth, manifest.assetIndexPath).first)

        bookRecordStore.replaceAll(bookRecords)
        readingStateStore.replaceAll(readingStates)
        remoteBindingStore.replaceAll(remoteBindings)
        sourceDefinitionStore.replaceAll(sourceDefinitions)
        preferencesStore.importSnapshot(preferences)

        val restoredAssets = assetIndex.map { asset ->
            val (bytes, _) = api.getBinary(auth, asset.syncPath)
            val storageUri = fileStore.persistOriginal(asset.bookId, asset.syncPath.substringAfterLast('.', "bin"), bytes)
            asset.copy(storageUri = storageUri)
        }
        bookAssetStore.replaceAll(restoredAssets)

        return RestoredSnapshot(snapshot = snapshot, assets = restoredAssets)
    }
}
