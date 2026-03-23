package com.wenwentome.reader.bridge.source

import com.wenwentome.reader.bridge.source.model.RuleEngine
import com.wenwentome.reader.bridge.source.model.RuleExpression
import com.wenwentome.reader.bridge.source.model.RuleTarget
import com.wenwentome.reader.bridge.source.model.TextCleaner
import com.wenwentome.reader.bridge.source.model.UnsupportedRuleKind

data class RuleExecutionContext(
    val baseUrl: String,
    val pageUrl: String,
)

data class RuleExecutionDiagnostic(
    val code: SourceBridgeErrorCode,
    val detail: String? = null,
)

data class RuleExecutionOutcome<T>(
    val value: T,
    val diagnostics: List<RuleExecutionDiagnostic> = emptyList(),
)

fun <T> RuleExecutionOutcome<T>.captureInto(
    diagnostics: MutableList<RuleExecutionDiagnostic>,
): T {
    diagnostics += this.diagnostics
    return value
}

class RuleExecutionEngine(
    private val jsoupRuleExecutor: JsoupRuleExecutor = JsoupRuleExecutor(),
    private val xpathRuleExecutor: XPathRuleExecutor = XPathRuleExecutor(),
    private val jsonPathRuleExecutor: JsonPathRuleExecutor = JsonPathRuleExecutor(),
    private val rhinoScriptEvaluator: RhinoScriptEvaluator = RhinoScriptEvaluator(),
) {
    fun assertSupported(unsupportedKind: UnsupportedRuleKind?) {
        unsupportedKind?.let {
            throw SourceBridgeException(
                code = SourceBridgeErrorCode.UNSUPPORTED_RULE_KIND,
                message = "Unsupported rule kind: ${it.name}",
            )
        }
    }

    fun extractList(
        target: RuleTarget,
        expression: RuleExpression,
        context: RuleExecutionContext,
    ): RuleExecutionOutcome<List<RuleTarget>> {
        assertSupported(expression.unsupportedKind)
        val value = when (expression.engine) {
            RuleEngine.HTML_CSS -> extractHtmlList(target, expression.selector)
            RuleEngine.HTML_XPATH -> extractXPathList(target, expression.selector)
            RuleEngine.JSON_PATH -> extractJsonList(target, expression.selector)
        }
        return RuleExecutionOutcome(value = value)
    }

    fun extractValue(
        target: RuleTarget,
        expression: RuleExpression,
        context: RuleExecutionContext,
    ): RuleExecutionOutcome<String?> {
        assertSupported(expression.unsupportedKind)
        val rawValue = when (expression.engine) {
            RuleEngine.HTML_CSS -> extractHtmlValue(target, expression)
            RuleEngine.HTML_XPATH -> extractXPathValue(target, expression)
            RuleEngine.JSON_PATH -> extractJsonValue(target, expression)
        } ?: return RuleExecutionOutcome(value = null)
        return postProcess(rawValue, expression, context)
    }

    private fun extractHtmlList(
        target: RuleTarget,
        selector: String,
    ): List<RuleTarget> =
        (target as? RuleTarget.Html)
            ?.let { htmlTarget ->
                jsoupRuleExecutor.selectElementsBySelector(htmlTarget.element, selector)
                    .map(RuleTarget::Html)
            }
            .orEmpty()

    private fun extractXPathList(
        target: RuleTarget,
        expression: String,
    ): List<RuleTarget> =
        (target as? RuleTarget.Html)
            ?.let { htmlTarget ->
                xpathRuleExecutor.selectElements(htmlTarget.element.outerHtml(), expression)
                    .map(RuleTarget::Html)
            }
            .orEmpty()

    private fun extractJsonList(
        target: RuleTarget,
        expression: String,
    ): List<RuleTarget> =
        (target as? RuleTarget.Json)
            ?.let { jsonTarget ->
                jsonPathRuleExecutor.selectItems(jsonTarget.value, expression)
                    .map(RuleTarget::Json)
            }
            .orEmpty()

    private fun extractHtmlValue(
        target: RuleTarget,
        expression: RuleExpression,
    ): String? =
        (target as? RuleTarget.Html)
            ?.let { htmlTarget ->
                jsoupRuleExecutor.selectValues(
                    element = htmlTarget.element,
                    selector = expression.selector,
                    extractor = expression.extractor,
                ).firstOrNull()
            }

    private fun extractXPathValue(
        target: RuleTarget,
        expression: RuleExpression,
    ): String? =
        (target as? RuleTarget.Html)
            ?.let { htmlTarget ->
                xpathRuleExecutor.selectValues(
                    html = htmlTarget.element.outerHtml(),
                    expression = expression.selector,
                    extractor = expression.extractor,
                ).firstOrNull()
            }

    private fun extractJsonValue(
        target: RuleTarget,
        expression: RuleExpression,
    ): String? =
        (target as? RuleTarget.Json)
            ?.let { jsonTarget ->
                jsonPathRuleExecutor.selectValues(
                    target = jsonTarget.value,
                    expression = expression.selector,
                ).firstOrNull()
            }

    private fun postProcess(
        raw: String,
        expression: RuleExpression,
        context: RuleExecutionContext,
    ): RuleExecutionOutcome<String?> {
        val cleaned = applyCleaners(raw, expression.cleaners)
        val diagnostics = mutableListOf<RuleExecutionDiagnostic>()
        val scripted = expression.postScript?.let { script ->
            runCatching {
                rhinoScriptEvaluator.eval(
                    script = script,
                    bindings = mapOf(
                        "result" to cleaned,
                        "baseUrl" to context.baseUrl,
                        "pageUrl" to context.pageUrl,
                    ),
                )
            }.getOrElse {
                diagnostics += RuleExecutionDiagnostic(
                    code = SourceBridgeErrorCode.SCRIPT_POST_PROCESS_FAILED,
                    detail = it.message,
                )
                null
            }?.takeUnless { it.isBlank() || it == "null" || it == "undefined" }
        } ?: cleaned
        return RuleExecutionOutcome(
            value = scripted?.trim()?.takeIf(String::isNotBlank),
            diagnostics = diagnostics,
        )
    }

    private fun applyCleaners(
        raw: String,
        cleaners: List<TextCleaner>,
    ): String =
        cleaners.fold(raw) { current, cleaner ->
            when (cleaner) {
                is TextCleaner.RemoveRegex -> current.replace(Regex(cleaner.pattern), "")
                is TextCleaner.ReplaceRegex -> current.replace(Regex(cleaner.pattern), cleaner.replacement)
            }
        }
}
