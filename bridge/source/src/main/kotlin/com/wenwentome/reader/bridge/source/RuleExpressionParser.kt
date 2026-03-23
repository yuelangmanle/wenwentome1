package com.wenwentome.reader.bridge.source

import com.wenwentome.reader.bridge.source.model.ParsedSearchUrlTemplate
import com.wenwentome.reader.bridge.source.model.ResponseKind
import com.wenwentome.reader.bridge.source.model.RuleEngine
import com.wenwentome.reader.bridge.source.model.RuleExpression
import com.wenwentome.reader.bridge.source.model.SearchRequestConfig
import com.wenwentome.reader.bridge.source.model.TextCleaner
import com.wenwentome.reader.bridge.source.model.UnsupportedRuleKind
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class RuleExpressionParser(
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    fun parse(raw: String): RuleExpression {
        val normalizedRaw = raw.trim()
        val (expressionWithCleaners, postScript) = splitPostScript(normalizedRaw)
        val fragments = expressionWithCleaners.split("##")
        val expressionPart = fragments.firstOrNull().orEmpty().trim()
        val isJsTemplate = expressionPart.startsWith("<js>")
        val unsupportedKind = if (isJsTemplate) UnsupportedRuleKind.JS_TEMPLATE else null
        val normalizedExpression = expressionPart.removePrefix("<js>").trim()
        val engine = if (isJsTemplate) RuleEngine.HTML_CSS else detectEngine(normalizedExpression)
        val (selector, extractor) = if (isJsTemplate) {
            normalizedExpression to null
        } else {
            splitSelectorAndExtractor(normalizedExpression, engine)
        }
        return RuleExpression(
            engine = engine,
            selector = selector,
            extractor = extractor,
            cleaners = parseCleaners(fragments.drop(1)),
            postScript = postScript,
            unsupportedKind = unsupportedKind,
        )
    }

    fun parseSearchTemplate(raw: String?): ParsedSearchUrlTemplate? {
        val normalized = raw?.trim()?.takeIf { it.isNotBlank() } ?: return null
        if (normalized.startsWith("<js>")) {
            return ParsedSearchUrlTemplate(
                raw = normalized,
                urlTemplate = normalized.removePrefix("<js>").trim(),
                requiresJsTemplate = true,
                responseKind = ResponseKind.HTML,
                unsupportedKind = UnsupportedRuleKind.JS_TEMPLATE,
            )
        }
        val urlTemplate = normalized.substringBefore(",{").trim()
        val configRaw = normalized
            .substringAfter(urlTemplate, "")
            .removePrefix(",")
            .trim()
            .takeIf { it.startsWith("{") && it.endsWith("}") }
        return ParsedSearchUrlTemplate(
            raw = normalized,
            urlTemplate = urlTemplate,
            requestConfig = configRaw?.let(::parseRequestConfig),
            requiresJsTemplate = false,
            responseKind = ResponseKind.HTML,
        )
    }

    private fun splitPostScript(raw: String): Pair<String, String?> {
        val markerIndex = raw.indexOf("@js:")
        if (markerIndex < 0) {
            return raw to null
        }
        val expression = raw.substring(0, markerIndex).trim()
        val postScript = raw.substring(markerIndex + "@js:".length).trim().ifBlank { null }
        return expression to postScript
    }

    private fun detectEngine(expression: String): RuleEngine =
        when {
            expression.startsWith("$") -> RuleEngine.JSON_PATH
            expression.startsWith("//") -> RuleEngine.HTML_XPATH
            else -> RuleEngine.HTML_CSS
        }

    private fun splitSelectorAndExtractor(
        expression: String,
        engine: RuleEngine,
    ): Pair<String, String?> {
        if (engine != RuleEngine.HTML_CSS) {
            return expression to null
        }
        val extractorIndex = expression.lastIndexOf('@')
        if (extractorIndex <= 0 || extractorIndex == expression.lastIndex) {
            return expression to null
        }
        val selector = expression.substring(0, extractorIndex).trim()
        val extractor = expression.substring(extractorIndex + 1).trim().ifBlank { null }
        return selector to extractor
    }

    private fun parseCleaners(parts: List<String>): List<TextCleaner> {
        val tokens = parts.mapNotNull { it.trim().takeIf(String::isNotEmpty) }
        if (tokens.isEmpty()) {
            return emptyList()
        }
        if (tokens.size == 1) {
            return listOf(TextCleaner.RemoveRegex(tokens.first()))
        }
        if (tokens.size == 2) {
            return listOf(TextCleaner.ReplaceRegex(tokens[0], tokens[1]))
        }
        val cleaners = mutableListOf<TextCleaner>()
        var index = 0
        while (index < tokens.size) {
            val remaining = tokens.size - index
            if (remaining == 1) {
                cleaners += TextCleaner.RemoveRegex(tokens[index])
                index += 1
            } else {
                cleaners += TextCleaner.ReplaceRegex(tokens[index], tokens[index + 1])
                index += 2
            }
        }
        return cleaners
    }

    private fun parseRequestConfig(rawJson: String): SearchRequestConfig {
        val parsed = runCatching { json.parseToJsonElement(rawJson).jsonObject }.getOrNull()
        if (parsed == null) {
            return SearchRequestConfig(rawJson = rawJson)
        }
        val method = parsed["method"]?.jsonPrimitive?.contentOrNull?.uppercase().orEmpty().ifBlank { "GET" }
        val bodyTemplate = parsed["body"]?.jsonPrimitive?.contentOrNull
        return SearchRequestConfig(
            method = method,
            bodyTemplate = bodyTemplate,
            rawJson = rawJson,
        )
    }
}
