package com.wenwentome.reader.core.database.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.wenwentome.reader.core.model.BrowserFindPreferences
import com.wenwentome.reader.core.model.BrowserMode
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
    val bootstrapToken: String = "",
)

data class PreferencesSnapshot(
    val owner: String,
    val repo: String,
    val branch: String,
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
private val READER_FONT_SIZE_SP = floatPreferencesKey("reader_font_size_sp_v2")
private val READER_FONT_SIZE_SP_LEGACY = intPreferencesKey("reader_font_size_sp")
private val READER_AUTO_FIT_FONT_SIZE = booleanPreferencesKey("reader_auto_fit_font_size")
private val READER_LINE_HEIGHT_MULTIPLIER = floatPreferencesKey("reader_line_height_multiplier")
private val READER_LETTER_SPACING_EM = floatPreferencesKey("reader_letter_spacing_em")
private val READER_PARAGRAPH_SPACING_EM = floatPreferencesKey("reader_paragraph_spacing_em")
private val READER_SIDE_PADDING_DP = intPreferencesKey("reader_side_padding_dp")
private val READER_BRIGHTNESS_PERCENT = intPreferencesKey("reader_brightness_percent")
private val READER_FONT_FAMILY_KEY = stringPreferencesKey("reader_font_family_key")
private val READER_BACKGROUND_PALETTE_KEY = stringPreferencesKey("reader_background_palette_key")
private val BROWSER_SEARCH_ENGINE_ID = stringPreferencesKey("browser_search_engine_id")
private val BROWSER_CUSTOM_SEARCH_ENGINE_NAME = stringPreferencesKey("browser_custom_search_engine_name")
private val BROWSER_CUSTOM_SEARCH_URL_TEMPLATE = stringPreferencesKey("browser_custom_search_url_template")
private val BROWSER_MODE = stringPreferencesKey("browser_mode")
private val BROWSER_AUTO_OPTIMIZE_READING = booleanPreferencesKey("browser_auto_optimize_reading")
private val BROWSER_SHOW_MANUAL_OPTIMIZE_FLOATING_BUTTON =
    booleanPreferencesKey("browser_show_manual_optimize_floating_button")

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
            val resolvedFontSize =
                prefs[READER_FONT_SIZE_SP]
                    ?: prefs[READER_FONT_SIZE_SP_LEGACY]?.toFloat()
                    ?: 18f
            ReaderPresentationPrefs(
                theme =
                    prefs[READER_THEME]
                        ?.let { themeName ->
                            runCatching { ReaderTheme.valueOf(themeName) }
                                .getOrDefault(ReaderTheme.PAPER)
                        }
                        ?: ReaderTheme.PAPER,
                fontSizeSp = resolvedFontSize,
                autoFitFontSize = prefs[READER_AUTO_FIT_FONT_SIZE] ?: true,
                lineHeightMultiplier = prefs[READER_LINE_HEIGHT_MULTIPLIER] ?: 1.55f,
                letterSpacingEm = prefs[READER_LETTER_SPACING_EM] ?: 0f,
                paragraphSpacingEm = prefs[READER_PARAGRAPH_SPACING_EM] ?: 0.35f,
                sidePaddingDp = prefs[READER_SIDE_PADDING_DP] ?: 18,
                brightnessPercent = prefs[READER_BRIGHTNESS_PERCENT] ?: 0,
                fontFamilyKey = prefs[READER_FONT_FAMILY_KEY] ?: "system-serif",
                backgroundPaletteKey = prefs[READER_BACKGROUND_PALETTE_KEY] ?: "paper",
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
            editable[READER_AUTO_FIT_FONT_SIZE] = prefs.autoFitFontSize
            editable[READER_LINE_HEIGHT_MULTIPLIER] = prefs.lineHeightMultiplier
            editable[READER_LETTER_SPACING_EM] = prefs.letterSpacingEm
            editable[READER_PARAGRAPH_SPACING_EM] = prefs.paragraphSpacingEm
            editable[READER_SIDE_PADDING_DP] = prefs.sidePaddingDp
            editable[READER_BRIGHTNESS_PERCENT] = prefs.brightnessPercent
            editable[READER_FONT_FAMILY_KEY] = prefs.fontFamilyKey
            editable[READER_BACKGROUND_PALETTE_KEY] = prefs.backgroundPaletteKey
        }
    }

    val browserFindPrefs: Flow<BrowserFindPreferences> =
        context.dataStore.data.map { prefs ->
            BrowserFindPreferences(
                defaultSearchEngineId = prefs[BROWSER_SEARCH_ENGINE_ID] ?: "bing",
                customSearchEngineName = prefs[BROWSER_CUSTOM_SEARCH_ENGINE_NAME].orEmpty(),
                customSearchUrlTemplate = prefs[BROWSER_CUSTOM_SEARCH_URL_TEMPLATE].orEmpty(),
                browserMode =
                    prefs[BROWSER_MODE]
                        ?.let { modeName ->
                            runCatching { BrowserMode.valueOf(modeName) }
                                .getOrDefault(BrowserMode.READER)
                        }
                        ?: BrowserMode.READER,
                autoOptimizeReading = prefs[BROWSER_AUTO_OPTIMIZE_READING] ?: true,
                showManualOptimizeFloatingButton =
                    prefs[BROWSER_SHOW_MANUAL_OPTIMIZE_FLOATING_BUTTON] ?: true,
            )
        }

    suspend fun saveBrowserFindPrefs(prefs: BrowserFindPreferences) {
        context.dataStore.edit { editable ->
            editable[BROWSER_SEARCH_ENGINE_ID] = prefs.defaultSearchEngineId
            editable[BROWSER_CUSTOM_SEARCH_ENGINE_NAME] = prefs.customSearchEngineName
            editable[BROWSER_CUSTOM_SEARCH_URL_TEMPLATE] = prefs.customSearchUrlTemplate
            editable[BROWSER_MODE] = prefs.browserMode.name
            editable[BROWSER_AUTO_OPTIMIZE_READING] = prefs.autoOptimizeReading
            editable[BROWSER_SHOW_MANUAL_OPTIMIZE_FLOATING_BUTTON] =
                prefs.showManualOptimizeFloatingButton
        }
    }

    val syncConfig: Flow<GitHubSyncConfig> =
        context.dataStore.data.map { prefs ->
            GitHubSyncConfig(
                owner = prefs[OWNER].orEmpty(),
                repo = prefs[REPO].orEmpty(),
                branch = prefs[BRANCH] ?: "main",
                bootstrapToken = prefs[TOKEN].orEmpty(),
            )
        }

    suspend fun saveGitHubConfig(owner: String, repo: String, branch: String) {
        context.dataStore.edit { prefs ->
            prefs[OWNER] = owner
            prefs[REPO] = repo
            prefs[BRANCH] = branch
            prefs.remove(TOKEN)
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
            deviceId = getOrCreateDeviceId(),
        )
    }

    suspend fun importSnapshot(snapshot: PreferencesSnapshot) {
        saveGitHubConfig(snapshot.owner, snapshot.repo, snapshot.branch)
        context.dataStore.edit { it[DEVICE_ID] = snapshot.deviceId }
    }
}
