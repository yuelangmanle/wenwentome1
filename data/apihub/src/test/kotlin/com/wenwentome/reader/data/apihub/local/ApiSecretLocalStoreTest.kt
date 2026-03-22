package com.wenwentome.reader.data.apihub.local

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ApiSecretLocalStoreTest {
    @Test
    fun apiSecretLocalStore_readsAndWritesBootstrapSecret() = runTest {
        val store = createStore("bootstrap")

        store.save("provider-1", "sk-test")

        assertEquals("sk-test", store.read("provider-1"))
    }

    @Test
    fun apiSecretLocalStore_deleteRemovesSavedSecret() = runTest {
        val store = createStore("delete")
        store.save("provider-2", "sk-delete")

        store.delete("provider-2")

        assertNull(store.read("provider-2"))
    }

    private fun createStore(name: String): ApiSecretLocalStore =
        SharedPreferencesApiSecretLocalStore(
            preferences =
                ApplicationProvider.getApplicationContext<android.content.Context>()
                    .getSharedPreferences("api-secret-store-test-$name", android.content.Context.MODE_PRIVATE),
        )
}
