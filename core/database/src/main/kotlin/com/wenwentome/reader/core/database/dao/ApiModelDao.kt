package com.wenwentome.reader.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.wenwentome.reader.core.database.entity.ApiModelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ApiModelDao {
    @Upsert
    suspend fun upsert(entity: ApiModelEntity)

    @Upsert
    suspend fun upsertAll(entities: List<ApiModelEntity>)

    @Query("SELECT * FROM api_models WHERE providerId = :providerId AND modelId = :modelId LIMIT 1")
    suspend fun findById(providerId: String, modelId: String): ApiModelEntity?

    @Query("SELECT * FROM api_models WHERE providerId = :providerId ORDER BY label")
    fun observeByProviderId(providerId: String): Flow<List<ApiModelEntity>>

    @Query("SELECT * FROM api_models ORDER BY providerId, label")
    fun observeAll(): Flow<List<ApiModelEntity>>

    @Query("SELECT * FROM api_models WHERE providerId = :providerId ORDER BY label")
    suspend fun getByProviderId(providerId: String): List<ApiModelEntity>

    @Query("DELETE FROM api_models WHERE providerId = :providerId AND modelId = :modelId")
    suspend fun deleteById(providerId: String, modelId: String)

    @Query("DELETE FROM api_models WHERE providerId = :providerId")
    suspend fun deleteByProviderId(providerId: String)

    @Query("DELETE FROM api_models")
    suspend fun clearAll()

    @Transaction
    suspend fun replaceForProvider(providerId: String, entities: List<ApiModelEntity>) {
        deleteByProviderId(providerId)
        upsertAll(entities)
    }
}
