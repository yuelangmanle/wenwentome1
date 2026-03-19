package com.wenwentome.reader.bridge.source

import com.wenwentome.reader.bridge.source.model.LegacyRuleDefinition
import com.wenwentome.reader.bridge.source.model.LegacySourceDefinition
import com.wenwentome.reader.bridge.source.model.NormalizedSourceDefinition
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.content
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class SourceRuleParser(
    private val mapper: LegacySourceMapper = LegacySourceMapper(),
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(rawJson: String): NormalizedSourceDefinition =
        mapper.map(parseLegacyDefinition(json.parseToJsonElement(rawJson)))

    fun parseAll(rawJson: String): List<NormalizedSourceDefinition> =
        when (val root = json.parseToJsonElement(rawJson)) {
            is JsonArray -> root.map { mapper.map(parseLegacyDefinition(it)) }
            else -> listOf(mapper.map(parseLegacyDefinition(root)))
        }

    private fun parseLegacyDefinition(element: JsonElement): LegacySourceDefinition {
        val root = element.jsonObject
        return LegacySourceDefinition(
            bookSourceName = root.requiredString("bookSourceName"),
            bookSourceGroup = root.optionalString("bookSourceGroup"),
            bookSourceType = root.optionalString("bookSourceType")?.toIntOrNull() ?: 0,
            bookSourceUrl = root.requiredString("bookSourceUrl"),
            enabled = root.optionalString("enabled")?.toBooleanStrictOrNull() ?: true,
            searchUrl = root.optionalString("searchUrl"),
            ruleSearch = root.ruleDefinition("ruleSearch"),
            ruleBookInfo = root.ruleDefinition("ruleBookInfo"),
            ruleToc = root.ruleDefinition("ruleToc"),
            ruleContent = root.ruleDefinition("ruleContent"),
        )
    }

    private fun JsonObject.ruleDefinition(key: String): LegacyRuleDefinition? =
        this[key]?.jsonObject?.let { rule ->
            LegacyRuleDefinition(
                values = rule.mapValues { (_, value) ->
                    value.jsonPrimitive.contentOrNull
                }
            )
        }

    private fun JsonObject.requiredString(key: String): String =
        getValue(key).jsonPrimitive.content

    private fun JsonObject.optionalString(key: String): String? =
        this[key]?.jsonPrimitive?.contentOrNull
}
