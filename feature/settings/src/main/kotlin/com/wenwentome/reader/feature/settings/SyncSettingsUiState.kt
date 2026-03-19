package com.wenwentome.reader.feature.settings

import com.wenwentome.reader.sync.github.GitHubAuthConfig

data class StoredSyncConfig(
    val owner: String = "",
    val repo: String = "",
    val branch: String = "main",
    val token: String = "",
)

data class SyncSettingsUiState(
    val owner: String = "",
    val repo: String = "",
    val branch: String = "main",
    val token: String = "",
) {
    fun updateOwner(value: String) = copy(owner = value)
    fun updateRepo(value: String) = copy(repo = value)
    fun updateBranch(value: String) = copy(branch = value)
    fun updateToken(value: String) = copy(token = value)

    fun toAuthConfig(): GitHubAuthConfig =
        GitHubAuthConfig(
            owner = owner,
            repo = repo,
            branch = branch,
            token = token,
        )

    fun toStoredConfig(): StoredSyncConfig =
        StoredSyncConfig(
            owner = owner,
            repo = repo,
            branch = branch,
            token = token,
        )

    companion object {
        fun fromStoredConfig(config: StoredSyncConfig): SyncSettingsUiState =
            SyncSettingsUiState(
                owner = config.owner,
                repo = config.repo,
                branch = config.branch,
                token = config.token,
            )
    }
}
