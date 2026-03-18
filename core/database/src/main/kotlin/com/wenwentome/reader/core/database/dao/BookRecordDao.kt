package com.wenwentome.reader.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.wenwentome.reader.core.database.entity.BookRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookRecordDao {
    @Upsert
    suspend fun upsert(entity: BookRecordEntity)

    @Upsert
    suspend fun upsertAll(entities: List<BookRecordEntity>)

    @Query("SELECT * FROM book_records WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<BookRecordEntity?>

    @Query("SELECT * FROM book_records ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<BookRecordEntity>>

    @Query("DELETE FROM book_records")
    suspend fun clearAll()

    @Transaction
    suspend fun replaceAll(entities: List<BookRecordEntity>) {
        clearAll()
        upsertAll(entities)
    }
}

