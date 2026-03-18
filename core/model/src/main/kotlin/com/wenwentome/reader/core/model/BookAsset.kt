package com.wenwentome.reader.core.model

enum class AssetRole { PRIMARY_TEXT, COVER, CACHE_PACKAGE }

data class BookAsset(
    val bookId: String,
    val assetRole: AssetRole,
    val storageUri: String,
    val mime: String,
    val size: Long,
    val hash: String,
    val syncPath: String,
)

