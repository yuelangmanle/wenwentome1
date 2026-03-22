package com.wenwentome.reader.di

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.wenwentome.reader.bridge.source.RealSourceBridgeRepository
import com.wenwentome.reader.bridge.source.SourceDefinitionProvider
import com.wenwentome.reader.bridge.source.SourceBridgeRepository
import com.wenwentome.reader.bridge.source.SourceRuleParser
import com.wenwentome.reader.core.database.MIGRATION_2_3
import com.wenwentome.reader.core.database.MIGRATION_3_4
import com.wenwentome.reader.core.database.ReaderDatabase
import com.wenwentome.reader.core.database.datastore.PreferencesSnapshot as LocalPreferencesSnapshot
import com.wenwentome.reader.core.database.datastore.ReaderPreferencesStore
import com.wenwentome.reader.core.database.toEntity
import com.wenwentome.reader.core.database.toModel
import com.wenwentome.reader.data.apihub.ApiHubModule
import com.wenwentome.reader.data.apihub.local.ApiSecretLocalStore
import com.wenwentome.reader.data.apihub.local.createSecureApiSecretLocalStore
import com.wenwentome.reader.data.apihub.secret.SecretEnvelopeV1
import com.wenwentome.reader.data.apihub.secret.SecretScope
import com.wenwentome.reader.data.apihub.secret.SecretSyncCrypto
import com.wenwentome.reader.data.apihub.sync.ApiHubMergeResolver
import com.wenwentome.reader.data.apihub.sync.MergeBindingResult
import com.wenwentome.reader.data.localbooks.EpubBookParser
import com.wenwentome.reader.data.localbooks.ImportLocalBookUseCase
import com.wenwentome.reader.data.localbooks.LocalBookContentRepository
import com.wenwentome.reader.data.localbooks.LocalBookFileStore
import com.wenwentome.reader.data.localbooks.LocalBookImportRepository
import com.wenwentome.reader.data.localbooks.TxtBookParser
import com.wenwentome.reader.core.model.ProviderSecretSyncMode
import com.wenwentome.reader.feature.settings.StoredSyncConfig
import com.wenwentome.reader.feature.settings.ChangelogRepository
import com.wenwentome.reader.feature.settings.SyncSettingsConfigStore
import com.wenwentome.reader.feature.discover.RefreshRemoteBook
import com.wenwentome.reader.feature.discover.RefreshRemoteBookUseCase
import com.wenwentome.reader.sync.github.ApiCapabilityBindingSyncStore
import com.wenwentome.reader.sync.github.BookAssetSyncStore
import com.wenwentome.reader.sync.github.BookRecordSyncStore
import com.wenwentome.reader.sync.github.CapabilityBindingConflict
import com.wenwentome.reader.sync.github.GitHubContentApi
import com.wenwentome.reader.sync.github.GitHubSyncRepository
import com.wenwentome.reader.sync.github.PreferencesSnapshot as RemotePreferencesSnapshot
import com.wenwentome.reader.sync.github.ReadingStateSyncStore
import com.wenwentome.reader.sync.github.RemoteBindingSyncStore
import com.wenwentome.reader.sync.github.SecretEnvelopePayload
import com.wenwentome.reader.sync.github.SecretScopePayload
import com.wenwentome.reader.sync.github.SourceDefinitionSyncStore
import com.wenwentome.reader.sync.github.SyncSecretStore
import com.wenwentome.reader.sync.github.SyncFileStore
import com.wenwentome.reader.sync.github.SyncManifestSerializer
import com.wenwentome.reader.sync.github.SyncPreferencesStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map

