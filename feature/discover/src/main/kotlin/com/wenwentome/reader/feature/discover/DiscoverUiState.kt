package com.wenwentome.reader.feature.discover

import com.wenwentome.reader.bridge.source.model.RemoteBookDetail
import com.wenwentome.reader.bridge.source.model.RemoteSearchResult
import com.wenwentome.reader.core.model.SourceDefinition

data class DiscoverUiState(
    val query: String = "",
    val results: List<DiscoverSearchResult> = emptyList(),
    val selectedResultId: String? = null,
    val selectedResult: DiscoverSearchResult? = null,
    val selectedPreview: RemoteBookDetail? = null,
    val addingResultIds: Set<String> = emptySet(),
    val refreshingResultIds: Set<String> = emptySet(),
    val lastAddedTitle: String? = null,
    val enhancementHint: String? = null,
    val lastRefreshHint: String? = null,
)

data class DiscoverSearchResult(
    val result: RemoteSearchResult,
    val healthScore: Float = 0.7f,
    val healthLabel: String = "稳定",
    val boostReason: String? = null,
) {
    val id: String get() = result.id
    val sourceId: String get() = result.sourceId
    val title: String get() = result.title
    val author: String? get() = result.author
}

data class SourceManagementUiState(
    val sources: List<SourceDefinition> = emptyList(),
)
