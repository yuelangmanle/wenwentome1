package com.wenwentome.reader.data.apihub.local

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

interface ApiSecretLocalStore {
    suspend fun save(secretId: String, plainText: String)
    suspend fun read(secretId: String): String?
    suspend fun delete(secretId: String)
}

class SharedPreferencesApiSecretLocalStore(
    private val preferences: SharedPreferences,
) : ApiSecretLocalStore {
    override suspend fun save(secretId: String, plainText: String) {
        persistSecret(preferences, secretId, plainText)
    }

    override suspend fun read(secretId: String): String? = preferences.getString(secretId, null)

    override suspend fun delete(secretId: String) {
        deleteSecret(preferences, secretId)
    }
}

class EncryptedSharedPreferencesApiSecretLocalStore(
    private val preferences: SharedPreferences,
) : ApiSecretLocalStore {
    override suspend fun save(secretId: String, plainText: String) {
        persistSecret(preferences, secretId, plainText)
    }

    override suspend fun read(secretId: String): String? = preferences.getString(secretId, null)

    override suspend fun delete(secretId: String) {
        deleteSecret(preferences, secretId)
    }

    fun importPlainSecrets(secrets: List<Pair<String, String>>) {
        secrets.forEach { (secretId, plainText) ->
            if (!preferences.contains(secretId)) {
                persistSecret(preferences, secretId, plainText)
            }
        }
    }

    fun readImmediately(secretId: String): String? = preferences.getString(secretId, null)
}

private class RobolectricSecureApiSecretLocalStore(
    private val preferences: SharedPreferences,
    packageName: String,
    preferencesName: String,
) : ApiSecretLocalStore {
    private val cipherCodec = RobolectricCipherCodec(packageName = packageName, preferencesName = preferencesName)

    override suspend fun save(secretId: String, plainText: String) {
        persistSecret(preferences, secretId, cipherCodec.encrypt(plainText))
    }

    override suspend fun read(secretId: String): String? =
        preferences.getString(secretId, null)?.let(cipherCodec::decrypt)

    override suspend fun delete(secretId: String) {
        deleteSecret(preferences, secretId)
    }

    fun importPlainSecrets(secrets: List<Pair<String, String>>) {
        secrets.forEach { (secretId, plainText) ->
            val existingValue = preferences.getString(secretId, null)
            if (existingValue == null || !cipherCodec.canDecrypt(existingValue)) {
                persistSecret(preferences, secretId, cipherCodec.encrypt(plainText))
            }
        }
    }

    fun readImmediately(secretId: String): String? =
        preferences.getString(secretId, null)?.let(cipherCodec::decrypt)
}

internal data class SecretStoreTestHooks(
    val beforeImport: () -> Unit = {},
    val createEncryptedPreferences: ((Context, String) -> SharedPreferences)? = null,
)

private data class PendingPlainSecretMigration(
    val secrets: List<Pair<String, String>>,
    val liveLegacySecretIds: List<String>,
    val backupPreferences: SharedPreferences,
)

fun createSecureApiSecretLocalStore(
    context: Context,
    preferencesName: String,
): ApiSecretLocalStore =
    createSecureApiSecretLocalStore(
        context = context,
        preferencesName = preferencesName,
        hooks = SecretStoreTestHooks(),
    )

internal fun createSecureApiSecretLocalStore(
    context: Context,
    preferencesName: String,
    hooks: SecretStoreTestHooks = SecretStoreTestHooks(),
): ApiSecretLocalStore {
    val sameNamePreferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    val pendingMigration =
        preparePendingMigration(
            context = context,
            sameNamePreferences = sameNamePreferences,
            preferencesName = preferencesName,
        )
    val store =
        createSecureStore(
            context = context,
            preferencesName = preferencesName,
            sameNamePreferences = sameNamePreferences,
            pendingMigration = pendingMigration,
            hooks = hooks,
        )
    return store.also {
        completePendingMigration(
            store = it,
            sameNamePreferences = sameNamePreferences,
            pendingMigration = pendingMigration,
            hooks = hooks,
        )
    } as ApiSecretLocalStore
}

internal fun createEncryptedPreferences(
    context: Context,
    preferencesName: String,
): SharedPreferences {
    val masterKey =
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    return EncryptedSharedPreferences.create(
        context,
        preferencesName,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )
}

private fun persistSecret(
    preferences: SharedPreferences,
    secretId: String,
    plainText: String,
) {
    check(preferences.edit().putString(secretId, plainText).commit()) {
        "Failed to persist api secret for key=$secretId"
    }
}

private fun deleteSecret(
    preferences: SharedPreferences,
    secretId: String,
) {
    check(preferences.edit().remove(secretId).commit()) {
        "Failed to delete api secret for key=$secretId"
    }
}

private fun ByteArray.toBase64(): String = Base64.getEncoder().encodeToString(this)

