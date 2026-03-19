package com.wenwentome.reader.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wenwentome.reader.sync.github.GitHubSyncService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

interface SyncSettingsConfigStore {
    val syncConfig: Flow<StoredSyncConfig>
    suspend fun saveConfig(config: StoredSyncConfig)
}

class SyncSettingsViewModel(
    private val configStore: SyncSettingsConfigStore,
    private val syncService: GitHubSyncService,
) : ViewModel() {
    private val draft = MutableStateFlow(SyncSettingsUiState())

    init {
        viewModelScope.launch {
            configStore.syncConfig.collect { config ->
                draft.value = SyncSettingsUiState.fromStoredConfig(config)
            }
        }
    }

    val uiState: StateFlow<SyncSettingsUiState> = draft.asStateFlow()

    fun setDraft(state: SyncSettingsUiState) {
        draft.value = state
    }

    fun saveConfig(state: SyncSettingsUiState) {
        viewModelScope.launch {
            configStore.saveConfig(state.toStoredConfig())
        }
    }

    fun pushNow(state: SyncSettingsUiState) {
        viewModelScope.launch {
            configStore.saveConfig(state.toStoredConfig())
            syncService.pushSnapshot(state.toAuthConfig())
        }
    }

    fun pullNow(state: SyncSettingsUiState) {
        viewModelScope.launch {
            configStore.saveConfig(state.toStoredConfig())
            syncService.pullLatestSnapshot(state.toAuthConfig())
        }
    }
}
