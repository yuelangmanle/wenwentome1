package com.wenwentome.reader.data.apihub.local

import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import java.security.KeyStoreException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ApiSecretLocalStoreTest {
    private val bootstrapSecretId = "github.bootstrap.token"

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

        val store =
            createSecureApiSecretLocalStore(
                context = context,
                preferencesName = preferencesName,
                knownSecretIdsProvider = { setOf("provider-legacy", bootstrapSecretId) },
            )

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

        val store =
            createSecureApiSecretLocalStore(
                context = context,
                preferencesName = preferencesName,
                knownSecretIdsProvider = { setOf("provider-legacy") },
            )

        assertEquals("sk-legacy", store.read("provider-legacy"))
        assertTrue(sameNamePreferences.all.isNotEmpty())
        assertTrue(sameNamePreferences.all.values.none { it == "sk-legacy" })
    }

    @Test
    fun apiSecretLocalStore_failedMigrationRetainsBackupAndRecoversOnNextStart() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val preferencesName = "api-secret-store-test-recoverable"
        val sameNamePreferences =
            context.getSharedPreferences(preferencesName, android.content.Context.MODE_PRIVATE).also {
                it.edit()
                    .clear()
                    .putString("provider-legacy", "sk-legacy")
                    .commit()
            }

        try {
            createSecureApiSecretLocalStore(
                context = context,
                preferencesName = preferencesName,
                knownSecretIdsProvider = { setOf("provider-legacy") },
                hooks =
                    SecretStoreTestHooks(
                        beforeImport = {
                            throw IllegalStateException("simulated import failure")
                        },
                    ),
            )
            fail("expected simulated migration failure")
        } catch (_: IllegalStateException) {
            val backupPreferences =
                context.getSharedPreferences(
                    migrationBackupPreferencesName(preferencesName),
                    android.content.Context.MODE_PRIVATE,
                )
            assertEquals("sk-legacy", backupPreferences.getString("provider-legacy", null))
            assertTrue(sameNamePreferences.all.isNotEmpty())
        }

        val recoveredStore =
            createSecureApiSecretLocalStore(
                context = context,
                preferencesName = preferencesName,
                knownSecretIdsProvider = { setOf("provider-legacy") },
            )

        assertEquals("sk-legacy", recoveredStore.read("provider-legacy"))
        val backupPreferences =
            context.getSharedPreferences(
                migrationBackupPreferencesName(preferencesName),
                android.content.Context.MODE_PRIVATE,
            )
        assertTrue(backupPreferences.all.isEmpty())
    }

    @Test
    fun apiSecretLocalStore_retriesEncryptedCreationAfterClearingLegacySameNameFile() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val preferencesName = "api-secret-store-test-production-retry"
        val sameNamePreferences =
            context.getSharedPreferences(preferencesName, android.content.Context.MODE_PRIVATE).also {
                it.edit()
                    .clear()
                    .putString("provider-legacy", "sk-legacy")
                    .commit()
            }
        var createAttempts = 0

        val store =
            createSecureApiSecretLocalStore(
                context = context,
                preferencesName = preferencesName,
                knownSecretIdsProvider = { setOf("provider-legacy") },
                hooks =
                    SecretStoreTestHooks(
                        createEncryptedPreferences = { _, _ ->
                            createAttempts += 1
                            if (createAttempts == 1) {
                                throw IllegalStateException("simulated encrypted prefs blocked by plain legacy")
                            }
                            PrefixingSharedPreferences(
                                delegate = sameNamePreferences,
                                prefix = "secure:",
                            )
                        },
                    ),
            )

        assertEquals(2, createAttempts)
        assertEquals("sk-legacy", store.read("provider-legacy"))
        assertNull(sameNamePreferences.getString("provider-legacy", null))
        assertTrue(sameNamePreferences.all.keys.any { it.startsWith("secure:") })
        val backupPreferences =
            context.getSharedPreferences(
                migrationBackupPreferencesName(preferencesName),
                android.content.Context.MODE_PRIVATE,
        )
        assertTrue(backupPreferences.all.isEmpty())
    }

    @Test
    fun apiSecretLocalStore_mergesBackupWithLivePlainLegacyBeforeRetryingEncryptedCreation() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val preferencesName = "api-secret-store-test-backup-live-merge"
        val sameNamePreferences =
            context.getSharedPreferences(preferencesName, android.content.Context.MODE_PRIVATE).also {
                it.edit()
                    .clear()
                    .putString("provider-shared", "sk-live")
                    .putString("provider-live-only", "sk-live-only")
                    .putInt("non-migration-version", 7)
                    .commit()
            }
        context.getSharedPreferences(
            migrationBackupPreferencesName(preferencesName),
            android.content.Context.MODE_PRIVATE,
        ).edit()
            .clear()
            .putString("provider-shared", "sk-backup")
            .putString("provider-backup-only", "sk-backup-only")
            .commit()

        var createAttempts = 0

        val store =
            createSecureApiSecretLocalStore(
                context = context,
                preferencesName = preferencesName,
                knownSecretIdsProvider = { setOf("provider-shared", "provider-live-only", "provider-backup-only") },
                hooks =
                    SecretStoreTestHooks(
                        createEncryptedPreferences = { _, _ ->
                            createAttempts += 1
                            if (createAttempts == 1) {
                                throw IllegalStateException("simulated encrypted prefs blocked by plain legacy")
                            }
                            PrefixingSharedPreferences(
                                delegate = sameNamePreferences,
                                prefix = "secure:",
                            )
                        },
                    ),
            )

        assertEquals(2, createAttempts)
        assertEquals("sk-live", store.read("provider-shared"))
        assertEquals("sk-live-only", store.read("provider-live-only"))
        assertEquals("sk-backup-only", store.read("provider-backup-only"))
        assertNull(sameNamePreferences.getString("provider-shared", null))
        assertNull(sameNamePreferences.getString("provider-live-only", null))
        assertEquals(7, sameNamePreferences.getInt("non-migration-version", -1))
        val backupPreferences =
            context.getSharedPreferences(
                migrationBackupPreferencesName(preferencesName),
                android.content.Context.MODE_PRIVATE,
            )
        assertTrue(backupPreferences.all.isEmpty())
    }

    @Test
    fun apiSecretLocalStore_migratesOnlyKnownSecretIdsWhenKeysetAndOtherEntriesExist() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val preferencesName = "api-secret-store-test-known-ids-with-keyset"
        val sameNamePreferences =
            context.getSharedPreferences(preferencesName, android.content.Context.MODE_PRIVATE).also {
                it.edit()
                    .clear()
                    .putString(ANDROIDX_SECURITY_KEY_KEYSET, "keyset")
                    .putString(ANDROIDX_SECURITY_VALUE_KEYSET, "value-keyset")
                    .putString("provider-known", "sk-known")
                    .putString("provider-unknown", "sk-unknown")
                    .commit()
            }

        val store =
            createSecureApiSecretLocalStore(
                context = context,
                preferencesName = preferencesName,
                knownSecretIdsProvider = { setOf("provider-known") },
                hooks =
                    SecretStoreTestHooks(
                        createEncryptedPreferences = { _, _ ->
                            PrefixingSharedPreferences(
                                delegate = sameNamePreferences,
                                prefix = "secure:",
                            )
                        },
                    ),
            )

        assertEquals("sk-known", store.read("provider-known"))
        assertNull(store.read("provider-unknown"))
        assertNull(sameNamePreferences.getString("provider-known", null))
        assertEquals("sk-unknown", sameNamePreferences.getString("provider-unknown", null))
        assertEquals("keyset", sameNamePreferences.getString(ANDROIDX_SECURITY_KEY_KEYSET, null))
        assertEquals("value-keyset", sameNamePreferences.getString(ANDROIDX_SECURITY_VALUE_KEYSET, null))
    }

    @Test
    fun apiSecretLocalStore_backupAndKeysetStillMergeKnownLivePlaintext() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val preferencesName = "api-secret-store-test-backup-keyset-known"
        val sameNamePreferences =
            context.getSharedPreferences(preferencesName, android.content.Context.MODE_PRIVATE).also {
                it.edit()
                    .clear()
                    .putString(ANDROIDX_SECURITY_KEY_KEYSET, "keyset")
                    .putString(ANDROIDX_SECURITY_VALUE_KEYSET, "value-keyset")
                    .putString("provider-shared", "sk-live")
                    .putString("provider-unknown", "sk-unknown")
                    .commit()
            }
        context.getSharedPreferences(
            migrationBackupPreferencesName(preferencesName),
            android.content.Context.MODE_PRIVATE,
        ).edit()
            .clear()
            .putString("provider-shared", "sk-backup")
            .putString("provider-backup-only", "sk-backup-only")
            .putString("provider-unknown-backup", "sk-unknown-backup")
            .commit()

        val store =
            createSecureApiSecretLocalStore(
                context = context,
                preferencesName = preferencesName,
                knownSecretIdsProvider = { setOf("provider-shared", "provider-backup-only") },
                hooks =
                    SecretStoreTestHooks(
                        createEncryptedPreferences = { _, _ ->
                            PrefixingSharedPreferences(
                                delegate = sameNamePreferences,
                                prefix = "secure:",
                            )
                        },
                    ),
            )

        assertEquals("sk-live", store.read("provider-shared"))
        assertEquals("sk-backup-only", store.read("provider-backup-only"))
        assertNull(store.read("provider-unknown-backup"))
        assertNull(sameNamePreferences.getString("provider-shared", null))
        assertEquals("sk-unknown", sameNamePreferences.getString("provider-unknown", null))
        assertEquals("keyset", sameNamePreferences.getString(ANDROIDX_SECURITY_KEY_KEYSET, null))
        assertEquals("value-keyset", sameNamePreferences.getString(ANDROIDX_SECURITY_VALUE_KEYSET, null))
        val backupPreferences =
            context.getSharedPreferences(
                migrationBackupPreferencesName(preferencesName),
                android.content.Context.MODE_PRIVATE,
            )
        assertTrue(backupPreferences.all.isEmpty())
    }

    @Test
    fun apiSecretLocalStore_ignoresUnknownBackupEntriesOutsideKnownSecretIds() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val preferencesName = "api-secret-store-test-backup-whitelist"
        val sameNamePreferences =
            context.getSharedPreferences(preferencesName, android.content.Context.MODE_PRIVATE).also {
                it.edit()
                    .clear()
                    .putString(ANDROIDX_SECURITY_KEY_KEYSET, "keyset")
                    .putString(ANDROIDX_SECURITY_VALUE_KEYSET, "value-keyset")
                    .commit()
            }
        context.getSharedPreferences(
            migrationBackupPreferencesName(preferencesName),
            android.content.Context.MODE_PRIVATE,
        ).edit()
            .clear()
            .putString("provider-known", "sk-known")
            .putString("provider-unknown", "sk-unknown")
            .commit()

        val store =
            createSecureApiSecretLocalStore(
                context = context,
                preferencesName = preferencesName,
                knownSecretIdsProvider = { setOf("provider-known") },
                hooks =
                    SecretStoreTestHooks(
                        createEncryptedPreferences = { _, _ ->
                            PrefixingSharedPreferences(
                                delegate = sameNamePreferences,
                                prefix = "secure:",
                            )
                        },
                    ),
            )

        assertEquals("sk-known", store.read("provider-known"))
        assertNull(store.read("provider-unknown"))
        val backupPreferences =
            context.getSharedPreferences(
                migrationBackupPreferencesName(preferencesName),
                android.content.Context.MODE_PRIVATE,
            )
        assertTrue(backupPreferences.all.isEmpty())
        assertEquals("keyset", sameNamePreferences.getString(ANDROIDX_SECURITY_KEY_KEYSET, null))
        assertEquals("value-keyset", sameNamePreferences.getString(ANDROIDX_SECURITY_VALUE_KEYSET, null))
    }

    @Test
    fun apiSecretLocalStore_bootstrapStoreOnlyMigratesBootstrapSecretId() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val preferencesName = "api-secret-store-test-bootstrap-whitelist"
        val sameNamePreferences =
            context.getSharedPreferences(preferencesName, android.content.Context.MODE_PRIVATE).also {
                it.edit()
                    .clear()
                    .putString(bootstrapSecretId, "ghp-bootstrap")
                    .putString("provider-unknown", "sk-unknown")
                    .commit()
            }

        val store =
            createSecureApiSecretLocalStore(
                context = context,
                preferencesName = preferencesName,
                knownSecretIdsProvider = { setOf(bootstrapSecretId) },
                hooks =
                    SecretStoreTestHooks(
                        createEncryptedPreferences = { _, _ ->
                            PrefixingSharedPreferences(
                                delegate = sameNamePreferences,
                                prefix = "secure:",
                            )
                        },
                    ),
            )

        assertEquals("ghp-bootstrap", store.read(bootstrapSecretId))
        assertNull(store.read("provider-unknown"))
        assertNull(sameNamePreferences.getString(bootstrapSecretId, null))
        assertEquals("sk-unknown", sameNamePreferences.getString("provider-unknown", null))
    }

    @Test
    fun apiSecretLocalStore_providerStoreOnlyMigratesRegisteredProviderIds() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val preferencesName = "api-secret-store-test-provider-whitelist"
        val sameNamePreferences =
            context.getSharedPreferences(preferencesName, android.content.Context.MODE_PRIVATE).also {
                it.edit()
                    .clear()
                    .putString("provider-known", "sk-known")
                    .putString("provider-unknown", "sk-unknown")
                    .commit()
            }

        val store =
            createSecureApiSecretLocalStore(
                context = context,
                preferencesName = preferencesName,
                knownSecretIdsProvider = { setOf("provider-known") },
                hooks =
                    SecretStoreTestHooks(
                        createEncryptedPreferences = { _, _ ->
                            PrefixingSharedPreferences(
                                delegate = sameNamePreferences,
                                prefix = "secure:",
                            )
                        },
                    ),
            )

        assertEquals("sk-known", store.read("provider-known"))
        assertNull(store.read("provider-unknown"))
        assertNull(sameNamePreferences.getString("provider-known", null))
        assertEquals("sk-unknown", sameNamePreferences.getString("provider-unknown", null))
    }

    @Test
    fun androidKeyStoreFallback_onlyTriggersForExplicitAndroidKeyStoreFailures() {
        assertTrue(
            shouldUseRobolectricFallback(
                KeyStoreException("AndroidKeyStore not found"),
            ),
        )
        assertFalse(
            shouldUseRobolectricFallback(
                KeyStoreException("generic keystore failure"),
            ),
        )
        assertFalse(
            shouldUseRobolectricFallback(
                NoSuchAlgorithmException("PBKDF2WithHmacSHA256 not available"),
            ),
        )
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

    private class PrefixingSharedPreferences(
        private val delegate: SharedPreferences,
        private val prefix: String,
    ) : SharedPreferences {
        override fun contains(key: String?): Boolean =
            key != null && delegate.contains(prefixed(key))

        override fun getBoolean(
            key: String?,
            defValue: Boolean,
        ): Boolean = delegate.getBoolean(requirePrefixed(key), defValue)

        override fun getFloat(
            key: String?,
            defValue: Float,
        ): Float = delegate.getFloat(requirePrefixed(key), defValue)

        override fun getInt(
            key: String?,
            defValue: Int,
        ): Int = delegate.getInt(requirePrefixed(key), defValue)

        override fun getLong(
            key: String?,
            defValue: Long,
        ): Long = delegate.getLong(requirePrefixed(key), defValue)

        override fun getString(
            key: String?,
            defValue: String?,
        ): String? = delegate.getString(requirePrefixed(key), defValue)

        override fun getStringSet(
            key: String?,
            defValues: MutableSet<String>?,
        ): MutableSet<String>? = delegate.getStringSet(requirePrefixed(key), defValues)

        override fun getAll(): MutableMap<String, *> =
            delegate.all
                .filterKeys { it.startsWith(prefix) }
                .mapKeys { (key, _) -> key.removePrefix(prefix) }
                .toMutableMap()

        override fun edit(): SharedPreferences.Editor =
            PrefixingEditor(
                preferences = delegate,
                prefix = prefix,
            )

        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
            // Test-only wrapper does not need listener bridging.
        }

        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
            // Test-only wrapper does not need listener bridging.
        }

        private fun prefixed(key: String): String = "$prefix$key"

        private fun requirePrefixed(key: String?): String = prefixed(requireNotNull(key))
    }

    private class PrefixingEditor(
        private val preferences: SharedPreferences,
        private val prefix: String,
    ) : SharedPreferences.Editor {
        private val delegate = preferences.edit()

        override fun putBoolean(
            key: String?,
            value: Boolean,
        ): SharedPreferences.Editor {
            delegate.putBoolean(requirePrefixed(key), value)
            return this
        }

        override fun putFloat(
            key: String?,
            value: Float,
        ): SharedPreferences.Editor {
            delegate.putFloat(requirePrefixed(key), value)
            return this
        }

        override fun putInt(
            key: String?,
            value: Int,
        ): SharedPreferences.Editor {
            delegate.putInt(requirePrefixed(key), value)
            return this
        }

        override fun putLong(
            key: String?,
            value: Long,
        ): SharedPreferences.Editor {
            delegate.putLong(requirePrefixed(key), value)
            return this
        }

        override fun putString(
            key: String?,
            value: String?,
        ): SharedPreferences.Editor {
            delegate.putString(requirePrefixed(key), value)
            return this
        }

        override fun putStringSet(
            key: String?,
            values: MutableSet<String>?,
        ): SharedPreferences.Editor {
            delegate.putStringSet(requirePrefixed(key), values)
            return this
        }

        override fun remove(key: String?): SharedPreferences.Editor {
            delegate.remove(requirePrefixed(key))
            return this
        }

        override fun clear(): SharedPreferences.Editor {
            preferences.all.keys
                .filter { it.startsWith(prefix) }
                .forEach(delegate::remove)
            return this
        }

        override fun commit(): Boolean = delegate.commit()

        override fun apply() {
            delegate.apply()
        }

        private fun requirePrefixed(key: String?): String = "$prefix${requireNotNull(key)}"
    }

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
