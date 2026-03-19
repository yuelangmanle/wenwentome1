package com.wenwentome.reader.feature.settings

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class ChangelogRepository(
    private val loadJson: suspend () -> String,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    suspend fun getAll(): List<ChangelogEntry> =
        json.parseToJsonElement(loadJson())
            .jsonArray
            .map { element ->
                val root = element.jsonObject
                ChangelogEntry(
                    version = root.requiredString("version"),
                    releaseDate = root.requiredString("releaseDate"),
                    title = root.requiredString("title"),
                    highlights = root.requiredStringList("highlights"),
                    details = root.requiredStringList("details"),
                )
            }
            .sortedByDescending { entry: ChangelogEntry -> versionKey(entry.version) }

    private fun versionKey(version: String): Int {
        val parts = version.split('.')
        val major = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return major * 100 + minor
    }
}

private fun kotlinx.serialization.json.JsonObject.requiredString(key: String): String =
    requireNotNull(getValue(key).jsonPrimitive.contentOrNull) { "$key must be a string" }

private fun kotlinx.serialization.json.JsonObject.requiredStringList(key: String): List<String> =
    getValue(key).jsonArray.map { element ->
        requireNotNull(element.jsonPrimitive.contentOrNull) { "$key entries must be strings" }
    }
