package com.wenwentome.reader.core.model

data class SyncSnapshot(
    val snapshotId: String,
    val revision: String,
    val deviceId: String,
    val mergedAt: Long,
    val manifestJson: String,
)

