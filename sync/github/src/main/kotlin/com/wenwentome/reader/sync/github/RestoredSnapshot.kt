package com.wenwentome.reader.sync.github

import com.wenwentome.reader.core.model.BookAsset
import com.wenwentome.reader.core.model.SyncSnapshot

data class RestoredSnapshot(
    val snapshot: SyncSnapshot,
    val assets: List<BookAsset>,
)
