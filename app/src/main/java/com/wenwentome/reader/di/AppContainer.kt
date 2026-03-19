package com.wenwentome.reader.di

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.wenwentome.reader.core.database.ReaderDatabase
import com.wenwentome.reader.core.database.datastore.PreferencesSnapshot as LocalPreferencesSnapshot
import com.wenwentome.reader.core.database.datastore.ReaderPreferencesStore
import com.wenwentome.reader.core.database.toEntity
import com.wenwentome.reader.core.database.toModel
import com.wenwentome.reader.data.localbooks.EpubBookParser
import com.wenwentome.reader.data.localbooks.ImportLocalBookUseCase
import com.wenwentome.reader.data.localbooks.LocalBookContentRepository
import com.wenwentome.reader.data.localbooks.LocalBookFileStore
import com.wenwentome.reader.data.localbooks.LocalBookImportRepository
import com.wenwentome.reader.data.localbooks.TxtBookParser
import com.wenwentome.reader.feature.settings.StoredSyncConfig
import com.wenwentome.reader.feature.settings.SyncSettingsConfigStore
import com.wenwentome.reader.sync.github.BookAssetSyncStore
import com.wenwentome.reader.sync.github.BookRecordSyncStore
import com.wenwentome.reader.sync.github.GitHubContentApi
import com.wenwentome.reader.sync.github.GitHubSyncRepository
import com.wenwentome.reader.sync.github.PreferencesSnapshot as RemotePreferencesSnapshot
import com.wenwentome.reader.sync.github.ReadingStateSyncStore
import com.wenwentome.reader.sync.github.RemoteBindingSyncStore
import com.wenwentome.reader.sync.github.SourceDefinitionSyncStore
import com.wenwentome.reader.sync.github.SyncFileStore
import com.wenwentome.reader.sync.github.SyncManifestSerializer
import com.wenwentome.reader.sync.github.SyncPreferencesStore
import kotlinx.coroutines.flow.map

class AppContainer(private val application: Application) {
    val appContext: Context = application

    val database: ReaderDatabase by lazy {
        Room.databaseBuilder(
            appContext,
            ReaderDatabase::class.java,
            "reader.db",
        ).build()
    }

    val fileStore: LocalBookFileStore by lazy {
        LocalBookFileStore(filesDir = appContext.filesDir)
    }

    val preferencesStore: ReaderPreferencesStore by lazy {
        ReaderPreferencesStore(appContext)
    }

    val localBookContentRepository: LocalBookContentRepository by lazy {
        LocalBookContentRepository(
            bookAssetDao = database.bookAssetDao(),
            fileStore = fileStore,
        )
    }

    private val localBookImportRepository: LocalBookImportRepository by lazy {
        LocalBookImportRepository(
            txtParser = TxtBookParser(),
            epubParser = EpubBookParser(),
            fileStore = fileStore,
            bookRecordDao = database.bookRecordDao(),
            readingStateDao = database.readingStateDao(),
            bookAssetDao = database.bookAssetDao(),
        )
    }

    val importLocalBook: ImportLocalBookUseCase by lazy {
        ImportLocalBookUseCase(
            contentResolver = appContext.contentResolver,
            repository = localBookImportRepository,
        )
    }

    val syncSettingsConfigStore: SyncSettingsConfigStore by lazy {
        object : SyncSettingsConfigStore {
            override val syncConfig =
                preferencesStore.syncConfig.map { config ->
                    StoredSyncConfig(
                        owner = config.owner,
                        repo = config.repo,
                        branch = config.branch,
                        token = config.token,
                    )
                }

            override suspend fun saveConfig(config: StoredSyncConfig) {
                preferencesStore.saveGitHubConfig(
                    owner = config.owner,
                    repo = config.repo,
                    branch = config.branch,
                    token = config.token,
                )
            }
        }
    }

    val gitHubSyncRepository: GitHubSyncRepository by lazy {
        GitHubSyncRepository(
            api = GitHubContentApi("https://api.github.com"),
            serializer = SyncManifestSerializer(),
            bookRecordStore = object : BookRecordSyncStore {
                override suspend fun getAll() =
                    database.bookRecordDao().getAll().map { it.toModel() }

                override suspend fun replaceAll(records: List<com.wenwentome.reader.core.model.BookRecord>) {
                    database.bookRecordDao().replaceAll(records.map { it.toEntity() })
                }
            },
            readingStateStore = object : ReadingStateSyncStore {
                override suspend fun getAll() =
                    database.readingStateDao().getAll().map { it.toModel() }

                override suspend fun replaceAll(states: List<com.wenwentome.reader.core.model.ReadingState>) {
                    database.readingStateDao().replaceAll(states.map { it.toEntity() })
                }
            },
            remoteBindingStore = object : RemoteBindingSyncStore {
                override suspend fun getAll() =
                    database.remoteBindingDao().getAll().map { it.toModel() }

                override suspend fun replaceAll(bindings: List<com.wenwentome.reader.core.model.RemoteBinding>) {
                    database.remoteBindingDao().replaceAll(bindings.map { it.toEntity() })
                }
            },
            sourceDefinitionStore = object : SourceDefinitionSyncStore {
                override suspend fun getAll() =
                    database.sourceDefinitionDao().getAll().map { it.toModel() }

                override suspend fun replaceAll(definitions: List<com.wenwentome.reader.core.model.SourceDefinition>) {
                    database.sourceDefinitionDao().replaceAll(definitions.map { it.toEntity() })
                }
            },
            bookAssetStore = object : BookAssetSyncStore {
                override suspend fun getAll() =
                    database.bookAssetDao().getAll().map { it.toModel() }

                override suspend fun replaceAll(assets: List<com.wenwentome.reader.core.model.BookAsset>) {
                    database.bookAssetDao().replaceAll(assets.map { it.toEntity() })
                }
            },
            preferencesStore = object : SyncPreferencesStore {
                override suspend fun exportSnapshot(): RemotePreferencesSnapshot =
                    preferencesStore.exportSnapshot().toRemoteSnapshot()

                override suspend fun importSnapshot(snapshot: RemotePreferencesSnapshot) {
                    preferencesStore.importSnapshot(snapshot.toLocalSnapshot())
                }

                override suspend fun getOrCreateDeviceId(): String =
                    preferencesStore.getOrCreateDeviceId()
            },
            fileStore = object : SyncFileStore {
                override fun open(storageUri: String) =
                    fileStore.open(storageUri)

                override fun persistOriginal(bookId: String, extension: String, bytes: ByteArray): String =
                    fileStore.persistOriginal(bookId, extension, bytes)
            },
        )
    }
}

private fun LocalPreferencesSnapshot.toRemoteSnapshot(): RemotePreferencesSnapshot =
    RemotePreferencesSnapshot(
        owner = owner,
        repo = repo,
        branch = branch,
        token = token,
        deviceId = deviceId,
    )

private fun RemotePreferencesSnapshot.toLocalSnapshot(): LocalPreferencesSnapshot =
    LocalPreferencesSnapshot(
        owner = owner,
        repo = repo,
        branch = branch,
        token = token,
        deviceId = deviceId,
    )