private fun isRobolectricRuntime(): Boolean =
    Build.FINGERPRINT.equals("robolectric", ignoreCase = true)

internal fun shouldUseRobolectricFallback(error: Throwable): Boolean =
    isRobolectricRuntime() && error.causalChain().any(::isAndroidKeyStoreUnavailable)

private fun Throwable.causalChain(): Sequence<Throwable> =
    generateSequence(this) { current -> current.cause }

internal fun loadPlainLegacySecretsOrNull(
    preferences: SharedPreferences,
    packageName: String,
    preferencesName: String,
): List<Pair<String, String>>? {
    val stringEntries =
        preferences.all.mapNotNull { (secretId, value) ->
            (value as? String)?.let { plainText -> secretId to plainText }
        }
    if (stringEntries.isEmpty()) return null
    if (containsEncryptedSharedPreferencesKeysetEntries(preferences)) return null

    val fallbackCodec = RobolectricCipherCodec(packageName = packageName, preferencesName = preferencesName)
    if (stringEntries.all { (_, storedValue) -> fallbackCodec.canDecrypt(storedValue) }) {
        return null
    }

    return stringEntries
}

private fun containsEncryptedSharedPreferencesKeysetEntries(preferences: SharedPreferences): Boolean =
    preferences.contains(ANDROIDX_SECURITY_KEY_KEYSET) ||
        preferences.contains(ANDROIDX_SECURITY_VALUE_KEYSET)

private fun preparePendingMigration(
    context: Context,
    sameNamePreferences: SharedPreferences,
    preferencesName: String,
): PendingPlainSecretMigration? {
    val backupPreferences =
        context.getSharedPreferences(
            migrationBackupPreferencesName(preferencesName),
            Context.MODE_PRIVATE,
        )
    val backupSecrets = loadStoredSecretsOrNull(backupPreferences).orEmpty()
    val livePlainLegacySecrets =
        loadPlainLegacySecretsOrNull(
            preferences = sameNamePreferences,
            packageName = context.packageName,
            preferencesName = preferencesName,
        ).orEmpty()
    if (backupSecrets.isEmpty() && livePlainLegacySecrets.isEmpty()) {
        return null
    }

    val mergedSecrets = mergePendingMigrationSecrets(backupSecrets, livePlainLegacySecrets)
    persistBackupSecrets(backupPreferences, mergedSecrets)
    return PendingPlainSecretMigration(
        secrets = mergedSecrets,
        liveLegacySecretIds = livePlainLegacySecrets.map { (secretId, _) -> secretId },
        backupPreferences = backupPreferences,
    )
}

private fun createSecureStore(
    context: Context,
    preferencesName: String,
    sameNamePreferences: SharedPreferences,
    pendingMigration: PendingPlainSecretMigration?,
    hooks: SecretStoreTestHooks,
): Any {
    val encryptedPreferencesFactory = hooks.createEncryptedPreferences ?: ::createEncryptedPreferences
    return runCatching<Any> {
        EncryptedSharedPreferencesApiSecretLocalStore(
            preferences = encryptedPreferencesFactory(context, preferencesName),
        )
    }.getOrElse { error ->
        if (shouldUseRobolectricFallback(error)) {
            return@getOrElse RobolectricSecureApiSecretLocalStore(
                preferences = sameNamePreferences,
                packageName = context.packageName,
                preferencesName = preferencesName,
            )
        }
        if (pendingMigration == null) {
            throw error
        }

        removePlainLegacyEntries(
            sameNamePreferences,
            pendingMigration.liveLegacySecretIds,
            "Failed to clear legacy plaintext secrets before retrying secure migration",
        )

        runCatching<Any> {
            EncryptedSharedPreferencesApiSecretLocalStore(
                preferences = encryptedPreferencesFactory(context, preferencesName),
            )
        }.getOrElse { retryError ->
            if (shouldUseRobolectricFallback(retryError)) {
                RobolectricSecureApiSecretLocalStore(
                    preferences = sameNamePreferences,
                    packageName = context.packageName,
                    preferencesName = preferencesName,
                )
            } else {
                throw retryError
            }
        }
    }
}

private fun completePendingMigration(
    store: Any,
    sameNamePreferences: SharedPreferences,
    pendingMigration: PendingPlainSecretMigration?,
    hooks: SecretStoreTestHooks,
) {
    if (pendingMigration == null) return
    hooks.beforeImport()
    when (store) {
        is EncryptedSharedPreferencesApiSecretLocalStore -> {
            store.importPlainSecrets(pendingMigration.secrets)
            verifyImportedSecrets(pendingMigration.secrets) { secretId ->
                store.readImmediately(secretId)
            }
            removePlainLegacyEntries(
                preferences = sameNamePreferences,
                secretIds = pendingMigration.liveLegacySecretIds,
                failureMessage = "Failed to clear legacy plaintext secrets after secure migration",
            )
        }

        is RobolectricSecureApiSecretLocalStore -> {
            store.importPlainSecrets(pendingMigration.secrets)
            verifyImportedSecrets(pendingMigration.secrets) { secretId ->
                store.readImmediately(secretId)
            }
        }
    }
    clearPreferences(
        pendingMigration.backupPreferences,
        "Failed to clear migration backup after secure migration",
    )
}

