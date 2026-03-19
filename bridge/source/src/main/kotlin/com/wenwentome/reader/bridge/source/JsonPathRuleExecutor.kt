package com.wenwentome.reader.bridge.source

import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.PathNotFoundException

class JsonPathRuleExecutor {
    fun select(json: String, expression: String): List<String> =
        try {
            JsonPath.read<List<Any?>>(json, expression).mapNotNull { it?.toString() }
        } catch (_: PathNotFoundException) {
            emptyList()
        }
}