class AppContainer(
    private val application: Application,
    private val databaseOverride: ReaderDatabase? = null,
    private val sourceBridgeRepositoryOverride: SourceBridgeRepository? = null,
    private val discoverIoDispatcherOverride: CoroutineDispatcher? = null,
    private val gitHubContentApiOverride: GitHubContentApi? = null,
) {
    val apiHubModule: ApiHubModule by lazy {
        ApiHubModule()
    }

    val appContext: Context = application

    val database: ReaderDatabase by lazy {
        databaseOverride ?: Room.databaseBuilder(
            appContext,
            ReaderDatabase::class.java,
            "reader.db",
        )
            .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
            .build()
    }

    val fileStore: LocalBookFileStore by lazy {
        LocalBookFileStore(filesDir = appContext.filesDir)
    }

    val preferencesStore: ReaderPreferencesStore by lazy {
        ReaderPreferencesStore(appContext)
    }

    private val bootstrapSecretStore by lazy {
        createSecureApiSecretLocalStore(
            context = appContext,
            preferencesName = "bootstrap-secrets",
        )
    }

    val apiSecretLocalStore: ApiSecretLocalStore by lazy {
        createSecureApiSecretLocalStore(
            context = appContext,
            preferencesName = "api-hub-secrets",
        )
    }

    private val syncCrypto by lazy {
        SecretSyncCrypto()
    }

    private val apiHubMergeResolver by lazy {
        ApiHubMergeResolver()
    }

    private val syncSecretStore: SyncSecretStore by lazy {
        object : SyncSecretStore {
            private var pendingRestore: List<SecretEnvelopePayload> = emptyList()

            override suspend fun exportEncryptedSecrets(syncPassword: String): List<SecretEnvelopePayload> =
                database.apiProviderDao().getAll()
                    .filter { provider -> provider.secretSyncMode == ProviderSecretSyncMode.SYNC_ENCRYPTED }
                    .mapNotNull { provider ->
                        val plainSecret = apiSecretLocalStore.read(provider.providerId) ?: return@mapNotNull null
                        syncCrypto.encrypt(
                            secretId = provider.providerId,
                            plainText = plainSecret,
                            password = syncPassword,
                            scope = SecretScope.SYNC_ENCRYPTED,
                            updatedAt = provider.updatedAt,
                        ).toPayload()
                    }

            override suspend fun cachePendingSecretRestore(secretEnvelopes: List<SecretEnvelopePayload>) {
                pendingRestore = secretEnvelopes
            }

            override suspend fun restorePendingSecrets(syncPassword: String): List<SecretEnvelopePayload> {
                val remaining = mutableListOf<SecretEnvelopePayload>()
                pendingRestore.forEach { payload ->
                    val envelope = payload.toEnvelope()
                    val plainSecret =
                        runCatching { syncCrypto.decrypt(envelope, syncPassword) }
                            .getOrElse {
                                remaining += payload
                                return@forEach
                            }
                    apiSecretLocalStore.save(envelope.secretId, plainSecret)
                }
                pendingRestore = remaining
                return pendingRestore
            }
        }
    }

    private val capabilityBindingSyncStore: ApiCapabilityBindingSyncStore by lazy {
        object : ApiCapabilityBindingSyncStore {
            override suspend fun exportBindings() =
                database.apiCapabilityBindingDao().getAll().map { it.toModel() }

            override suspend fun mergeIncomingBindings(
                incoming: List<com.wenwentome.reader.core.model.ApiCapabilityBinding>,
            ): List<CapabilityBindingConflict> {
                val localById =
                    database.apiCapabilityBindingDao().getAll()
                        .map { it.toModel() }
                        .associateBy { it.capabilityId }
                val incomingById = incoming.associateBy { it.capabilityId }
                val merged = mutableListOf<com.wenwentome.reader.core.model.ApiCapabilityBinding>()
                val conflicts = mutableListOf<CapabilityBindingConflict>()

                (localById.keys + incomingById.keys)
                    .sorted()
                    .forEach { capabilityId ->
                        val local = localById[capabilityId]
                        val remote = incomingById[capabilityId]
                        when {
                            local == null && remote != null -> merged += remote
                            local != null && remote == null -> merged += local
                            local != null && remote != null ->
                                when (val result = apiHubMergeResolver.mergeBinding(local, remote)) {
                                    is MergeBindingResult.Resolved -> merged += result.binding
                                    is MergeBindingResult.Conflict -> {
                                        merged += local
                                        conflicts +=
                                            CapabilityBindingConflict(
                                                capabilityId = capabilityId,
                                                local = result.local,
                                                remote = result.remote,
                                            )
                                    }
                                }
                        }
                    }

                database.apiCapabilityBindingDao().replaceAll(merged.map { it.toEntity() })
                return conflicts
            }
        }
    }

    val localBookContentRepository: LocalBookContentRepository by lazy {
        LocalBookContentRepository(
            bookAssetDao = database.bookAssetDao(),
            fileStore = fileStore,
        )
    }

    val epubBookParser: EpubBookParser by lazy {
        EpubBookParser()
    }

    private val localBookImportRepository: LocalBookImportRepository by lazy {
        LocalBookImportRepository(
            txtParser = TxtBookParser(),
            epubParser = epubBookParser,
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

    val sourceRuleParser: SourceRuleParser by lazy {
        SourceRuleParser()
    }

    val sourceBridgeRepository: SourceBridgeRepository by lazy {
        sourceBridgeRepositoryOverride ?: RealSourceBridgeRepository(
            sourceProvider = object : SourceDefinitionProvider {
                override suspend fun getAll() =
                    database.sourceDefinitionDao().getAll()
                        .map { it.toModel() }
                        .mapNotNull { definition ->
                            val raw = definition.rawDefinition ?: return@mapNotNull null
                            runCatching {
                                val parsed = sourceRuleParser.parse(raw)
                                parsed.copy(
                                    id = definition.sourceId,
                                    name = definition.sourceName,
                                    group = definition.group,
                                    baseUrl = definition.sourceUrl ?: parsed.baseUrl,
                                    enabled = definition.enabled,
                                )
                            }.getOrNull()
                        }
            },
        )
    }

    val refreshRemoteBook: RefreshRemoteBook by lazy {
        RefreshRemoteBookUseCase(
            sourceBridgeRepository = sourceBridgeRepository,
            remoteBindingDao = database.remoteBindingDao(),
            readingStateDao = database.readingStateDao(),
        )
    }

    val discoverIoDispatcher: CoroutineDispatcher by lazy {
        discoverIoDispatcherOverride ?: Dispatchers.IO
    }

    val syncSettingsConfigStore: SyncSettingsConfigStore by lazy {
        object : SyncSettingsConfigStore {
            override val syncConfig =
                preferencesStore.syncConfig.map { config ->
                    val storedBootstrapToken = bootstrapSecretStore.read(GITHUB_BOOTSTRAP_SECRET_ID).orEmpty()
                    val bootstrapToken = storedBootstrapToken.ifEmpty { config.bootstrapToken }
                    if (storedBootstrapToken.isBlank() && config.bootstrapToken.isNotBlank()) {
                        bootstrapSecretStore.save(GITHUB_BOOTSTRAP_SECRET_ID, config.bootstrapToken)
                        preferencesStore.saveGitHubConfig(
                            owner = config.owner,
                            repo = config.repo,
                            branch = config.branch,
                        )
                    }
                    StoredSyncConfig(
                        owner = config.owner,
                        repo = config.repo,
                        branch = config.branch,
                        bootstrapToken = bootstrapToken,
                    )
                }

            override suspend fun saveConfig(config: StoredSyncConfig) {
                preferencesStore.saveGitHubConfig(owner = config.owner, repo = config.repo, branch = config.branch)
                if (config.bootstrapToken.isBlank()) {
                    bootstrapSecretStore.delete(GITHUB_BOOTSTRAP_SECRET_ID)
                } else {
                    bootstrapSecretStore.save(GITHUB_BOOTSTRAP_SECRET_ID, config.bootstrapToken)
                }
            }
        }
    }

    val changelogRepository: ChangelogRepository by lazy {
        ChangelogRepository(
            loadJson = {
                appContext.assets.open("changelog.json")
                    .bufferedReader()
                    .use { it.readText() }
            },
        )
    }

    val gitHubSyncRepository: GitHubSyncRepository by lazy {
        GitHubSyncRepository(
            api = gitHubContentApiOverride ?: GitHubContentApi("https://api.github.com"),
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
                    if (snapshot.bootstrapToken.isNotBlank() &&
                        bootstrapSecretStore.read(GITHUB_BOOTSTRAP_SECRET_ID).isNullOrBlank()
                    ) {
                        bootstrapSecretStore.save(GITHUB_BOOTSTRAP_SECRET_ID, snapshot.bootstrapToken)
                    }
                }

                override suspend fun getOrCreateDeviceId(): String =
                    preferencesStore.getOrCreateDeviceId()
            },
            secretStore = syncSecretStore,
            capabilityBindingStore = capabilityBindingSyncStore,
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
        deviceId = deviceId,
    )

private fun RemotePreferencesSnapshot.toLocalSnapshot(): LocalPreferencesSnapshot =
    LocalPreferencesSnapshot(
        owner = owner,
        repo = repo,
        branch = branch,
        deviceId = deviceId,
    )

private fun SecretEnvelopeV1.toPayload(): SecretEnvelopePayload =
    SecretEnvelopePayload(
        secretId = secretId,
        scope =
            when (scope) {
                SecretScope.LOCAL_ONLY -> SecretScopePayload.LOCAL_ONLY
                SecretScope.SYNC_ENCRYPTED -> SecretScopePayload.SYNC_ENCRYPTED
            },
        version = version,
        kdf = kdf,
        iterations = iterations,
        saltBase64 = saltBase64,
        ivBase64 = ivBase64,
        cipherTextBase64 = cipherTextBase64,
        checksumBase64 = checksumBase64,
        updatedAt = updatedAt,
    )

private fun SecretEnvelopePayload.toEnvelope(): SecretEnvelopeV1 =
    SecretEnvelopeV1(
        secretId = secretId,
        version = version,
        scope =
            when (scope) {
                SecretScopePayload.LOCAL_ONLY -> SecretScope.LOCAL_ONLY
                SecretScopePayload.SYNC_ENCRYPTED -> SecretScope.SYNC_ENCRYPTED
            },
        kdf = kdf,
        iterations = iterations,
        saltBase64 = saltBase64,
        ivBase64 = ivBase64,
        cipherTextBase64 = cipherTextBase64,
        checksumBase64 = checksumBase64,
        updatedAt = updatedAt,
    )

private const val GITHUB_BOOTSTRAP_SECRET_ID = "github.bootstrap.token"
