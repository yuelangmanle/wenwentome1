package com.wenwentome.reader.core.model

enum class RemoteSyncMode { LATEST_ONLY, PINNED_TOC }

data class RemoteBinding(
    val bookId: String,
    val sourceId: String,
    val remoteBookId: String,
    val remoteBookUrl: String,
    val tocRef: String? = null,
    val syncMode: RemoteSyncMode = RemoteSyncMode.LATEST_ONLY,
    val lastFetchedAt: Long? = null,
)

