package com.wenwentome.reader.feature.library

import com.wenwentome.reader.core.database.dao.BookAssetDao
import com.wenwentome.reader.core.database.dao.BookRecordDao
import com.wenwentome.reader.core.database.dao.ReadingStateDao
import com.wenwentome.reader.core.database.dao.RemoteBindingDao
import com.wenwentome.reader.core.database.toModel
import com.wenwentome.reader.core.model.AssetRole
import com.wenwentome.reader.core.model.BookAsset
import com.wenwentome.reader.core.model.BookRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlin.math.roundToInt

fun interface ObserveBookshelfUseCase {
    operator fun invoke(): Flow<List<LibraryBookItem>>

    companion object {
        fun from(
            bookRecordDao: BookRecordDao,
            readingStateDao: ReadingStateDao,
            remoteBindingDao: RemoteBindingDao,
            bookAssetDao: BookAssetDao,
        ): ObserveBookshelfUseCase =
            ObserveBookshelfUseCase {
                combine(
                    bookRecordDao.observeAll(),
                    readingStateDao.observeAll(),
                    remoteBindingDao.observeAll(),
                    bookAssetDao.observeAll(),
                ) { bookEntities, readingStates, bindings, assets ->
                    val stateByBookId = readingStates.associateBy { it.bookId }
                    val bindingByBookId = bindings.associateBy { it.bookId }
                    val assetsByBookId = assets.groupBy { it.bookId }

                    bookEntities.map { entity ->
                        val book = entity.toModel()
                        val readingState = stateByBookId[book.id]?.toModel()
                        val binding = bindingByBookId[book.id]?.toModel()
                        val bookAssets = assetsByBookId[book.id].orEmpty().map { it.toModel() }
                        val hasUpdates =
                            binding?.latestKnownChapterRef?.isNotBlank() == true &&
                                binding.latestKnownChapterRef != readingState?.chapterRef

                        LibraryBookItem(
                            book = book,
                            effectiveCover = resolveEffectiveCover(book, bookAssets),
                            progressPercent = readingState?.progressPercent ?: 0f,
                            progressLabel = formatProgressLabel(readingState?.progressPercent ?: 0f),
                            hasUpdates = hasUpdates,
                            canRestoreAutomaticCover = hasManualCover(bookAssets),
                        )
                    }
                }
            }
    }
}

private fun resolveEffectiveCover(
    book: BookRecord,
    assets: List<BookAsset>,
): String? {
    val coverAssets = assets.filter { it.assetRole == AssetRole.COVER }
    val manualCover = coverAssets.firstOrNull(::isManualCover)
    if (manualCover != null) return manualCover.storageUri
    val automaticCover = coverAssets.firstOrNull()
    return automaticCover?.storageUri ?: book.cover
}

private fun hasManualCover(assets: List<BookAsset>): Boolean =
    assets.any { it.assetRole == AssetRole.COVER && isManualCover(it) }

private fun isManualCover(asset: BookAsset): Boolean =
    asset.syncPath.substringAfterLast('/').startsWith("manual-cover.")

private fun formatProgressLabel(progressPercent: Float): String =
    "${(progressPercent.coerceIn(0f, 1f) * 100).roundToInt()}%"
