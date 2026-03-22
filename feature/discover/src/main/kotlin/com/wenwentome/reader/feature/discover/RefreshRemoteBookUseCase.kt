package com.wenwentome.reader.feature.discover

import com.wenwentome.reader.bridge.source.SourceBridgeRepository
import com.wenwentome.reader.bridge.source.model.RemoteBookDetail
import com.wenwentome.reader.bridge.source.model.RemoteSearchResult
import com.wenwentome.reader.core.database.dao.ReadingStateDao
import com.wenwentome.reader.core.database.dao.RemoteBindingDao
import kotlinx.coroutines.flow.first

data class RefreshRemoteBookResult(
    val latestKnownChapterRef: String?,
    val hasUpdates: Boolean,
    val activeSourceId: String,
    val activeRemoteBookId: String,
    val activeRemoteBookUrl: String,
    val autoSwitched: Boolean,
    val primarySourceId: String,
    val primarySourceFailed: Boolean,
)

fun interface RefreshRemoteBook {
    suspend operator fun invoke(bookId: String): RefreshRemoteBookResult
}

/**
 * 手动刷新 Web 书籍的目录元数据：只在这里写入 latestKnownChapterRef / lastCatalogRefreshAt。
 */
class RefreshRemoteBookUseCase(
    private val sourceBridgeRepository: SourceBridgeRepository,
    private val remoteBindingDao: RemoteBindingDao,
    private val readingStateDao: ReadingStateDao,
    private val healthTracker: SourceHealthTracker = SourceHealthTracker(),
    private val findFallbackCandidates: suspend (String, String, String) -> List<RemoteSearchResult> =
        { _, primarySourceId, query ->
            sourceBridgeRepository.search(query, emptyList()).filter { result ->
                result.sourceId != primarySourceId
            }
        },
    private val now: () -> Long = { System.currentTimeMillis() },
) : RefreshRemoteBook {
    override suspend fun invoke(bookId: String): RefreshRemoteBookResult {
        val binding = remoteBindingDao.observeByBookId(bookId).first()
        requireNotNull(binding) { "Remote binding not found for bookId=$bookId" }

        val readingState = readingStateDao.observeByBookId(bookId).first()
        val primarySourceId = binding.sourceId
        val primaryRemoteBookId = binding.remoteBookId
        val primaryRemoteBookUrl = binding.remoteBookUrl
        val primaryAttempt =
            attemptSourceRefresh(
                sourceId = primarySourceId,
                remoteBookId = primaryRemoteBookId,
            )

        return primaryAttempt.fold(
            onSuccess = { refreshed ->
            val latestKnownChapterRef =
                    resolveLatestKnownChapterRefForRefresh(
                        refreshed.detail,
                        refreshed.toc,
                        binding.latestKnownChapterRef,
                    )
            val hasUpdates =
                latestKnownChapterRef != null && latestKnownChapterRef != readingState?.chapterRef
            remoteBindingDao.upsert(
                binding.copy(
                    tocRef = refreshed.toc.firstOrNull()?.chapterRef ?: binding.tocRef,
                    latestKnownChapterRef = latestKnownChapterRef,
                    lastCatalogRefreshAt = now(),
                ),
            )
                RefreshRemoteBookResult(
                latestKnownChapterRef = latestKnownChapterRef,
                hasUpdates = hasUpdates,
                activeSourceId = primarySourceId,
                activeRemoteBookId = primaryRemoteBookId,
                activeRemoteBookUrl = primaryRemoteBookUrl,
                autoSwitched = false,
                primarySourceId = primarySourceId,
                primarySourceFailed = false,
            )
            },
            onFailure = { primaryError ->
                val query = resolveFallbackQuery(binding.remoteBookUrl, binding.remoteBookId, null)
                    ?: throw primaryError
                val fallback =
                    findFallbackCandidates(bookId, primarySourceId, query)
                        .firstOrNull()
                        ?: throw primaryError
                val fallbackAttempt =
                    attemptSourceRefresh(
                        sourceId = fallback.sourceId,
                        remoteBookId = fallback.id,
                    ).getOrElse { throw primaryError }
                val latestKnownChapterRef =
                    resolveLatestKnownChapterRefForRefresh(
                        fallbackAttempt.detail,
                        fallbackAttempt.toc,
                        binding.latestKnownChapterRef,
                    )
                val hasUpdates =
                    latestKnownChapterRef != null && latestKnownChapterRef != readingState?.chapterRef
                remoteBindingDao.upsert(
                    binding.copy(
                        sourceId = fallback.sourceId,
                        remoteBookId = fallback.id,
                        remoteBookUrl = fallback.detailUrl,
                        tocRef = fallbackAttempt.toc.firstOrNull()?.chapterRef,
                        latestKnownChapterRef = latestKnownChapterRef,
                        lastCatalogRefreshAt = now(),
                    ),
                )
                RefreshRemoteBookResult(
                    latestKnownChapterRef = latestKnownChapterRef,
                    hasUpdates = hasUpdates,
                    activeSourceId = fallback.sourceId,
                    activeRemoteBookId = fallback.id,
                    activeRemoteBookUrl = fallback.detailUrl,
                    autoSwitched = true,
                    primarySourceId = primarySourceId,
                    primarySourceFailed = true,
                )
            },
        )
    }

    private suspend fun attemptSourceRefresh(
        sourceId: String,
        remoteBookId: String,
    ): Result<RefreshedRemoteBook> {
        val startedAt = System.currentTimeMillis()
        return runCatching {
            val detail = sourceBridgeRepository.fetchBookDetail(sourceId, remoteBookId)
            val toc = sourceBridgeRepository.fetchToc(sourceId, remoteBookId)
            RefreshedRemoteBook(detail = detail, toc = toc)
        }.onSuccess {
            healthTracker.recordResult(
                sourceId = sourceId,
                success = true,
                latencyMs = System.currentTimeMillis() - startedAt,
            )
        }.onFailure {
            healthTracker.recordResult(
                sourceId = sourceId,
                success = false,
                latencyMs = System.currentTimeMillis() - startedAt,
            )
        }
    }

    private fun resolveFallbackQuery(
        remoteBookUrl: String,
        remoteBookId: String,
        detail: RemoteBookDetail?,
    ): String? {
        detail?.title?.takeIf { it.isNotBlank() }?.let { return it }
        val fromUrl = remoteBookUrl.substringAfterLast('/').substringBefore('?').trim()
        if (fromUrl.isNotBlank()) {
            return fromUrl.replace('-', ' ').replace('_', ' ')
        }
        return remoteBookId.takeIf { it.isNotBlank() }
    }

    private data class RefreshedRemoteBook(
        val detail: RemoteBookDetail,
        val toc: List<com.wenwentome.reader.bridge.source.model.RemoteChapter>,
    )
}
