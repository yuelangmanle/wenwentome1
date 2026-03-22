package com.wenwentome.reader.data.apihub.sync

import com.wenwentome.reader.core.model.ApiCapabilityBinding
import kotlin.math.abs

sealed interface MergeBindingResult {
    data class Resolved(
        val binding: ApiCapabilityBinding,
    ) : MergeBindingResult

    data class Conflict(
        val local: ApiCapabilityBinding,
        val remote: ApiCapabilityBinding,
        val pending: PendingSyncConflict =
            PendingSyncConflict(
                capabilityId = local.capabilityId,
                local = local,
                remote = remote,
            ),
    ) : MergeBindingResult
}

class ApiHubMergeResolver(
    private val conflictWindowMs: Long = DEFAULT_CONFLICT_WINDOW_MS,
) {
    fun mergeBinding(
        local: ApiCapabilityBinding,
        remote: ApiCapabilityBinding,
    ): MergeBindingResult =
        if (abs(local.updatedAt - remote.updatedAt) <= conflictWindowMs) {
            MergeBindingResult.Conflict(local = local, remote = remote)
        } else {
            MergeBindingResult.Resolved(
                binding = if (local.updatedAt >= remote.updatedAt) local else remote,
            )
        }

    private companion object {
        const val DEFAULT_CONFLICT_WINDOW_MS = 5 * 60 * 1000L
    }
}
