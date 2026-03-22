package com.wenwentome.reader.data.apihub.secret

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SecretEnvelopeV1Test {
    @Test
    fun encryptAndDecrypt_roundTripsPlainSecret() {
        val crypto = SecretSyncCrypto()

        val envelope = crypto.encrypt(
            secretId = "provider/openai",
            plainText = "sk-live-secret",
            password = "sync-pass",
            scope = SecretScope.SYNC_ENCRYPTED,
            updatedAt = 1711000000000,
        )

        assertEquals(1, envelope.version)
        assertEquals("PBKDF2WithHmacSHA256", envelope.kdf)
        assertEquals("sk-live-secret", crypto.decrypt(envelope, "sync-pass"))
    }

    @Test
    fun decrypt_throwsWhenPasswordIsWrong() {
        val crypto = SecretSyncCrypto()
        val envelope = crypto.encrypt(
            secretId = "provider/openai",
            plainText = "sk-live-secret",
            password = "sync-pass",
            scope = SecretScope.SYNC_ENCRYPTED,
            updatedAt = 1711000000000,
        )

        assertThrows(IllegalArgumentException::class.java) {
            crypto.decrypt(envelope, "wrong-pass")
        }
    }
}
