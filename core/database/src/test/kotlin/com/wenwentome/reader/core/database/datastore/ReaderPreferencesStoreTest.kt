package com.wenwentome.reader.core.database.datastore

import android.content.Context
import android.content.ContextWrapper
import androidx.test.core.app.ApplicationProvider
import com.wenwentome.reader.core.model.ReaderMode
import com.wenwentome.reader.core.model.ReaderPresentationPrefs
import com.wenwentome.reader.core.model.ReaderTheme
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
                fontSizeSp = 20,
                lineHeightMultiplier = 1.7f,
                brightnessPercent = 72,
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
            token = "token-1",
        )

        val generatedDeviceId = store.getOrCreateDeviceId()
        val exported = store.exportSnapshot()

        assertEquals("yuelangmanle", exported.owner)
        assertEquals("wenwentome1", exported.repo)
        assertEquals("feat/android-reader-mvp", exported.branch)
        assertEquals("token-1", exported.token)
        assertEquals(generatedDeviceId, exported.deviceId)

        val imported =
            PreferencesSnapshot(
                owner = "moonreader",
                repo = "mirror",
                branch = "main",
                token = "token-2",
                deviceId = "device-fixed",
            )
        store.importSnapshot(imported)

        val restored = store.exportSnapshot()
        assertEquals(imported, restored)
        assertEquals("device-fixed", store.getOrCreateDeviceId())
        assertNotEquals(generatedDeviceId, restored.deviceId)
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