private fun verifyImportedSecrets(
    secrets: List<Pair<String, String>>,
    readSecret: (String) -> String?,
) {
    secrets.forEach { (secretId, plainText) ->
        check(readSecret(secretId) == plainText) {
            "Failed to verify migrated api secret for key=$secretId"
        }
    }
}

private fun removePlainLegacyEntries(
    preferences: SharedPreferences,
    secretIds: List<String>,
    failureMessage: String,
) {
    if (secretIds.isEmpty()) return
    val editor = preferences.edit()
    secretIds.distinct().forEach(editor::remove)
    check(editor.commit()) {
        failureMessage
    }
}

private fun mergePendingMigrationSecrets(
    backupSecrets: List<Pair<String, String>>,
    livePlainLegacySecrets: List<Pair<String, String>>,
): List<Pair<String, String>> {
    val mergedSecrets = LinkedHashMap<String, String>()
    backupSecrets.forEach { (secretId, plainText) ->
        mergedSecrets[secretId] = plainText
    }
    livePlainLegacySecrets.forEach { (secretId, plainText) ->
        mergedSecrets[secretId] = plainText
    }
    return mergedSecrets.entries.map { (secretId, plainText) -> secretId to plainText }
}

private fun persistBackupSecrets(
    backupPreferences: SharedPreferences,
    secrets: List<Pair<String, String>>,
) {
    val editor = backupPreferences.edit().clear()
    secrets.forEach { (secretId, plainText) ->
        editor.putString(secretId, plainText)
    }
    check(editor.commit()) {
        "Failed to persist api secret migration backup"
    }
}

private fun loadStoredSecretsOrNull(preferences: SharedPreferences): List<Pair<String, String>>? =
    preferences.all.mapNotNull { (secretId, value) ->
        (value as? String)?.let { plainText -> secretId to plainText }
    }.ifEmpty { null }

private fun clearPreferences(
    preferences: SharedPreferences,
    failureMessage: String,
) {
    check(preferences.edit().clear().commit()) {
        failureMessage
    }
}

private fun isAndroidKeyStoreUnavailable(cause: Throwable): Boolean {
    val message = cause.message.orEmpty()
    val hasExplicitAndroidKeyStoreClue =
        cause::class.java.name.contains("AndroidKeyStore", ignoreCase = true) ||
            message.contains("AndroidKeyStore", ignoreCase = true)
    if (!hasExplicitAndroidKeyStoreClue) return false

    val normalizedMessage = message.lowercase()
    val looksUnsupported =
        normalizedMessage.contains("not found") ||
            normalizedMessage.contains("not available") ||
            normalizedMessage.contains("unsupported") ||
            normalizedMessage.contains("unavailable") ||
            normalizedMessage.contains("cannot find") ||
            normalizedMessage.contains("failed to load")
    if (!looksUnsupported) return false

    return cause is KeyStoreException || cause is NoSuchAlgorithmException
}

private class RobolectricCipherCodec(
    packageName: String,
    preferencesName: String,
) {
    private val secretKey =
        SecretKeySpec(
            MessageDigest.getInstance("SHA-256")
                .digest("$packageName:$preferencesName".encodeToByteArray()),
            "AES",
        )

    fun encrypt(plainText: String): String {
        val iv = ByteArray(12)
        SecureRandom().nextBytes(iv)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
        val cipherBytes = cipher.doFinal(plainText.encodeToByteArray())
        return "${iv.toBase64()}:${cipherBytes.toBase64()}"
    }

    fun decrypt(encoded: String): String {
        val (ivBase64, cipherBase64) = encoded.split(':', limit = 2)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            secretKey,
            GCMParameterSpec(128, Base64.getDecoder().decode(ivBase64)),
        )
        return cipher.doFinal(Base64.getDecoder().decode(cipherBase64)).decodeToString()
    }

    fun canDecrypt(encoded: String): Boolean =
        runCatching {
            decrypt(encoded)
        }.isSuccess
}

internal fun migrationBackupPreferencesName(preferencesName: String): String =
    "$preferencesName.__migration_backup__"

internal const val ANDROIDX_SECURITY_KEY_KEYSET = "__androidx_security_crypto_encrypted_prefs_key_keyset__"
internal const val ANDROIDX_SECURITY_VALUE_KEYSET = "__androidx_security_crypto_encrypted_prefs_value_keyset__"
