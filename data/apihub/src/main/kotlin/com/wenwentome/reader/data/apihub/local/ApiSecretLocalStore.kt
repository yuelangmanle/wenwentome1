package com.wenwentome.reader.data.apihub.local

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest
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
}

private class RobolectricSecureApiSecretLocalStore(
    private val preferences: SharedPreferences,
    packageName: String,
    preferencesName: String,
) : ApiSecretLocalStore {
    private val secretKey =
        SecretKeySpec(
            MessageDigest.getInstance("SHA-256")
                .digest("$packageName:$preferencesName".encodeToByteArray()),
            "AES",
        )

    override suspend fun save(secretId: String, plainText: String) {
        persistSecret(preferences, secretId, encrypt(plainText))
    }

    override suspend fun read(secretId: String): String? =
        preferences.getString(secretId, null)?.let(::decrypt)

    override suspend fun delete(secretId: String) {
        deleteSecret(preferences, secretId)
    }

    private fun encrypt(plainText: String): String {
        val iv = ByteArray(12)
        SecureRandom().nextBytes(iv)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
        val cipherBytes = cipher.doFinal(plainText.encodeToByteArray())
        return "${iv.toBase64()}:${cipherBytes.toBase64()}"
    }

    private fun decrypt(encoded: String): String {
        val (ivBase64, cipherBase64) = encoded.split(':', limit = 2)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            secretKey,
            GCMParameterSpec(128, Base64.getDecoder().decode(ivBase64)),
        )
        return cipher.doFinal(Base64.getDecoder().decode(cipherBase64)).decodeToString()
    }
}

fun createSecureApiSecretLocalStore(
    context: Context,
    preferencesName: String,
): ApiSecretLocalStore =
    runCatching {
        EncryptedSharedPreferencesApiSecretLocalStore(
            preferences = createEncryptedPreferences(context, preferencesName),
        )
    }.getOrElse { error ->
        if (isRobolectricRuntime()) {
            RobolectricSecureApiSecretLocalStore(
                preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE),
                packageName = context.packageName,
                preferencesName = preferencesName,
            )
        } else {
            throw error
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
