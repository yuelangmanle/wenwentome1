package com.wenwentome.reader.sync.github

data class SyncManifest(
    val snapshotPath: String = "manifests/latest.json",
    val bookRecordsPath: String = "data/book-records.json",
    val readingStatesPath: String = "data/reading-states.json",
    val remoteBindingsPath: String = "data/remote-bindings.json",
    val sourceDefinitionsPath: String = "data/source-definitions.json",
    val preferencesPath: String = "data/preferences.json",
    val assetIndexPath: String = "data/assets.json",
)

data class PreferencesSnapshot(
    val owner: String,
    val repo: String,
    val branch: String,
    val token: String,
    val deviceId: String,
)
