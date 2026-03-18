package com.wenwentome.reader.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.wenwentome.reader.core.database.entity.RemoteBindingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RemoteBindingDao {
    @Upsert
    suspend fun upsert(entity: RemoteBindingEntity)

    @Upsert
    suspend fun upsertAll(entities: List<RemoteBindingEntity>)

    @Query("SELECT * FROM remote_bindings WHERE bookId = :bookId LIMIT 1")
    fun observeByBookId(bookId: String): Flow<RemoteBindingEntity?>

    @Query("DELETE FROM remote_bindings")
    suspend fun clearAll()

    @Transaction
    suspend fun replaceAll(entities: List<RemoteBindingEntity>) {
        clearAll()
        upsertAll(entities)
    }
}

