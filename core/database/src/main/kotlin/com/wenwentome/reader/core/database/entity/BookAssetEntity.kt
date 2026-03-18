package com.wenwentome.reader.core.database.entity

import androidx.room.Entity
import com.wenwentome.reader.core.model.AssetRole

@Entity(
    tableName = "book_assets",
    primaryKeys = ["bookId", "assetRole"],
)
data class BookAssetEntity(
    val bookId: String,
    val assetRole: AssetRole,
    val storageUri: String,
    val mime: String,
    val size: Long,
    val hash: String,
    val syncPath: String,
)

