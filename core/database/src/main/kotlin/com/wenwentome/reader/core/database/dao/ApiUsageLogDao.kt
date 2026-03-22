package com.wenwentome.reader.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.wenwentome.reader.core.database.entity.ApiUsageLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ApiUsageLogDao {
    @Upsert
    suspend fun upsert(entity: ApiUsageLogEntity)

    @Upsert
    suspend fun upsertAll(entities: List<ApiUsageLogEntity>)

    @Query("SELECT * FROM api_usage_logs ORDER BY createdAt DESC")
    fun observeLatest(): Flow<List<ApiUsageLogEntity>>

    @Query("SELECT * FROM api_usage_logs ORDER BY createdAt DESC")
    suspend fun getAll(): List<ApiUsageLogEntity>

    @Query("DELETE FROM api_usage_logs WHERE callId = :callId")
    suspend fun deleteById(callId: String)

    @Query("DELETE FROM api_usage_logs WHERE createdAt < :cutoffAt")
    suspend fun deleteOlderThan(cutoffAt: Long)

    @Query("DELETE FROM api_usage_logs")
    suspend fun clearAll()
}
