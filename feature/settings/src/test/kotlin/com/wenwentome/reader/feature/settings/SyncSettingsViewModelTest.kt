package com.wenwentome.reader.feature.settings

import com.wenwentome.reader.sync.github.GitHubAuthConfig
import com.wenwentome.reader.sync.github.GitHubSyncService
import com.wenwentome.reader.sync.github.RestoredSnapshot
import com.wenwentome.reader.core.model.SyncSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class SyncSettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun storedConfig_isLoadedIntoUiState() = runTest {
        val configStore = FakeSyncSettingsConfigStore(
            StoredSyncConfig(
                owner = "me",
                repo = "books",
                branch = "main",
                bootstrapToken = "ghp_secret",
            )
        )
        val viewModel = SyncSettingsViewModel(
            configStore = configStore,
            syncService = FakeGitHubSyncService(),
        )

        val state = viewModel.uiState.first { it.owner.isNotEmpty() }
        assertEquals("me", state.owner)
        assertEquals("books", state.repo)
        assertEquals("ghp_secret", state.bootstrapToken)
        assertEquals("", state.syncPassword)
    }

    @Test
    fun pushNow_persistsBootstrapTokenLocallyBeforeSync() = runTest {
        val configStore = FakeSyncSettingsConfigStore()
        val syncService = FakeGitHubSyncService()
        val viewModel = SyncSettingsViewModel(
            configStore = configStore,
            syncService = syncService,
        )
        val draft = SyncSettingsUiState(
            owner = "me",
            repo = "books",
            branch = "main",
            bootstrapToken = "ghp_secret",
            syncPassword = "sync-pass",
        )

        viewModel.pushNow(draft)

        assertEquals(draft.toAuthConfig(), syncService.lastPushed)
        assertEquals(draft.toStoredConfig(), configStore.saved)
        assertEquals("ghp_secret", configStore.saved.bootstrapToken)
    }
}

private class FakeSyncSettingsConfigStore(
    initial: StoredSyncConfig = StoredSyncConfig(),
) : SyncSettingsConfigStore {
    private val state = MutableStateFlow(initial)
    var saved: StoredSyncConfig = initial

    override val syncConfig: Flow<StoredSyncConfig> = state

    override suspend fun saveConfig(config: StoredSyncConfig) {
        saved = config
        state.value = config
    }
}

private class FakeGitHubSyncService : GitHubSyncService {
    var lastPushed: GitHubAuthConfig? = null

    override suspend fun pushSnapshot(auth: GitHubAuthConfig) {
        lastPushed = auth
    }

    override suspend fun pullLatestSnapshot(auth: GitHubAuthConfig): RestoredSnapshot =
        RestoredSnapshot(
            snapshot = SyncSnapshot(
                snapshotId = "snapshot-1",
                revision = "revision-1",
                deviceId = "device-1",
                mergedAt = 1L,
                manifestJson = "{}",
            ),
            assets = emptyList(),
            preferences = com.wenwentome.reader.sync.github.PreferencesSnapshot(
                owner = "me",
                repo = "books",
                branch = "main",
                deviceId = "device-1",
            ),
        )
}

class MainDispatcherRule(
    private val dispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
