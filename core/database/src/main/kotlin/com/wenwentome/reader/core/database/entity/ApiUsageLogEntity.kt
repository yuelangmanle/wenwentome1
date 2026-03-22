package com.wenwentome.reader.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "api_usage_logs")
data class ApiUsageLogEntity(
    @PrimaryKey val callId: String,
    val capabilityId: String,
    val providerId: String,
    val modelId: String,
    val success: Boolean,
    val bookId: String? = null,
    val chapterRef: String? = null,
    val durationMs: Long? = null,
    val estimatedCostMicros: Long,
    val errorMessage: String? = null,
    val createdAt: Long,
)
