package com.wenwentome.reader.bridge.source.model

import org.jsoup.nodes.Element

sealed interface RuleTarget {
    data class Html(
        val element: Element,
    ) : RuleTarget

    data class Json(
        val value: Any?,
    ) : RuleTarget
}
