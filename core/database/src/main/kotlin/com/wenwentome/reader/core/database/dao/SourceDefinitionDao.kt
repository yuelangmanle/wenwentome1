package com.wenwentome.reader.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.wenwentome.reader.core.database.entity.SourceDefinitionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SourceDefinitionDao {
    @Upsert
    suspend fun upsert(entity: SourceDefinitionEntity)

    @Upsert
    suspend fun upsertAll(entities: List<SourceDefinitionEntity>)

    @Query("SELECT * FROM source_definitions WHERE sourceId = :sourceId LIMIT 1")
    fun observeById(sourceId: String): Flow<SourceDefinitionEntity?>

    @Query("SELECT * FROM source_definitions")
    fun observeAll(): Flow<List<SourceDefinitionEntity>>

    @Query("DELETE FROM source_definitions")
    suspend fun clearAll()

    @Transaction
    suspend fun replaceAll(entities: List<SourceDefinitionEntity>) {
        clearAll()
        upsertAll(entities)
    }
}

