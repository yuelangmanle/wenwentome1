package com.wenwentome.reader.feature.discover

import com.wenwentome.reader.bridge.source.SourceBridgeRepository
import com.wenwentome.reader.core.database.dao.ReadingStateDao
import com.wenwentome.reader.core.database.dao.RemoteBindingDao
import kotlinx.coroutines.flow.first

data class RefreshRemoteBookResult(
    val latestKnownChapterRef: String?,
    val hasUpdates: Boolean,
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
    private val now: () -> Long = { System.currentTimeMillis() },
) : RefreshRemoteBook {
    override suspend fun invoke(bookId: String): RefreshRemoteBookResult {
        val binding = remoteBindingDao.observeByBookId(bookId).first()
        requireNotNull(binding) { "Remote binding not found for bookId=$bookId" }

        val readingState = readingStateDao.observeByBookId(bookId).first()

        val detail = sourceBridgeRepository.fetchBookDetail(binding.sourceId, binding.remoteBookId)
        val toc = sourceBridgeRepository.fetchToc(binding.sourceId, binding.remoteBookId)
        val latestKnownChapterRef = resolveLatestKnownChapterRef(detail, toc)

        val hasUpdates =
            latestKnownChapterRef != null && latestKnownChapterRef != readingState?.chapterRef

        remoteBindingDao.upsert(
            binding.copy(
                latestKnownChapterRef = latestKnownChapterRef,
                lastCatalogRefreshAt = now(),
            ),
        )

        return RefreshRemoteBookResult(
            latestKnownChapterRef = latestKnownChapterRef,
            hasUpdates = hasUpdates,
        )
    }
}

