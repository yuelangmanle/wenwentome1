package com.wenwentome.reader.sync.github

import com.wenwentome.reader.core.model.ApiCapabilityBinding

data class SyncManifest(
    val snapshotPath: String = "manifests/latest.json",
    val bookRecordsPath: String = "data/book-records.json",
    val readingStatesPath: String = "data/reading-states.json",
    val remoteBindingsPath: String = "data/remote-bindings.json",
    val sourceDefinitionsPath: String = "data/source-definitions.json",
    val preferencesPath: String = "data/preferences.json",
    val capabilityBindingsPath: String = "data/api-capability-bindings.json",
    val secretEnvelopesPath: String = "data/secret-envelopes.json",
    val assetIndexPath: String = "data/assets.json",
)

data class PreferencesSnapshot(
    val owner: String,
    val repo: String,
    val branch: String,
    val deviceId: String,
    val bootstrapToken: String = "",
)

enum class SecretScopePayload {
    LOCAL_ONLY,
    SYNC_ENCRYPTED,
}

data class SecretEnvelopePayload(
    val secretId: String,
    val scope: SecretScopePayload,
    val version: Int = 1,
    val kdf: String = "PBKDF2WithHmacSHA256",
    val iterations: Int,
    val saltBase64: String,
    val ivBase64: String,
    val cipherTextBase64: String,
    val checksumBase64: String,
    val updatedAt: Long,
)

data class CapabilityBindingConflict(
    val capabilityId: String,
    val local: ApiCapabilityBinding,
    val remote: ApiCapabilityBinding,
)
