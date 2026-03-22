package com.wenwentome.reader.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.wenwentome.reader.core.database.entity.ApiAbilityCacheEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ApiAbilityCacheDao {
    @Upsert
    suspend fun upsert(entity: ApiAbilityCacheEntity)

    @Query("SELECT * FROM api_ability_cache WHERE cacheKey = :cacheKey LIMIT 1")
    suspend fun findByKey(cacheKey: String): ApiAbilityCacheEntity?

    @Query("SELECT * FROM api_ability_cache ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ApiAbilityCacheEntity>>

    @Query("DELETE FROM api_ability_cache WHERE cacheKey = :cacheKey")
    suspend fun deleteByKey(cacheKey: String)

    @Query("DELETE FROM api_ability_cache WHERE expiresAt <= :expiredAt")
    suspend fun deleteExpired(expiredAt: Long)

    @Query("DELETE FROM api_ability_cache")
    suspend fun clearAll()
}
