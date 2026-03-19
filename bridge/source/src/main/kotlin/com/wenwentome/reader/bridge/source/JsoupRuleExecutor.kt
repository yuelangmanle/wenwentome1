package com.wenwentome.reader.bridge.source

import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class JsoupRuleExecutor {
    fun select(document: Document, expression: String): List<String> =
        select(document as Element, expression)

    fun select(element: Element, expression: String): List<String> =
        expression.split("||")
            .map { it.trim() }
            .firstNotNullOfOrNull { candidate ->
                val values = selectSingle(element, candidate)
                values.takeIf { it.isNotEmpty() }
            }
            .orEmpty()

    fun selectElements(element: Element, expression: String): List<Element> {
        val cleaned = cleanExpression(expression)
        val selector = cleaned.substringBefore("@").trim()
        if (selector.isBlank() || selector == "text") {
            return listOf(element)
        }
        val (normalizedSelector, index) = parseSelector(selector)
        val matches = element.select(normalizedSelector)
        return index?.let { matches.getOrNull(it)?.let(::listOf).orEmpty() } ?: matches
    }

    private fun selectSingle(element: Element, expression: String): List<String> {
        val cleaned = cleanExpression(expression)
        val selector = cleaned.substringBefore("@").trim()
        val extractor = cleaned.substringAfter("@", "text").trim()
        val matches = selectElements(element, selector)
        return matches.mapNotNull { match ->
            when {
                extractor.isBlank() || extractor == "text" -> match.text()
                extractor == "html" -> match.html()
                else -> match.attr(extractor.removePrefix("attr:"))
            }.takeIf { it.isNotBlank() }
        }
    }

    private fun cleanExpression(expression: String): String =
        expression.substringBefore("@js:")
            .substringBefore("##")
            .trim()

    private fun parseSelector(selector: String): Pair<String, Int?> {
        val indexedMatch = Regex("^(.*)\\.(\\d+)$").matchEntire(selector)
        return if (indexedMatch != null && !selector.startsWith("//")) {
            indexedMatch.groupValues[1] to indexedMatch.groupValues[2].toInt()
        } else {
            selector to null
        }
    }
}
