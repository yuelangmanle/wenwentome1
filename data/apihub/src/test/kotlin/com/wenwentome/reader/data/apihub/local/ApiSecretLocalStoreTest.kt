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

    private fun createStore(name: String): StoreFixture {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val preferencesName = "api-secret-store-test-$name"
        val preferences =
            context.getSharedPreferences(preferencesName, android.content.Context.MODE_PRIVATE)
                .also { it.edit().clear().commit() }
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
