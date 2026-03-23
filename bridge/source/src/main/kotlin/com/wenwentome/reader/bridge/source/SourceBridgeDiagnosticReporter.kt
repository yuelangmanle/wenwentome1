package com.wenwentome.reader.bridge.source

fun interface SourceBridgeDiagnosticReporter {
    fun report(
        sourceId: String,
        pageUrl: String,
        diagnostics: List<RuleExecutionDiagnostic>,
    )
}

object NoOpSourceBridgeDiagnosticReporter : SourceBridgeDiagnosticReporter {
    override fun report(
        sourceId: String,
        pageUrl: String,
        diagnostics: List<RuleExecutionDiagnostic>,
    ) = Unit
}
