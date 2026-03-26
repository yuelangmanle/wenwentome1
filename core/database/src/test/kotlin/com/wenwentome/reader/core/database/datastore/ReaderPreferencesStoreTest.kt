package com.wenwentome.reader.core.database.datastore

import android.content.Context
import android.content.ContextWrapper
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.test.core.app.ApplicationProvider
import com.wenwentome.reader.core.model.ReaderMode
import com.wenwentome.reader.core.model.ReaderPresentationPrefs
import com.wenwentome.reader.core.model.ReaderTheme
import com.wenwentome.reader.core.model.BrowserFindPreferences
import com.wenwentome.reader.core.model.BrowserMode
import com.wenwentome.reader.core.model.BrowserSearchEnginePreset
import java.io.File
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ReaderPreferencesStoreTest {
    @Test
    fun readerPreferences_roundTripModeAndPresentationPrefs() = runTest {
        val context = isolatedContext()
        val store = ReaderPreferencesStore(context)
        val expectedPresentationPrefs =
            ReaderPresentationPrefs(
                theme = ReaderTheme.SEPIA,
                fontSizeSp = 20f,
                autoFitFontSize = false,
                lineHeightMultiplier = 1.7f,
                letterSpacingEm = 0.02f,
                paragraphSpacingEm = 0.5f,
                sidePaddingDp = 22,
                brightnessPercent = 72,
                fontFamilyKey = "serif-compact",
                backgroundPaletteKey = "night-paper",
            )

        store.saveReaderMode(ReaderMode.HORIZONTAL_PAGING)
        store.savePresentationPrefs(expectedPresentationPrefs)

        assertEquals(ReaderMode.HORIZONTAL_PAGING, store.readerMode.first())
        assertEquals(expectedPresentationPrefs, store.presentationPrefs.first())
    }

    @Test
    fun exportAndImportSnapshot_roundTripsGitHubConfigAndDeviceId() = runTest {
        val context = isolatedContext()
        val store = ReaderPreferencesStore(context)

        store.saveGitHubConfig(
            owner = "yuelangmanle",
            repo = "wenwentome1",
            branch = "feat/android-reader-mvp",
        )

        val generatedDeviceId = store.getOrCreateDeviceId()
        val exported = store.exportSnapshot()

        assertEquals("yuelangmanle", exported.owner)
        assertEquals("wenwentome1", exported.repo)
        assertEquals("feat/android-reader-mvp", exported.branch)
        assertEquals(generatedDeviceId, exported.deviceId)

        val imported =
            PreferencesSnapshot(
                owner = "moonreader",
                repo = "mirror",
                branch = "main",
                deviceId = "device-fixed",
            )
        store.importSnapshot(imported)

        val restored = store.exportSnapshot()
        assertEquals(imported, restored)
        assertEquals("device-fixed", store.getOrCreateDeviceId())
        assertNotEquals(generatedDeviceId, restored.deviceId)
    }

    @Test
    fun presentationPrefs_readsLegacyIntegerFontSizeWhenV2ValueAbsent() = runTest {
        val context = isolatedContext()
        context.testDataStore.edit { prefs ->
            prefs[intPreferencesKey("reader_font_size_sp")] = 21
        }

        val presentation = ReaderPreferencesStore(context).presentationPrefs.first()

        assertEquals(21f, presentation.fontSizeSp, 0.0001f)
        assertEquals(true, presentation.autoFitFontSize)
    }

    @Test
    fun browserFindPrefs_roundTripSearchEngineAndModeSettings() = runTest {
        val context = isolatedContext()
        val store = ReaderPreferencesStore(context)
        val expected =
            BrowserFindPreferences(
                defaultSearchEngineId = BrowserSearchEnginePreset.SOGOU.id,
                customSearchEngineName = "站内搜",
                customSearchUrlTemplate = "https://example.com/search?q={query}",
                browserMode = BrowserMode.IMMERSIVE,
                autoOptimizeReading = false,
                showManualOptimizeFloatingButton = true,
            )

        store.saveBrowserFindPrefs(expected)

        assertEquals(expected, store.browserFindPrefs.first())
    }

    private fun isolatedContext(): Context {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val root =
            File(base.cacheDir, "reader-preferences-test-${UUID.randomUUID()}").apply {
                mkdirs()
            }

        return object : ContextWrapper(base) {
            override fun getApplicationContext(): Context = this

            override fun getFilesDir(): File = root.resolve("files").apply { mkdirs() }

            override fun getCacheDir(): File = root.resolve("cache").apply { mkdirs() }
        }
    }
}

private val Context.testDataStore by preferencesDataStore(name = "reader_prefs")
