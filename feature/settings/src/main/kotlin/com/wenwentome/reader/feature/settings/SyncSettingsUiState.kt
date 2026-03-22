package com.wenwentome.reader.feature.settings

import com.wenwentome.reader.sync.github.GitHubAuthConfig

data class StoredSyncConfig(
    val owner: String = "",
    val repo: String = "",
    val branch: String = "main",
    val bootstrapToken: String = "",
)

data class SyncSettingsUiState(
    val owner: String = "",
    val repo: String = "",
    val branch: String = "main",
    val bootstrapToken: String = "",
    val syncPassword: String = "",
) {
    fun updateOwner(value: String) = copy(owner = value)
    fun updateRepo(value: String) = copy(repo = value)
    fun updateBranch(value: String) = copy(branch = value)
    fun updateBootstrapToken(value: String) = copy(bootstrapToken = value)
    fun updateSyncPassword(value: String) = copy(syncPassword = value)

    fun toAuthConfig(): GitHubAuthConfig =
        GitHubAuthConfig(
            owner = owner,
            repo = repo,
            branch = branch,
            token = bootstrapToken,
            syncPassword = syncPassword,
        )

    fun toStoredConfig(): StoredSyncConfig =
        StoredSyncConfig(
            owner = owner,
            repo = repo,
            branch = branch,
            bootstrapToken = bootstrapToken,
        )

    companion object {
        fun fromStoredConfig(config: StoredSyncConfig): SyncSettingsUiState =
            SyncSettingsUiState(
                owner = config.owner,
                repo = config.repo,
                branch = config.branch,
                bootstrapToken = config.bootstrapToken,
            )
    }
}
