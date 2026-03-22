package com.wenwentome.reader.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "api_ability_cache")
data class ApiAbilityCacheEntity(
    @PrimaryKey val cacheKey: String,
    val payloadJson: String,
    val expiresAt: Long,
    val updatedAt: Long,
)
