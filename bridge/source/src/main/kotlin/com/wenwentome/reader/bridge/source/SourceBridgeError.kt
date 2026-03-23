package com.wenwentome.reader.bridge.source

enum class SourceBridgeErrorCode {
    REQUEST_FAILED,
    RULE_EMPTY_RESULT,
    UNSUPPORTED_RULE_KIND,
    SCRIPT_POST_PROCESS_FAILED,
    INVALID_SOURCE_DEFINITION,
}

class SourceBridgeException(
    val code: SourceBridgeErrorCode,
    override val message: String,
    cause: Throwable? = null,
) : IllegalStateException(message, cause)

fun Throwable.sourceBridgeCodeOrNull(): SourceBridgeErrorCode? =
    (this as? SourceBridgeException)?.code
