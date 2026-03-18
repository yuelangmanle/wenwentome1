package com.wenwentome.reader.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.wenwentome.reader.core.model.RemoteSyncMode

@Entity(tableName = "remote_bindings")
data class RemoteBindingEntity(
    @PrimaryKey val bookId: String,
    val sourceId: String,
    val remoteBookId: String,
    val remoteBookUrl: String,
    val tocRef: String? = null,
    val syncMode: RemoteSyncMode = RemoteSyncMode.LATEST_ONLY,
    val lastFetchedAt: Long? = null,
)

