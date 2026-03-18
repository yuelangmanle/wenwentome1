package com.wenwentome.reader.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.wenwentome.reader.core.model.RuleFormat
import com.wenwentome.reader.core.model.SourceType

@Entity(tableName = "source_definitions")
data class SourceDefinitionEntity(
    @PrimaryKey val sourceId: String,
    val sourceName: String,
    val sourceType: SourceType,
    val ruleFormat: RuleFormat,
    val authState: String? = null,
    val enabled: Boolean = true,
    val group: String? = null,
)

