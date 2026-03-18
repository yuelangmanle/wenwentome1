package com.wenwentome.reader.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.wenwentome.reader.core.database.entity.BookAssetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookAssetDao {
    @Upsert
    suspend fun upsert(entity: BookAssetEntity)

    @Upsert
    suspend fun upsertAll(entities: List<BookAssetEntity>)

    @Query("SELECT * FROM book_assets WHERE bookId = :bookId")
    fun observeByBookId(bookId: String): Flow<List<BookAssetEntity>>

    @Query("DELETE FROM book_assets")
    suspend fun clearAll()

    @Transaction
    suspend fun replaceAll(entities: List<BookAssetEntity>) {
        clearAll()
        upsertAll(entities)
    }
}

