package com.wenwentome.reader.core.model

enum class SourceType { BUILT_IN, IMPORTED, COMPAT }
enum class RuleFormat { LEGACY, LEGADO3, CUSTOM }

data class SourceDefinition(
    val sourceId: String,
    val sourceName: String,
    val sourceType: SourceType,
    val ruleFormat: RuleFormat,
    val sourceUrl: String? = null,
    val rawDefinition: String? = null,
    val authState: String? = null,
    val enabled: Boolean = true,
    val group: String? = null,
)
