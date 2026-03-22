package com.wenwentome.reader.data.apihub.sync

import com.wenwentome.reader.core.model.ApiCapabilityBinding
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiHubMergeResolverTest {
    private val mergeResolver = ApiHubMergeResolver(conflictWindowMs = 5_000)

    @Test
    fun mergeResolver_returnsConflictWhenTwoBindingsChangeInsideConflictWindow() {
        val local =
            ApiCapabilityBinding(
                capabilityId = "reader.summary",
                primaryProviderId = "openai",
                primaryModelId = "gpt-4.1-mini",
                updatedAt = 10_000,
            )
        val remote =
            ApiCapabilityBinding(
                capabilityId = "reader.summary",
                primaryProviderId = "dashscope",
                primaryModelId = "qwen-plus",
                updatedAt = 12_000,
            )

        val result = mergeResolver.mergeBinding(local = local, remote = remote)

        assertTrue(result is MergeBindingResult.Conflict)
    }

    @Test
    fun mergeResolver_prefersNewestBindingOutsideConflictWindow() {
        val local =
            ApiCapabilityBinding(
                capabilityId = "reader.summary",
                primaryProviderId = "openai",
                primaryModelId = "gpt-4.1-mini",
                updatedAt = 10_000,
            )
        val remote =
            ApiCapabilityBinding(
                capabilityId = "reader.summary",
                primaryProviderId = "dashscope",
                primaryModelId = "qwen-plus",
                updatedAt = 20_000,
            )

        val result = mergeResolver.mergeBinding(local = local, remote = remote)

        assertTrue(result is MergeBindingResult.Resolved)
        assertEquals(remote, (result as MergeBindingResult.Resolved).binding)
    }
}
