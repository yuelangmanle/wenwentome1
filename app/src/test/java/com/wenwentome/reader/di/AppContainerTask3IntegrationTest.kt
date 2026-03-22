package com.wenwentome.reader.di

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.wenwentome.reader.core.database.ReaderDatabase
import com.wenwentome.reader.core.database.entity.ApiCapabilityBindingEntity
import com.wenwentome.reader.core.database.entity.ApiProviderEntity
import com.wenwentome.reader.core.model.ProviderKind
import com.wenwentome.reader.core.model.ProviderSecretSyncMode
import com.wenwentome.reader.data.apihub.local.SharedPreferencesApiSecretLocalStore
import com.wenwentome.reader.sync.github.GitHubAuthConfig
import com.wenwentome.reader.sync.github.GitHubContentApi
import com.wenwentome.reader.sync.github.SyncManifestSerializer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Base64

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class AppContainerTask3IntegrationTest {
    @Test
    fun gitHubSyncRepository_pushSnapshot_exportsEncryptedProviderSecretsThroughRealContainerWiring() = runTest {
        clearSecretStores()
        val server = MockWebServer()
        val dispatcher = FakeGitHubDispatcher()
        server.dispatcher = dispatcher
        server.start()
        val database = testDatabase("push")
        try {
            val container =
                AppContainer(
                    application = application(),
                    databaseOverride = database,
                    gitHubContentApiOverride = GitHubContentApi(server.url("/").toString()),
                )
            database.apiProviderDao().upsert(
                ApiProviderEntity(
                    providerId = "provider-openai",
                    displayName = "OpenAI",
                    providerKind = ProviderKind.OPENAI_COMPATIBLE,
                    secretSyncMode = ProviderSecretSyncMode.SYNC_ENCRYPTED,
                ),
            )
            container.apiSecretLocalStore.save("provider-openai", "sk-live-provider-secret")

            container.gitHubSyncRepository.pushSnapshot(
                auth = GitHubAuthConfig(owner = "me", repo = "books", branch = "main", token = "ghp_bootstrap", syncPassword = "sync-pass"),
            )

            val secretsJson = dispatcher.readText("data/secret-envelopes.json")
            assertTrue(secretsJson.contains("provider-openai"))
            assertFalse(secretsJson.contains("ghp_bootstrap"))
        } finally {
            database.close()
            server.shutdown()
        }
    }

    @Test
    fun gitHubSyncRepository_pullLatestSnapshot_mergesCapabilityBindingsThroughRealContainerBridge() = runTest {
        clearSecretStores()
        val server = MockWebServer()
        val dispatcher = FakeGitHubDispatcher()
        server.dispatcher = dispatcher
        server.start()
        val remoteDatabase = testDatabase("remote")
        val localDatabase = testDatabase("local")
        try {
            remoteDatabase.apiCapabilityBindingDao().upsert(
                ApiCapabilityBindingEntity(
                    capabilityId = "reader.summary",
                    primaryProviderId = "provider-remote",
                    primaryModelId = "model-remote",
                    updatedAt = 12_000L,
                ),
            )
            val remoteContainer =
                AppContainer(
                    application = application(),
                    databaseOverride = remoteDatabase,
                    gitHubContentApiOverride = GitHubContentApi(server.url("/").toString()),
                )
            remoteContainer.gitHubSyncRepository.pushSnapshot(
                auth = GitHubAuthConfig(owner = "me", repo = "books", branch = "main", token = "ghp_bootstrap"),
            )

            localDatabase.apiCapabilityBindingDao().upsert(
                ApiCapabilityBindingEntity(
                    capabilityId = "reader.summary",
                    primaryProviderId = "provider-local",
                    primaryModelId = "model-local",
                    updatedAt = 10_000L,
                ),
            )
            val localContainer =
                AppContainer(
                    application = application(),
                    databaseOverride = localDatabase,
                    gitHubContentApiOverride = GitHubContentApi(server.url("/").toString()),
                )

            val restored =
                localContainer.gitHubSyncRepository.pullLatestSnapshot(
                    auth = GitHubAuthConfig(owner = "me", repo = "books", branch = "main", token = "ghp_bootstrap"),
                )

            assertEquals(1, restored.pendingConflicts.size)
            val stored = localDatabase.apiCapabilityBindingDao().getAll().single()
            assertEquals("provider-local", stored.primaryProviderId)
        } finally {
            remoteDatabase.close()
            localDatabase.close()
            server.shutdown()
        }
    }

    @Test
    fun gitHubSyncRepository_pullLatestSnapshot_restoresSecretsIndividuallyThroughRealContainerWiring() = runTest {
        clearSecretStores()
        val server = MockWebServer()
        val dispatcher = FakeGitHubDispatcher()
        server.dispatcher = dispatcher
        server.start()
        val remoteDatabase = testDatabase("secret-remote")
        val localDatabase = testDatabase("secret-local")
        try {
            val remoteContainer =
                AppContainer(
                    application = application(),
                    databaseOverride = remoteDatabase,
                    gitHubContentApiOverride = GitHubContentApi(server.url("/").toString()),
                )
            remoteDatabase.apiProviderDao().upsertAll(
                listOf(
                    ApiProviderEntity(
                        providerId = "provider-good",
                        displayName = "Good",
                        providerKind = ProviderKind.OPENAI_COMPATIBLE,
                        secretSyncMode = ProviderSecretSyncMode.SYNC_ENCRYPTED,
                        updatedAt = 10_000L,
                    ),
                    ApiProviderEntity(
                        providerId = "provider-bad",
                        displayName = "Bad",
                        providerKind = ProviderKind.OPENAI_COMPATIBLE,
                        secretSyncMode = ProviderSecretSyncMode.SYNC_ENCRYPTED,
                        updatedAt = 11_000L,
                    ),
                ),
            )
            remoteContainer.apiSecretLocalStore.save("provider-good", "sk-good")
            remoteContainer.apiSecretLocalStore.save("provider-bad", "sk-bad")

            remoteContainer.gitHubSyncRepository.pushSnapshot(
                auth = GitHubAuthConfig(owner = "me", repo = "books", branch = "main", token = "ghp_bootstrap", syncPassword = "sync-pass"),
            )

            val serializer = SyncManifestSerializer()
            val corruptedEnvelopes =
                serializer.decodeSecretEnvelopes(dispatcher.readText("data/secret-envelopes.json"))
                    .map { payload ->
                        if (payload.secretId == "provider-bad") {
                            payload.copy(cipherTextBase64 = "not-valid-base64")
                        } else {
                            payload
                        }
                    }
            dispatcher.writeText("data/secret-envelopes.json", serializer.encodeSecretEnvelopes(corruptedEnvelopes))

            clearSecretStores()
            val localContainer =
                AppContainer(
                    application = application(),
                    databaseOverride = localDatabase,
                    gitHubContentApiOverride = GitHubContentApi(server.url("/").toString()),
                )

            val restored =
                localContainer.gitHubSyncRepository.pullLatestSnapshot(
                    auth = GitHubAuthConfig(owner = "me", repo = "books", branch = "main", token = "ghp_bootstrap", syncPassword = "sync-pass"),
                )

            assertEquals("sk-good", localContainer.apiSecretLocalStore.read("provider-good"))
            assertNull(localContainer.apiSecretLocalStore.read("provider-bad"))
            assertEquals(listOf("provider-bad"), restored.pendingSecretRestore.map { it.secretId })
        } finally {
            clearSecretStores()
            remoteDatabase.close()
            localDatabase.close()
            server.shutdown()
        }
    }

    @Test
    fun pullLatestSnapshot_migratesLegacyBootstrapTokenIntoLocalSecretStore() = runTest {
        clearSecretStores()
        val server = MockWebServer()
        val dispatcher = FakeGitHubDispatcher()
        server.dispatcher = dispatcher
        server.start()
        val remoteDatabase = testDatabase("legacy-remote")
        val localDatabase = testDatabase("legacy-local")
        try {
            val remoteContainer =
                AppContainer(
                    application = application(),
                    databaseOverride = remoteDatabase,
                    gitHubContentApiOverride = GitHubContentApi(server.url("/").toString()),
                )
            remoteContainer.gitHubSyncRepository.pushSnapshot(
                auth = GitHubAuthConfig(owner = "me", repo = "books", branch = "main", token = "ghp_bootstrap"),
            )

            dispatcher.writeText(
                "data/preferences.json",
                """
                {
                  "owner": "legacy-owner",
                  "repo": "legacy-repo",
                  "branch": "legacy-branch",
                  "deviceId": "legacy-device",
                  "token": "ghp_legacy_token"
                }
                """.trimIndent(),
            )

            clearSecretStores()
            val localContainer =
                AppContainer(
                    application = application(),
                    databaseOverride = localDatabase,
                    gitHubContentApiOverride = GitHubContentApi(server.url("/").toString()),
                )

            localContainer.gitHubSyncRepository.pullLatestSnapshot(
                auth = GitHubAuthConfig(owner = "me", repo = "books", branch = "main", token = "ghp_bootstrap"),
            )

            val restoredConfig = localContainer.syncSettingsConfigStore.syncConfig.first()
            assertEquals("ghp_legacy_token", restoredConfig.bootstrapToken)
            assertEquals("legacy-owner", restoredConfig.owner)
            assertEquals("legacy-repo", restoredConfig.repo)
            assertEquals("legacy-branch", restoredConfig.branch)
        } finally {
            clearSecretStores()
            remoteDatabase.close()
            localDatabase.close()
            server.shutdown()
        }
    }

    @Test
    fun appContainer_realSecretWiring_isNotPlainSharedPreferencesStore() {
        clearSecretStores()
        val container = AppContainer(application = application())

        assertFalse(container.apiSecretLocalStore is SharedPreferencesApiSecretLocalStore)
    }

    private fun application(): Application =
        ApplicationProvider.getApplicationContext()

    private fun clearSecretStores() {
        val application = application()
        application.getSharedPreferences("api-hub-secrets", Context.MODE_PRIVATE).edit().clear().commit()
        application.getSharedPreferences("bootstrap-secrets", Context.MODE_PRIVATE).edit().clear().commit()
    }

    private fun testDatabase(name: String): ReaderDatabase =
        Room.inMemoryDatabaseBuilder(
            application(),
            ReaderDatabase::class.java,
        )
            .setQueryExecutor { runnable -> runnable.run() }
            .setTransactionExecutor { runnable -> runnable.run() }
            .allowMainThreadQueries()
            .build()

    private class FakeGitHubDispatcher : Dispatcher() {
        private val files = linkedMapOf<String, ByteArray>()
        private var revision = 0

        fun readText(path: String): String = files.getValue(path).decodeToString()

        fun writeText(path: String, text: String) {
            files[path] = text.encodeToByteArray()
        }

        override fun dispatch(request: RecordedRequest): MockResponse {
            val path = request.requestUrl?.encodedPath.orEmpty()
            return when {
                request.method == "PUT" && path.contains("/contents/") -> {
                    val contentPath = path.substringAfter("/contents/")
                    val encoded =
                        request.body.readUtf8()
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
                        """.trimIndent(),
                    )
                }

                else -> MockResponse().setResponseCode(404)
            }
        }
    }
}
