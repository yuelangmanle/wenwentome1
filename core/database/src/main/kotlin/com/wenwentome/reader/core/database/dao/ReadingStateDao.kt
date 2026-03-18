package com.wenwentome.reader.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.wenwentome.reader.core.database.entity.ReadingStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingStateDao {
    @Upsert
    suspend fun upsert(entity: ReadingStateEntity)

    @Upsert
    suspend fun upsertAll(entities: List<ReadingStateEntity>)

    @Query("SELECT * FROM reading_states WHERE bookId = :bookId LIMIT 1")
    fun observeByBookId(bookId: String): Flow<ReadingStateEntity?>

    @Query("DELETE FROM reading_states")
    suspend fun clearAll()

    @Transaction
    suspend fun replaceAll(entities: List<ReadingStateEntity>) {
        clearAll()
        upsertAll(entities)
    }
}

