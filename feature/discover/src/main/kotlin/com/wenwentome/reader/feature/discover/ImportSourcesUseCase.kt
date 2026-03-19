package com.wenwentome.reader.feature.discover

import com.wenwentome.reader.bridge.source.SourceRuleParser
import com.wenwentome.reader.core.database.dao.SourceDefinitionDao
import com.wenwentome.reader.core.database.toEntity
import com.wenwentome.reader.core.model.RuleFormat
import com.wenwentome.reader.core.model.SourceDefinition
import com.wenwentome.reader.core.model.SourceType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement

class ImportSourcesUseCase(
    private val sourceRuleParser: SourceRuleParser,
    private val sourceDefinitionDao: SourceDefinitionDao,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend operator fun invoke(rawJsonArray: String) {
        elements(rawJsonArray).forEach { element ->
            val raw = element.toString()
            val parsed = sourceRuleParser.parse(raw)
            sourceDefinitionDao.upsert(
                SourceDefinition(
                    sourceId = parsed.id,
                    sourceName = parsed.name,
                    sourceType = SourceType.IMPORTED,
                    ruleFormat = RuleFormat.LEGACY,
                    sourceUrl = parsed.baseUrl,
                    rawDefinition = raw,
                    enabled = parsed.enabled,
                    group = parsed.group,
                ).toEntity()
            )
        }
    }

    private fun elements(rawJson: String): List<JsonElement> =
        when (val root = json.parseToJsonElement(rawJson)) {
            is JsonArray -> root
            else -> listOf(root)
        }
}
