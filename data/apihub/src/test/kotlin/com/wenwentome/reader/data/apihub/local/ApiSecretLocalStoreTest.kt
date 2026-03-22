package com.wenwentome.reader.data.apihub.local

import androidx.test.core.app.ApplicationProvider
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
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
    fun apiSecretLocalStore_migratesSameNamePlainLegacySecretsIntoLatestSecureBacking() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val preferencesName = "api-secret-store-test-migrate"
        val sameNamePreferences =
            context.getSharedPreferences(preferencesName, android.content.Context.MODE_PRIVATE).also {
                it.edit()
                    .clear()
                    .putString("provider-legacy", "sk-legacy")
                    .putString("github.bootstrap.token", "ghp-legacy")
                    .commit()
            }

        val store = createSecureApiSecretLocalStore(context, preferencesName)

        assertEquals("sk-legacy", store.read("provider-legacy"))
        assertEquals("ghp-legacy", store.read("github.bootstrap.token"))
        assertTrue(sameNamePreferences.all.isNotEmpty())
        assertTrue(sameNamePreferences.all.values.none { it == "sk-legacy" || it == "ghp-legacy" })

        store.save("provider-legacy", "sk-updated")

        assertTrue(sameNamePreferences.all.isNotEmpty())
        assertTrue(sameNamePreferences.all.values.none { it == "sk-legacy" || it == "sk-updated" || it == "ghp-legacy" })
        assertEquals("sk-updated", store.read("provider-legacy"))
    }

    @Test
    fun apiSecretLocalStore_keepsSameNameSecureBackingFromPreviousVersionReadable() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val preferencesName = "api-secret-store-test-upgrade-secure"
        val sameNamePreferences =
            context.getSharedPreferences(preferencesName, android.content.Context.MODE_PRIVATE).also {
                it.edit()
                    .clear()
                    .putString("provider-legacy", encryptForPreviousRobolectricVersion(context, preferencesName, "sk-legacy"))
                    .commit()
            }

        val store = createSecureApiSecretLocalStore(context, preferencesName)

        assertEquals("sk-legacy", store.read("provider-legacy"))
        assertTrue(sameNamePreferences.all.isNotEmpty())
        assertTrue(sameNamePreferences.all.values.none { it == "sk-legacy" })
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

    private fun encryptForPreviousRobolectricVersion(
        context: android.content.Context,
        preferencesName: String,
        plainText: String,
    ): String {
        val secretKey =
            SecretKeySpec(
                MessageDigest.getInstance("SHA-256")
                    .digest("${context.packageName}:$preferencesName".encodeToByteArray()),
                "AES",
            )
        val iv = ByteArray(12)
        SecureRandom().nextBytes(iv)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
        val cipherBytes = cipher.doFinal(plainText.encodeToByteArray())
        return "${Base64.getEncoder().encodeToString(iv)}:${Base64.getEncoder().encodeToString(cipherBytes)}"
    }
}
