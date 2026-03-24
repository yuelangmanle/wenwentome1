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

    @Query("SELECT * FROM source_definitions ORDER BY sourceName")
    fun observeAll(): Flow<List<SourceDefinitionEntity>>

    @Query("SELECT * FROM source_definitions")
    suspend fun getAll(): List<SourceDefinitionEntity>

    @Query("UPDATE source_definitions SET enabled = NOT enabled WHERE sourceId = :sourceId")
    suspend fun toggleEnabled(sourceId: String)

    @Query("DELETE FROM source_definitions WHERE sourceId = :sourceId")
    suspend fun deleteById(sourceId: String)

    @Query("DELETE FROM source_definitions")
    suspend fun clearAll()

    @Transaction
    suspend fun replaceAll(entities: List<SourceDefinitionEntity>) {
        clearAll()
        upsertAll(entities)
    }
}
