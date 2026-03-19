package com.wenwentome.reader.bridge.source.model

data class LegacySourceDefinition(
    val bookSourceName: String,
    val bookSourceGroup: String? = null,
    val bookSourceType: Int = 0,
    val bookSourceUrl: String,
    val enabled: Boolean = true,
    val searchUrl: String? = null,
    val ruleSearch: LegacyRuleDefinition? = null,
    val ruleBookInfo: LegacyRuleDefinition? = null,
    val ruleToc: LegacyRuleDefinition? = null,
    val ruleContent: LegacyRuleDefinition? = null,
)

data class LegacyRuleDefinition(
    val values: Map<String, String?> = emptyMap(),
) {
    operator fun get(key: String): String? = values[key]
}
