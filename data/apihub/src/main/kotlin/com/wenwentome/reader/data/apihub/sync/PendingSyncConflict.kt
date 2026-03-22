package com.wenwentome.reader.data.apihub.sync

import com.wenwentome.reader.core.model.ApiCapabilityBinding

data class PendingSyncConflict(
    val capabilityId: String,
    val local: ApiCapabilityBinding,
    val remote: ApiCapabilityBinding,
)
