package com.wenwentome.reader.bridge.source

import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.PathNotFoundException

class JsonPathRuleExecutor {
    fun select(json: String, expression: String): List<String> =
        selectValues(json, expression)

    fun selectItems(
        target: Any?,
        expression: String,
    ): List<Any?> =
        try {
            normalizeReadResult(read(target, expression))
        } catch (_: PathNotFoundException) {
            emptyList()
        }

    fun selectValues(
        target: Any?,
        expression: String,
    ): List<String> =
        selectItems(target, expression).mapNotNull { item ->
            when (item) {
                null -> null
                is String -> item
                else -> item.toString()
            }?.takeIf { it.isNotBlank() }
        }

    private fun read(
        target: Any?,
        expression: String,
    ): Any? =
        when (target) {
            null -> null
            is String -> JsonPath.parse(target).read<Any?>(expression)
            else -> JsonPath.parse(target).read<Any?>(expression)
        }

    private fun normalizeReadResult(value: Any?): List<Any?> =
        when (value) {
            null -> emptyList()
            is List<*> -> value.toList()
            else -> listOf(value)
        }
}
