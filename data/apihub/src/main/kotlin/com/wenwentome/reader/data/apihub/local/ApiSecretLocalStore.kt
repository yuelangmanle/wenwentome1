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
            if (!preferences.contains(secretId)) {
                persistSecret(preferences, secretId, cipherCodec.encrypt(plainText))
            }
        }
    }
}

fun createSecureApiSecretLocalStore(
    context: Context,
    preferencesName: String,
): ApiSecretLocalStore {
    val sameNamePreferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    val plainLegacySecrets =
        loadPlainLegacySecretsOrNull(
            preferences = sameNamePreferences,
            packageName = context.packageName,
            preferencesName = preferencesName,
        )
    if (plainLegacySecrets != null) {
        clearPreferences(sameNamePreferences, "Failed to clear legacy plaintext secrets before secure migration")
    }
    return runCatching {
        EncryptedSharedPreferencesApiSecretLocalStore(
            preferences = createEncryptedPreferences(context, preferencesName),
        ).also { store ->
            store.importPlainSecrets(plainLegacySecrets.orEmpty())
        }
    }.getOrElse { error ->
        if (shouldUseRobolectricFallback(error)) {
            RobolectricSecureApiSecretLocalStore(
                preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE),
                packageName = context.packageName,
                preferencesName = preferencesName,
            ).also { store ->
                store.importPlainSecrets(plainLegacySecrets.orEmpty())
            }
        } else {
            throw error
        }
    }
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

private fun shouldUseRobolectricFallback(error: Throwable): Boolean =
    isRobolectricRuntime() && error.causalChain().any(::isAndroidKeyStoreUnavailable)

private fun Throwable.causalChain(): Sequence<Throwable> =
    generateSequence(this) { current -> current.cause }

private fun loadPlainLegacySecretsOrNull(
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

private fun clearPreferences(
    preferences: SharedPreferences,
    failureMessage: String,
) {
    check(preferences.edit().clear().commit()) {
        failureMessage
    }
}

private fun isAndroidKeyStoreUnavailable(cause: Throwable): Boolean {
    val className = cause::class.java.name
    val message = cause.message.orEmpty()
    val hasKeyStoreClue =
        className.contains("KeyStore", ignoreCase = true) ||
            message.contains("AndroidKeyStore", ignoreCase = true) ||
            message.contains("KeyStore", ignoreCase = true)
    if (!hasKeyStoreClue) return false
    return cause is KeyStoreException ||
        (cause is NoSuchAlgorithmException && message.contains("AndroidKeyStore", ignoreCase = true))
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

private const val ANDROIDX_SECURITY_KEY_KEYSET = "__androidx_security_crypto_encrypted_prefs_key_keyset__"
private const val ANDROIDX_SECURITY_VALUE_KEYSET = "__androidx_security_crypto_encrypted_prefs_value_keyset__"
