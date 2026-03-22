package com.wenwentome.reader.data.apihub.secret

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class SecretSyncCrypto(
    private val secureRandom: SecureRandom = SecureRandom(),
    private val iterations: Int = 120_000,
    private val keyLengthBits: Int = 256,
) {
    fun encrypt(
        secretId: String,
        plainText: String,
        password: String,
        scope: SecretScope,
        updatedAt: Long,
    ): SecretEnvelopeV1 {
        require(password.isNotBlank()) { "sync password must not be blank" }
        val salt = ByteArray(SALT_BYTES).also(secureRandom::nextBytes)
        val iv = ByteArray(GCM_IV_BYTES).also(secureRandom::nextBytes)
        val secretKey = deriveKey(password = password, salt = salt)
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        }
        val cipherBytes = cipher.doFinal(plainText.encodeToByteArray())
        return SecretEnvelopeV1(
            secretId = secretId,
            scope = scope,
            iterations = iterations,
            saltBase64 = salt.toBase64(),
            ivBase64 = iv.toBase64(),
            cipherTextBase64 = cipherBytes.toBase64(),
            checksumBase64 = sha256(plainText.encodeToByteArray()).toBase64(),
            updatedAt = updatedAt,
        )
    }

    fun decrypt(
        envelope: SecretEnvelopeV1,
        password: String,
    ): String {
        require(password.isNotBlank()) { "sync password must not be blank" }
        val secretKey = deriveKey(password = password, salt = envelope.saltBase64.fromBase64(), iterations = envelope.iterations)
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION).apply {
            init(
                Cipher.DECRYPT_MODE,
                secretKey,
                GCMParameterSpec(GCM_TAG_LENGTH_BITS, envelope.ivBase64.fromBase64()),
            )
        }
        val plainBytes =
            runCatching {
                cipher.doFinal(envelope.cipherTextBase64.fromBase64())
            }.getOrElse { error ->
                throw IllegalArgumentException("Unable to decrypt secret envelope ${envelope.secretId}", error)
            }
        val checksum = sha256(plainBytes).toBase64()
        require(checksum == envelope.checksumBase64) {
            "Secret envelope checksum mismatch for ${envelope.secretId}"
        }
        return plainBytes.decodeToString()
    }

    private fun deriveKey(
        password: String,
        salt: ByteArray,
        iterations: Int = this.iterations,
    ): SecretKeySpec {
        val keyFactory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, keyLengthBits)
        val encoded = keyFactory.generateSecret(spec).encoded
        return SecretKeySpec(encoded, KEY_ALGORITHM)
    }

    private fun sha256(bytes: ByteArray): ByteArray =
        MessageDigest.getInstance(SHA_256).digest(bytes)

    private fun ByteArray.toBase64(): String = Base64.getEncoder().encodeToString(this)

    private fun String.fromBase64(): ByteArray = Base64.getDecoder().decode(this)

    private companion object {
        const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_IV_BYTES = 12
        const val GCM_TAG_LENGTH_BITS = 128
        const val KEY_ALGORITHM = "AES"
        const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
        const val SALT_BYTES = 16
        const val SHA_256 = "SHA-256"
    }
}
