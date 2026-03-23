package com.wenwentome.reader.bridge.source.model

enum class RuleEngine {
    HTML_CSS,
    HTML_XPATH,
    JSON_PATH,
}

enum class ResponseKind {
    HTML,
    JSON,
}

sealed interface TextCleaner {
    data class RemoveRegex(val pattern: String) : TextCleaner

    data class ReplaceRegex(
        val pattern: String,
        val replacement: String,
    ) : TextCleaner
}

enum class UnsupportedRuleKind {
    JS_TEMPLATE,
}

data class RuleExpression(
    val engine: RuleEngine,
    val selector: String,
    val extractor: String? = null,
    val cleaners: List<TextCleaner> = emptyList(),
    val postScript: String? = null,
    val unsupportedKind: UnsupportedRuleKind? = null,
)

data class ParsedSearchUrlTemplate(
    val raw: String,
    val urlTemplate: String,
    val requestConfig: SearchRequestConfig? = null,
    val requiresJsTemplate: Boolean = false,
    val responseKind: ResponseKind = ResponseKind.HTML,
    val unsupportedKind: UnsupportedRuleKind? = null,
)

data class SearchRequestConfig(
    val method: String = "GET",
    val bodyTemplate: String? = null,
    val rawJson: String? = null,
)
