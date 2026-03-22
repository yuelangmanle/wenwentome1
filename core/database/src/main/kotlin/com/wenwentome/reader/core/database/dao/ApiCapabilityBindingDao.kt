package com.wenwentome.reader.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.wenwentome.reader.core.database.entity.ApiCapabilityBindingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ApiCapabilityBindingDao {
    @Upsert
    suspend fun upsert(entity: ApiCapabilityBindingEntity)

    @Upsert
    suspend fun upsertAll(entities: List<ApiCapabilityBindingEntity>)

    @Query("SELECT * FROM api_capability_bindings WHERE capabilityId = :capabilityId LIMIT 1")
    fun observeById(capabilityId: String): Flow<ApiCapabilityBindingEntity?>

    @Query("SELECT * FROM api_capability_bindings ORDER BY capabilityId")
    fun observeAll(): Flow<List<ApiCapabilityBindingEntity>>

    @Query("SELECT * FROM api_capability_bindings ORDER BY capabilityId")
    suspend fun getAll(): List<ApiCapabilityBindingEntity>

    @Query("DELETE FROM api_capability_bindings WHERE capabilityId = :capabilityId")
    suspend fun deleteById(capabilityId: String)

    @Query("DELETE FROM api_capability_bindings")
    suspend fun clearAll()

    @Transaction
    suspend fun replaceAll(entities: List<ApiCapabilityBindingEntity>) {
        clearAll()
        upsertAll(entities)
    }
}
