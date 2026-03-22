package com.wenwentome.reader.core.model

data class ApiCapabilityBinding(
    val capabilityId: String,
    val primaryProviderId: String,
    val primaryModelId: String,
    val fallbackProviderId: String? = null,
    val fallbackModelId: String? = null,
    val enabled: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis(),
)
