package com.wenwentome.reader.core.database.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.wenwentome.reader.core.model.ReaderMode
import com.wenwentome.reader.core.model.ReaderPresentationPrefs
import com.wenwentome.reader.core.model.ReaderTheme
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class GitHubSyncConfig(
    val owner: String,
    val repo: String,
    val branch: String,
    val token: String,
)

data class PreferencesSnapshot(
    val owner: String,
    val repo: String,
    val branch: String,
    val token: String,
    val deviceId: String,
)

private val Context.dataStore by preferencesDataStore(name = "reader_prefs")

private val OWNER = stringPreferencesKey("github_owner")
private val REPO = stringPreferencesKey("github_repo")
private val BRANCH = stringPreferencesKey("github_branch")
private val TOKEN = stringPreferencesKey("github_token")
private val DEVICE_ID = stringPreferencesKey("device_id")
private val READER_MODE = stringPreferencesKey("reader_mode")
private val READER_THEME = stringPreferencesKey("reader_theme")
private val READER_FONT_SIZE_SP = intPreferencesKey("reader_font_size_sp")
private val READER_LINE_HEIGHT_MULTIPLIER = floatPreferencesKey("reader_line_height_multiplier")
private val READER_BRIGHTNESS_PERCENT = intPreferencesKey("reader_brightness_percent")

/**
 * 注意：这里不依赖 Task 7 的 `GitHubAuthConfig`，避免跨任务耦合。
 * 后续若引入统一配置类型，可在更高层做 mapping。
 */
class ReaderPreferencesStore(private val context: Context) {
    val readerMode: Flow<ReaderMode> =
        context.dataStore.data.map { prefs ->
            prefs[READER_MODE]
                ?.let { modeName ->
                    runCatching { ReaderMode.valueOf(modeName) }
                        .getOrDefault(ReaderMode.SIMULATED_PAGE_TURN)
                }
                ?: ReaderMode.SIMULATED_PAGE_TURN
        }

    val presentationPrefs: Flow<ReaderPresentationPrefs> =
        context.dataStore.data.map { prefs ->
            ReaderPresentationPrefs(
                theme =
                    prefs[READER_THEME]
                        ?.let { themeName ->
                            runCatching { ReaderTheme.valueOf(themeName) }
                                .getOrDefault(ReaderTheme.PAPER)
                        }
                        ?: ReaderTheme.PAPER,
                fontSizeSp = prefs[READER_FONT_SIZE_SP] ?: 18,
                lineHeightMultiplier = prefs[READER_LINE_HEIGHT_MULTIPLIER] ?: 1.55f,
                brightnessPercent = prefs[READER_BRIGHTNESS_PERCENT] ?: 0,
            )
        }

    suspend fun saveReaderMode(mode: ReaderMode) {
        context.dataStore.edit { prefs ->
            prefs[READER_MODE] = mode.name
        }
    }

    suspend fun savePresentationPrefs(prefs: ReaderPresentationPrefs) {
        context.dataStore.edit { editable ->
            editable[READER_THEME] = prefs.theme.name
            editable[READER_FONT_SIZE_SP] = prefs.fontSizeSp
            editable[READER_LINE_HEIGHT_MULTIPLIER] = prefs.lineHeightMultiplier
            editable[READER_BRIGHTNESS_PERCENT] = prefs.brightnessPercent
        }
    }

    val syncConfig: Flow<GitHubSyncConfig> =
        context.dataStore.data.map { prefs ->
            GitHubSyncConfig(
                owner = prefs[OWNER].orEmpty(),
                repo = prefs[REPO].orEmpty(),
                branch = prefs[BRANCH] ?: "main",
                token = prefs[TOKEN].orEmpty(),
            )
        }

    suspend fun saveGitHubConfig(owner: String, repo: String, branch: String, token: String) {
        context.dataStore.edit { prefs ->
            prefs[OWNER] = owner
            prefs[REPO] = repo
            prefs[BRANCH] = branch
            prefs[TOKEN] = token
        }
    }

    suspend fun getOrCreateDeviceId(): String {
        val existing = context.dataStore.data.first()[DEVICE_ID]
        if (existing != null) return existing
        val generated = UUID.randomUUID().toString()
        context.dataStore.edit { it[DEVICE_ID] = generated }
        return generated
    }

    suspend fun exportSnapshot(): PreferencesSnapshot {
        val config = syncConfig.first()
        return PreferencesSnapshot(
            owner = config.owner,
            repo = config.repo,
            branch = config.branch,
            token = config.token,
            deviceId = getOrCreateDeviceId(),
        )
    }

    suspend fun importSnapshot(snapshot: PreferencesSnapshot) {
        saveGitHubConfig(snapshot.owner, snapshot.repo, snapshot.branch, snapshot.token)
        context.dataStore.edit { it[DEVICE_ID] = snapshot.deviceId }
    }
}
