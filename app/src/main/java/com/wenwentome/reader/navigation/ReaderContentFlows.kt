package com.wenwentome.reader.navigation

import com.wenwentome.reader.bridge.source.SourceBridgeRepository
import com.wenwentome.reader.core.database.dao.RemoteBindingDao
import com.wenwentome.reader.core.database.toModel
import com.wenwentome.reader.core.model.BookRecord
import com.wenwentome.reader.core.model.OriginType
import com.wenwentome.reader.core.model.ReadingState
import com.wenwentome.reader.data.localbooks.LocalBookContentRepository
import com.wenwentome.reader.data.localbooks.ReaderContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

fun readerContentFlow(
    book: BookRecord?,
    bookId: String,
    observeReadingState: Flow<ReadingState?>,
    localBookContentRepository: LocalBookContentRepository,
    remoteBindingDao: RemoteBindingDao,
    sourceBridgeRepository: SourceBridgeRepository,
): Flow<ReaderContent> {
    if (book == null) {
        return flowOf(ReaderContent(chapterTitle = "", paragraphs = emptyList()))
    }

    return when (book.originType) {
        OriginType.LOCAL -> observeReadingState.flatMapLatest { state ->
            flow {
                emit(
                    runCatching {
                        withContext(Dispatchers.IO) {
                            localBookContentRepository.load(bookId, state?.locator)
                        }
                    }.getOrElse { error ->
                        ReaderContent(
                            chapterTitle = book.title,
                            paragraphs = listOf(error.message ?: "正文加载失败"),
                        )
                    }
                )
            }
        }

        OriginType.WEB, OriginType.MIXED -> {
            val observeBinding = remoteBindingDao.observeByBookId(bookId).map { it?.toModel() }
            observeReadingState
                .combine(observeBinding) { state, binding -> state to binding }
                .flatMapLatest { (state, binding) ->
                    flow {
                        if (binding == null) {
                            emit(
                                ReaderContent(
                                    chapterTitle = book.title,
                                    paragraphs = listOf("未找到书源绑定"),
                                )
                            )
                            return@flow
                        }
                        val chapterRef =
                            state?.chapterRef?.takeIf { it.isNotBlank() }
                                ?: binding.latestKnownChapterRef?.takeIf { it.isNotBlank() }
                                ?: binding.tocRef?.takeIf { it.isNotBlank() }
                        if (chapterRef == null) {
                            emit(
                                ReaderContent(
                                    chapterTitle = book.title,
                                    paragraphs = listOf("未找到可阅读章节"),
                                )
                            )
                            return@flow
                        }

                        emit(
                            runCatching {
                                val remote = withContext(Dispatchers.IO) {
                                    sourceBridgeRepository.fetchChapterContent(
                                        sourceId = binding.sourceId,
                                        chapterRef = chapterRef,
                                    )
                                }
                                ReaderContent(
                                    chapterTitle = remote.title.ifBlank { book.title },
                                    paragraphs = splitToParagraphs(remote.content),
                                )
                            }.getOrElse { error ->
                                ReaderContent(
                                    chapterTitle = book.title,
                                    paragraphs = listOf(error.message ?: "正文加载失败"),
                                )
                            }
                        )
                    }
                }
        }
    }
}

private fun splitToParagraphs(raw: String): List<String> {
    val normalized = raw
        .replace("\r\n", "\n")
        .replace('\r', '\n')
        .trim()
    if (normalized.isBlank()) return emptyList()
    val parts = normalized
        .split(Regex("\n+"))
        .map { it.trim() }
        .filter { it.isNotBlank() }
    return if (parts.isEmpty()) listOf(normalized) else parts
}

