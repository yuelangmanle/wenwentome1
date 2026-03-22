package com.wenwentome.reader.data.apihub.web

import kotlin.math.roundToInt

data class SourceHealthSnapshot(
    val sourceId: String,
    val successRate: Float,
    val medianLatencyMs: Long,
    val lastFailureAt: Long?,
)

data class WebNovelSearchResult(
    val id: String,
    val sourceId: String,
    val title: String,
    val author: String? = null,
    val detailUrl: String? = null,
    val healthScore: Float = 0.5f,
    val healthLabel: String = "未知",
    val boostReason: String? = null,
)

class WebNovelEnhancementFacade(
    private val nowProvider: () -> Long = { System.currentTimeMillis() },
) {
    private val states = linkedMapOf<String, HealthState>()

    fun recordSourceResult(
        sourceId: String,
        success: Boolean,
        latencyMs: Long,
    ) {
        val now = nowProvider()
        val state = states.getOrPut(sourceId) { HealthState() }
        state.update(success = success, latencyMs = latencyMs, now = now)
    }

    fun healthScore(sourceId: String): Float =
        states[sourceId]?.score() ?: DEFAULT_SCORE

    fun healthSnapshot(sourceId: String): SourceHealthSnapshot? {
        val state = states[sourceId] ?: return null
        return SourceHealthSnapshot(
            sourceId = sourceId,
            successRate = state.successRate,
            medianLatencyMs = state.latencyEma.roundToInt().toLong(),
            lastFailureAt = state.lastFailureAt,
        )
    }

    fun enhanceSearchResults(
        results: List<WebNovelSearchResult>,
        boostedSourceIds: Set<String> = emptySet(),
        preferredTitle: String? = null,
        preferredAuthor: String? = null,
    ): List<WebNovelSearchResult> {
        if (results.isEmpty()) return results
        return results
            .map { result ->
                val state = states.getOrPut(result.sourceId) { HealthState() }
                val healthScore = state.score()
                val boosted = result.sourceId in boostedSourceIds
                val boostReasons = mutableListOf<String>()
                var rankingBonus = 0f
                if (boosted) {
                    rankingBonus += BOOST_BONUS
                    boostReasons += "API 加速"
                }
                val titleMatch = matchTitle(preferredTitle, result.title)
                if (titleMatch > 0f) {
                    rankingBonus += titleMatch
                    boostReasons += if (titleMatch >= EXACT_TITLE_BONUS) "题名直达" else "题名接近"
                }
                if (matchAuthor(preferredAuthor, result.author)) {
                    rankingBonus += AUTHOR_MATCH_BONUS
                    boostReasons += "作者命中"
                }
                RankedSearchResult(
                    result =
                        result.copy(
                            healthScore = healthScore,
                            healthLabel = healthLabel(healthScore),
                            boostReason = boostReasons.takeIf { it.isNotEmpty() }?.joinToString(" · "),
                        ),
                    rankingScore = (healthScore + rankingBonus).coerceAtMost(1.2f),
                )
            }
            .sortedWith(
                compareByDescending<RankedSearchResult> { it.rankingScore }
                    .thenByDescending { it.result.healthScore }
                    .thenByDescending { it.result.title.length },
            )
            .map { it.result }
    }

    fun healthLabel(score: Float): String =
        when {
            score >= 0.85f -> "优秀"
            score >= 0.7f -> "良好"
            score >= 0.5f -> "稳定"
            score >= 0.3f -> "不稳"
            else -> "较差"
        }

    private class HealthState(
        var successRate: Float = DEFAULT_SCORE,
        var latencyEma: Float = DEFAULT_LATENCY_MS,
        var lastFailureAt: Long? = null,
    ) {
        fun update(success: Boolean, latencyMs: Long, now: Long) {
            val latency = latencyMs.coerceAtLeast(1L).toFloat()
            latencyEma = latencyEma + (latency - latencyEma) * LATENCY_ALPHA
            if (success) {
                successRate = successRate + (1f - successRate) * SUCCESS_ALPHA
            } else {
                successRate *= FAILURE_DECAY
                lastFailureAt = now
            }
        }

        fun score(): Float {
            val penalty = (1f / (1f + (latencyEma / LATENCY_PENALTY_BASE))).coerceIn(MIN_LATENCY_PENALTY, 1f)
            return (successRate * penalty).coerceIn(0f, 1f)
        }
    }

    private data class RankedSearchResult(
        val result: WebNovelSearchResult,
        val rankingScore: Float,
    )

    private fun matchTitle(
        preferredTitle: String?,
        resultTitle: String,
    ): Float {
        val preferred = normalize(preferredTitle)
        if (preferred.isBlank()) return 0f
        val candidate = normalize(resultTitle)
        return when {
            candidate == preferred -> EXACT_TITLE_BONUS
            candidate.contains(preferred) || preferred.contains(candidate) -> PARTIAL_TITLE_BONUS
            else -> 0f
        }
    }

    private fun matchAuthor(
        preferredAuthor: String?,
        resultAuthor: String?,
    ): Boolean {
        val preferred = normalize(preferredAuthor)
        val candidate = normalize(resultAuthor)
        return preferred.isNotBlank() && preferred == candidate
    }

    private fun normalize(value: String?): String =
        value
            .orEmpty()
            .lowercase()
            .replace(Regex("\\s+"), "")
            .replace(Regex("[：:·•,，。.!！?？\\-_/\\\\()（）\\[\\]【】]"), "")

    private companion object {
        const val DEFAULT_SCORE = 0.7f
        const val DEFAULT_LATENCY_MS = 1200f
        const val SUCCESS_ALPHA = 0.15f
        const val FAILURE_DECAY = 0.45f
        const val LATENCY_ALPHA = 0.2f
        const val LATENCY_PENALTY_BASE = 2500f
        const val MIN_LATENCY_PENALTY = 0.3f
        const val BOOST_BONUS = 0.05f
        const val EXACT_TITLE_BONUS = 0.08f
        const val PARTIAL_TITLE_BONUS = 0.04f
        const val AUTHOR_MATCH_BONUS = 0.03f
    }
}
