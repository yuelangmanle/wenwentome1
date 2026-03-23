package com.wenwentome.reader.bridge.source

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class XPathRuleExecutor {
    fun select(html: String, expression: String): List<String> =
        selectValues(html, expression)

    fun selectElements(
        html: String,
        expression: String,
    ): List<Element> =
        Jsoup.parse(html).selectXpath(expression)

    fun selectValues(
        html: String,
        expression: String,
        extractor: String? = null,
    ): List<String> {
        val normalizedExtractor = extractor?.trim().orEmpty().ifBlank { "text" }
        return selectElements(html, expression).mapNotNull { element ->
            when {
                normalizedExtractor == "text" -> element.text()
                normalizedExtractor == "html" -> element.html()
                else -> element.attr(normalizedExtractor.removePrefix("attr:"))
            }.takeIf { it.isNotBlank() }
        }
    }
}
