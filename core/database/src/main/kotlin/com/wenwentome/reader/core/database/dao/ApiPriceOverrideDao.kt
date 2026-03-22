package com.wenwentome.reader.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.wenwentome.reader.core.database.entity.ApiPriceOverrideEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ApiPriceOverrideDao {
    @Upsert
    suspend fun upsert(entity: ApiPriceOverrideEntity)

    @Upsert
    suspend fun upsertAll(entities: List<ApiPriceOverrideEntity>)

    @Query("SELECT * FROM api_price_overrides WHERE providerId = :providerId AND modelId = :modelId LIMIT 1")
    suspend fun findById(providerId: String, modelId: String): ApiPriceOverrideEntity?

    @Query("SELECT * FROM api_price_overrides WHERE providerId = :providerId ORDER BY modelId")
    fun observeByProviderId(providerId: String): Flow<List<ApiPriceOverrideEntity>>

    @Query("SELECT * FROM api_price_overrides ORDER BY providerId, modelId")
    fun observeAll(): Flow<List<ApiPriceOverrideEntity>>

    @Query("DELETE FROM api_price_overrides WHERE providerId = :providerId AND modelId = :modelId")
    suspend fun deleteById(providerId: String, modelId: String)

    @Query("DELETE FROM api_price_overrides WHERE providerId = :providerId")
    suspend fun deleteByProviderId(providerId: String)

    @Query("DELETE FROM api_price_overrides")
    suspend fun clearAll()
}
