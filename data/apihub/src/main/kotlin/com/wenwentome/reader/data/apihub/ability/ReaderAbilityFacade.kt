package com.wenwentome.reader.data.apihub.ability

import com.wenwentome.reader.data.apihub.cache.AbilityResultCacheRepository
import com.wenwentome.reader.data.apihub.runtime.ApiAbilityRequest
import com.wenwentome.reader.data.apihub.runtime.ApiDispatchResult
import kotlinx.serialization.Serializable

typealias AbilityInvoker = suspend (String, ApiAbilityRequest) -> ApiDispatchResult

data class ReaderAbilityInput(
    val bookId: String,
    val chapterRef: String?,
    val chapterTitle: String?,
    val paragraphs: List<String>,
    val selectedParagraph: String? = paragraphs.firstOrNull(),
    val targetLanguage: String = "现代白话文",
)

data class ReaderAbilityResult(
    val capabilityId: String,
    val text: String,
    val cached: Boolean,
    val providerId: String? = null,
    val modelId: String? = null,
    val estimatedCostMicros: Long? = null,
    val usedFallback: Boolean = false,
)

interface ReaderAbilityFacade {
    suspend fun summarizeChapter(input: ReaderAbilityInput): ReaderAbilityResult

    suspend fun explainParagraph(input: ReaderAbilityInput): ReaderAbilityResult

    suspend fun translateParagraph(input: ReaderAbilityInput): ReaderAbilityResult

    suspend fun speakChapter(input: ReaderAbilityInput): ReaderAbilityResult
}

class DefaultReaderAbilityFacade(
    private val invoker: AbilityInvoker,
    private val cacheRepository: AbilityResultCacheRepository,
) : ReaderAbilityFacade {
    override suspend fun summarizeChapter(input: ReaderAbilityInput): ReaderAbilityResult =
        invokeCached(
            capabilityId = CAPABILITY_SUMMARY,
            cacheKey = "reader.summary:${input.bookId}:${input.chapterRef.orEmpty()}",
            body =
                buildString {
                    appendLine("请为以下章节生成一段简洁总结。")
                    appendLine("书籍 ID: ${input.bookId}")
                    appendLine("章节标题: ${input.chapterTitle.orEmpty()}")
                    appendLine("章节正文:")
                    appendLine(input.paragraphs.joinToString("\n").take(MAX_BODY_CHARS))
                },
            bookId = input.bookId,
            chapterRef = input.chapterRef,
        )

    override suspend fun explainParagraph(input: ReaderAbilityInput): ReaderAbilityResult =
        invokeCached(
            capabilityId = CAPABILITY_EXPLAIN,
            cacheKey = "reader.explain:${input.bookId}:${input.chapterRef.orEmpty()}:${input.selectedParagraph.orEmpty().hashCode()}",
            body =
                buildString {
                    appendLine("请解释下面这段文字的含义，并指出关键意象。")
                    appendLine("章节标题: ${input.chapterTitle.orEmpty()}")
                    appendLine("段落:")
                    appendLine(input.selectedParagraph.orEmpty())
                },
            bookId = input.bookId,
            chapterRef = input.chapterRef,
        )

    override suspend fun translateParagraph(input: ReaderAbilityInput): ReaderAbilityResult =
        invokeCached(
            capabilityId = CAPABILITY_TRANSLATE,
            cacheKey =
                "reader.translate:${input.bookId}:${input.chapterRef.orEmpty()}:${input.targetLanguage}:${input.selectedParagraph.orEmpty().hashCode()}",
            body =
                buildString {
                    appendLine("请将下面这段文字翻译成${input.targetLanguage}。")
                    appendLine(input.selectedParagraph.orEmpty())
                },
            bookId = input.bookId,
            chapterRef = input.chapterRef,
        )

    override suspend fun speakChapter(input: ReaderAbilityInput): ReaderAbilityResult =
        invokeCached(
            capabilityId = CAPABILITY_TTS,
            cacheKey = "reader.tts:${input.bookId}:${input.chapterRef.orEmpty()}",
            body =
                buildString {
                    appendLine("请将以下章节整理成适合 TTS 朗读的顺滑文稿。")
                    appendLine("章节标题: ${input.chapterTitle.orEmpty()}")
                    appendLine(input.paragraphs.joinToString("\n").take(MAX_BODY_CHARS))
                },
            bookId = input.bookId,
            chapterRef = input.chapterRef,
        )

    private suspend fun invokeCached(
        capabilityId: String,
        cacheKey: String,
        body: String,
        bookId: String,
        chapterRef: String?,
    ): ReaderAbilityResult {
        val lookup =
            cacheRepository.getOrPut(
                cacheKey = cacheKey,
                serializer = CachedReaderAbilityPayload.serializer(),
            ) {
                val response =
                    invoker(
                        capabilityId,
                        ApiAbilityRequest(
                            body = body,
                            bookId = bookId,
                            chapterRef = chapterRef,
                            estimatedInputTokens = body.length / 2,
                            estimatedOutputTokens = 320,
                        ),
                    )
                CachedReaderAbilityPayload(
                    capabilityId = capabilityId,
                    text = response.outputText,
                    providerId = response.providerId,
                    modelId = response.modelId,
                    estimatedCostMicros = response.estimatedCostMicros,
                    usedFallback = response.usedFallback,
                )
            }

        return lookup.value.toResult(cached = lookup.hit)
    }

    @Serializable
    private data class CachedReaderAbilityPayload(
        val capabilityId: String,
        val text: String,
        val providerId: String? = null,
        val modelId: String? = null,
        val estimatedCostMicros: Long? = null,
        val usedFallback: Boolean = false,
    ) {
        fun toResult(cached: Boolean): ReaderAbilityResult =
            ReaderAbilityResult(
                capabilityId = capabilityId,
                text = text,
                cached = cached,
                providerId = providerId,
                modelId = modelId,
                estimatedCostMicros = estimatedCostMicros,
                usedFallback = usedFallback,
            )
    }

    private companion object {
        const val CAPABILITY_SUMMARY = "reader.summary"
        const val CAPABILITY_EXPLAIN = "reader.explain"
        const val CAPABILITY_TRANSLATE = "reader.translate"
        const val CAPABILITY_TTS = "reader.tts"
        const val MAX_BODY_CHARS = 2_000
    }
}
