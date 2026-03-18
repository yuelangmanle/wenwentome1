package com.wenwentome.reader.feature.library

import com.wenwentome.reader.core.database.dao.BookRecordDao
import com.wenwentome.reader.core.database.toModel
import com.wenwentome.reader.core.model.BookRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

fun interface ObserveBookshelfUseCase {
    operator fun invoke(): Flow<List<BookRecord>>

    companion object {
        fun from(bookRecordDao: BookRecordDao): ObserveBookshelfUseCase =
            ObserveBookshelfUseCase {
                bookRecordDao.observeAll().map { entities ->
                    entities.map { it.toModel() }
                }
            }
    }
}

