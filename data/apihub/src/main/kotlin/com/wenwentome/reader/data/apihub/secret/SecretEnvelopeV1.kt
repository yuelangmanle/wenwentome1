package com.wenwentome.reader.data.apihub.secret

import kotlinx.serialization.Serializable

@Serializable
enum class SecretScope {
    LOCAL_ONLY,
    SYNC_ENCRYPTED,
}

@Serializable
data class SecretEnvelopeV1(
    val secretId: String,
    val version: Int = 1,
    val scope: SecretScope,
    val kdf: String = "PBKDF2WithHmacSHA256",
    val iterations: Int,
    val saltBase64: String,
    val ivBase64: String,
    val cipherTextBase64: String,
    val checksumBase64: String,
    val updatedAt: Long,
)
