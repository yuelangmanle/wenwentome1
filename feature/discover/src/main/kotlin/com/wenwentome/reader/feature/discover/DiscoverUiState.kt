package com.wenwentome.reader.feature.discover

import com.wenwentome.reader.bridge.source.model.RemoteBookDetail
import com.wenwentome.reader.bridge.source.model.RemoteSearchResult
import com.wenwentome.reader.core.model.SourceDefinition

data class DiscoverUiState(
    val draftQuery: String = "",
    val browserDraftQuery: String = "",
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
    val browserSearchEngineLabel: String = "必应",
    val browserModeLabel: String = "智能阅读",
    val autoOptimizeReadingLabel: String = "自动识别正文",
    val browserAvailableEngines: List<DiscoverBrowserEngine> = DiscoverBrowserEngine.defaults(),
)

data class DiscoverBrowserEngine(
    val id: String,
    val label: String,
) {
    companion object {
        fun defaults(): List<DiscoverBrowserEngine> =
            listOf(
                DiscoverBrowserEngine(id = "bing", label = "必应"),
                DiscoverBrowserEngine(id = "baidu", label = "百度"),
                DiscoverBrowserEngine(id = "sogou", label = "搜狗"),
                DiscoverBrowserEngine(id = "sohu", label = "搜狐"),
                DiscoverBrowserEngine(id = "shenma", label = "神马"),
                DiscoverBrowserEngine(id = "google", label = "谷歌"),
                DiscoverBrowserEngine(id = "custom", label = "自定义"),
            )
    }
}

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
