package com.wenwentome.reader.feature.discover

import com.wenwentome.reader.bridge.source.model.RemoteSearchResult
import com.wenwentome.reader.data.apihub.web.WebNovelEnhancementFacade
import com.wenwentome.reader.data.apihub.web.WebNovelSearchResult as ApiWebNovelSearchResult
import com.wenwentome.reader.data.apihub.web.SourceHealthSnapshot as ApiSourceHealthSnapshot

data class SourceHealthSnapshot(
    val sourceId: String,
    val successRate: Float,
    val medianLatencyMs: Long,
    val lastFailureAt: Long?,
)

class SourceHealthTracker(
    private val facade: WebNovelEnhancementFacade = WebNovelEnhancementFacade(),
    nowProvider: (() -> Long)? = null,
) {
    private val delegate = nowProvider?.let { WebNovelEnhancementFacade(nowProvider = it) } ?: facade

    fun recordResult(
        sourceId: String,
        success: Boolean,
        latencyMs: Long,
    ) {
        delegate.recordSourceResult(
            sourceId = sourceId,
            success = success,
            latencyMs = latencyMs,
        )
    }

    fun healthScore(sourceId: String): Float =
        delegate.healthScore(sourceId)

    fun healthSnapshot(sourceId: String): SourceHealthSnapshot? {
        val snapshot = delegate.healthSnapshot(sourceId) ?: return null
        return snapshot.toDiscoverSnapshot()
    }

    fun enhanceResults(
        results: List<RemoteSearchResult>,
        boostedSourceIds: Set<String> = emptySet(),
        preferredTitle: String? = null,
        preferredAuthor: String? = null,
    ): List<DiscoverSearchResult> {
        if (results.isEmpty()) return emptyList()
        return delegate
            .enhanceSearchResults(
                results =
                    results.map { result ->
                        ApiWebNovelSearchResult(
                            id = result.id,
                            sourceId = result.sourceId,
                            title = result.title,
                            author = result.author,
                            detailUrl = result.detailUrl,
                        )
                    },
                boostedSourceIds = boostedSourceIds,
                preferredTitle = preferredTitle,
                preferredAuthor = preferredAuthor,
            )
            .map { result ->
                DiscoverSearchResult(
                    result =
                        RemoteSearchResult(
                            id = result.id,
                            sourceId = result.sourceId,
                            title = result.title,
                            author = result.author,
                            detailUrl = result.detailUrl.orEmpty(),
                        ),
                    healthScore = result.healthScore,
                    healthLabel = result.healthLabel,
                    boostReason = result.boostReason,
                )
            }
    }

    fun healthLabel(score: Float): String =
        delegate.healthLabel(score)

    private fun ApiSourceHealthSnapshot.toDiscoverSnapshot(): SourceHealthSnapshot =
        SourceHealthSnapshot(
            sourceId = sourceId,
            successRate = successRate,
            medianLatencyMs = medianLatencyMs,
            lastFailureAt = lastFailureAt,
        )
}
