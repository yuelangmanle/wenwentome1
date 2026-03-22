package com.wenwentome.reader.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.wenwentome.reader.core.database.entity.ApiProviderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ApiProviderDao {
    @Upsert
    suspend fun upsert(entity: ApiProviderEntity)

    @Upsert
    suspend fun upsertAll(entities: List<ApiProviderEntity>)

    @Query("SELECT * FROM api_providers WHERE providerId = :providerId LIMIT 1")
    fun observeById(providerId: String): Flow<ApiProviderEntity?>

    @Query("SELECT * FROM api_providers ORDER BY displayName")
    fun observeAll(): Flow<List<ApiProviderEntity>>

    @Query("SELECT * FROM api_providers ORDER BY displayName")
    suspend fun getAll(): List<ApiProviderEntity>

    @Query("DELETE FROM api_providers WHERE providerId = :providerId")
    suspend fun deleteById(providerId: String)

    @Query("DELETE FROM api_providers")
    suspend fun clearAll()

    @Transaction
    suspend fun replaceAll(entities: List<ApiProviderEntity>) {
        clearAll()
        upsertAll(entities)
    }
}
