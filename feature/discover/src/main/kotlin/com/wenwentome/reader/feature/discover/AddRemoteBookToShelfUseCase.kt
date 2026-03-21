package com.wenwentome.reader.feature.discover

import com.wenwentome.reader.bridge.source.SourceBridgeRepository
import com.wenwentome.reader.bridge.source.model.RemoteBookDetail
import com.wenwentome.reader.bridge.source.model.RemoteChapter
import com.wenwentome.reader.bridge.source.model.RemoteSearchResult
import com.wenwentome.reader.core.database.dao.BookRecordDao
import com.wenwentome.reader.core.database.dao.RemoteBindingDao
import com.wenwentome.reader.core.database.toEntity
import com.wenwentome.reader.core.model.BookFormat
import com.wenwentome.reader.core.model.BookRecord
import com.wenwentome.reader.core.model.OriginType
import com.wenwentome.reader.core.model.RemoteBinding
import java.util.UUID

fun interface AddRemoteBookToShelf {
    suspend operator fun invoke(result: RemoteSearchResult): String
}

private fun normalizeChapterTitleForMatch(raw: String): String {
    // 最小有效规范化：去掉“最新章节”前缀、处理全角空格/多余空白、统一常见标点差异。
    var text = raw.replace('\u3000', ' ').trim()
    text = text.replace(Regex("^最新章节\\s*[:：]?\\s*"), "")
    // 全角冒号/半角冒号差异，以及一些站点会插入的多余标点
    text = text.replace("：", ":").replace(":", "")
    // 去掉所有空白，避免“ 第 三 章 ”这类格式噪音
    text = text.replace(Regex("\\s+"), "")
    return text
}

private fun findChapterRefByLastChapterTitle(
    lastChapterTitle: String,
    toc: List<RemoteChapter>,
): String? {
    if (toc.isEmpty()) return null
    val target = normalizeChapterTitleForMatch(lastChapterTitle)
    if (target.isBlank()) return null
    return toc.firstOrNull { normalizeChapterTitleForMatch(it.title) == target }?.chapterRef
}

internal fun resolveLatestKnownChapterRefForAdd(
    detail: RemoteBookDetail,
    toc: List<RemoteChapter>,
): String? {
    if (toc.isEmpty()) return null
    val lastChapterTitle = detail.lastChapter?.takeIf { it.isNotBlank() }
    if (lastChapterTitle != null) {
        // add-to-shelf: 有 lastChapter 但无法匹配时，不要硬猜 toc.last()
        return findChapterRefByLastChapterTitle(lastChapterTitle, toc)
    }
    // add-to-shelf: 没有 lastChapter 时 best-effort 取 toc.last()
    return toc.lastOrNull()?.chapterRef
}

internal fun resolveLatestKnownChapterRefForRefresh(
    detail: RemoteBookDetail,
    toc: List<RemoteChapter>,
    existingLatestKnownChapterRef: String?,
): String? {
    val lastChapterTitle = detail.lastChapter?.takeIf { it.isNotBlank() }
    if (lastChapterTitle != null) {
        // refresh: 优先按详情页 lastChapter 匹配，匹配不到就保留旧值
        return findChapterRefByLastChapterTitle(lastChapterTitle, toc) ?: existingLatestKnownChapterRef
    }
    // refresh: 没有 lastChapter 时优先保留旧值；旧值没有再 best-effort 用 toc.last()
    return existingLatestKnownChapterRef ?: toc.lastOrNull()?.chapterRef
}

class AddRemoteBookToShelfUseCase(
    private val sourceBridgeRepository: SourceBridgeRepository,
    private val bookRecordDao: BookRecordDao,
    private val remoteBindingDao: RemoteBindingDao,
) : AddRemoteBookToShelf {
    override suspend fun invoke(result: RemoteSearchResult): String {
        val existingBinding = remoteBindingDao.getByRemoteBook(result.sourceId, result.id)
        if (existingBinding != null) {
            return existingBinding.bookId
        }
        val detail = sourceBridgeRepository.fetchBookDetail(result.sourceId, result.id)
        val toc = sourceBridgeRepository.fetchToc(result.sourceId, result.id)
        val latestKnownChapterRef = resolveLatestKnownChapterRefForAdd(detail, toc)
        val bookId = UUID.randomUUID().toString()
        val book = BookRecord(
            id = bookId,
            title = detail.title.ifBlank { result.title },
            author = detail.author ?: result.author,
            originType = OriginType.WEB,
            primaryFormat = BookFormat.WEB,
            cover = detail.coverUrl,
            summary = detail.summary,
        )
        bookRecordDao.upsert(book.toEntity())
        remoteBindingDao.upsert(
            RemoteBinding(
                bookId = book.id,
                sourceId = result.sourceId,
                remoteBookId = result.id,
                remoteBookUrl = result.detailUrl,
                tocRef = toc.firstOrNull()?.chapterRef,
                latestKnownChapterRef = latestKnownChapterRef,
            ).toEntity()
        )
        return book.id
    }
}
