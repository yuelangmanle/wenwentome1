package com.wenwentome.reader.bridge.source

import org.jsoup.Jsoup

class XPathRuleExecutor {
    fun select(html: String, expression: String): List<String> =
        Jsoup.parse(html).selectXpath(expression).map { it.text() }
}
