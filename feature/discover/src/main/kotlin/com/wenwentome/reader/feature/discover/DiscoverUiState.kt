package com.wenwentome.reader.feature.discover

import com.wenwentome.reader.bridge.source.model.RemoteSearchResult
import com.wenwentome.reader.core.model.SourceDefinition

data class DiscoverUiState(
    val query: String = "",
    val results: List<RemoteSearchResult> = emptyList(),
    val addingResultIds: Set<String> = emptySet(),
    val lastAddedTitle: String? = null,
)

data class SourceManagementUiState(
    val sources: List<SourceDefinition> = emptyList(),
)
