package com.wenwentome.reader.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "api_capability_bindings")
data class ApiCapabilityBindingEntity(
    @PrimaryKey val capabilityId: String,
    val primaryProviderId: String,
    val primaryModelId: String,
    val fallbackProviderId: String? = null,
    val fallbackModelId: String? = null,
    val enabled: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis(),
)
