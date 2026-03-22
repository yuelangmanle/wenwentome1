package com.wenwentome.reader.data.apihub.local

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ApiSecretLocalStoreTest {
    @Test
    fun apiSecretLocalStore_readsAndWritesBootstrapSecret() = runTest {
        val store = createStore("bootstrap").store

        store.save("provider-1", "sk-test")

        assertEquals("sk-test", store.read("provider-1"))
    }

    @Test
    fun apiSecretLocalStore_deleteRemovesSavedSecret() = runTest {
        val store = createStore("delete").store
        store.save("provider-2", "sk-delete")

        store.delete("provider-2")

        assertNull(store.read("provider-2"))
    }

    @Test
    fun apiSecretLocalStore_doesNotPersistPlaintextIntoBackingPreferences() = runTest {
        val fixture = createStore("encrypted")

        fixture.store.save("provider-3", "sk-plain-text")

        assertTrue(fixture.preferences.all.values.none { it == "sk-plain-text" })
        assertEquals("sk-plain-text", fixture.store.read("provider-3"))
    }

    @Test
    fun apiSecretLocalStore_migratesLegacyPlaintextSecretsIntoSecureBacking() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val preferencesName = "api-secret-store-test-migrate"
        val legacyPreferences =
            context.getSharedPreferences(preferencesName, android.content.Context.MODE_PRIVATE)
                .also {
                    it.edit()
                        .clear()
                        .putString("provider-legacy", "sk-legacy")
                        .putString("github.bootstrap.token", "ghp-legacy")
                        .commit()
                }
        val securePreferences =
            context.getSharedPreferences("$preferencesName.secure", android.content.Context.MODE_PRIVATE)
                .also { it.edit().clear().commit() }

        val store = createSecureApiSecretLocalStore(context, preferencesName)

        assertEquals("sk-legacy", store.read("provider-legacy"))
        assertEquals("ghp-legacy", store.read("github.bootstrap.token"))
        assertTrue(legacyPreferences.all.isEmpty())

        store.save("provider-legacy", "sk-updated")

        assertTrue(securePreferences.all.isNotEmpty())
        assertTrue(securePreferences.all.values.none { it == "sk-legacy" || it == "sk-updated" || it == "ghp-legacy" })
        assertEquals("sk-updated", store.read("provider-legacy"))
    }

    private fun createStore(name: String): StoreFixture {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val preferencesName = "api-secret-store-test-$name"
        val preferences =
            context.getSharedPreferences(preferencesName, android.content.Context.MODE_PRIVATE)
                .also { it.edit().clear().commit() }
        context.getSharedPreferences("$preferencesName.secure", android.content.Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        return StoreFixture(
            store = createSecureApiSecretLocalStore(context, preferencesName),
            preferences = preferences,
        )
    }

    private data class StoreFixture(
        val store: ApiSecretLocalStore,
        val preferences: android.content.SharedPreferences,
    )
}
