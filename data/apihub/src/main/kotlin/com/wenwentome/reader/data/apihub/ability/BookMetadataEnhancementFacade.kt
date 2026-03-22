package com.wenwentome.reader.data.apihub.ability

import com.wenwentome.reader.core.model.BookRecord
import com.wenwentome.reader.data.apihub.cache.AbilityResultCacheRepository
import com.wenwentome.reader.data.apihub.runtime.ApiAbilityRequest
import com.wenwentome.reader.data.apihub.runtime.ApiDispatchResult
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.security.MessageDigest

data class BookMetadataEnhancementResult(
    val improvedSummary: String? = null,
    val suggestedCoverUri: String? = null,
    val authorIntroduction: String? = null,
    val tags: List<String> = emptyList(),
    val cached: Boolean = false,
)

interface BookMetadataEnhancementFacade {
    suspend fun enhance(book: BookRecord): BookMetadataEnhancementResult
}

class DefaultBookMetadataEnhancementFacade(
    private val invoker: AbilityInvoker,
    private val cacheRepository: AbilityResultCacheRepository,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : BookMetadataEnhancementFacade {
    override suspend fun enhance(book: BookRecord): BookMetadataEnhancementResult {
        val snapshot = listOf(book.title, book.author, book.summary, book.cover).joinToString("|")
        val cacheKey = "book.metadata:${book.id}:${snapshot.sha256()}"
        val lookup =
            cacheRepository.getOrPut(
                cacheKey = cacheKey,
                serializer = CachedMetadataPayload.serializer(),
            ) {
                val body =
                    buildString {
                        appendLine("请补全这本书的元信息，并使用 JSON 返回。")
                        appendLine("""字段: improvedSummary, suggestedCoverUri, authorIntroduction, tags""")
                        appendLine("书名: ${book.title}")
                        appendLine("作者: ${book.author.orEmpty()}")
                        appendLine("简介: ${book.summary.orEmpty()}")
                        appendLine("现有封面: ${book.cover.orEmpty()}")
                    }
                val response =
                    invoker(
                        CAPABILITY_ID,
                        ApiAbilityRequest(
                            body = body,
                            bookId = book.id,
                            estimatedInputTokens = body.length / 2,
                            estimatedOutputTokens = 480,
                        ),
                    )
                response.outputText.toCachedPayload(json)
            }

        return lookup.value.toResult(cached = lookup.hit)
    }

    @Serializable
    private data class CachedMetadataPayload(
        val improvedSummary: String? = null,
        val suggestedCoverUri: String? = null,
        val authorIntroduction: String? = null,
        val tags: List<String> = emptyList(),
    ) {
        fun toResult(cached: Boolean): BookMetadataEnhancementResult =
            BookMetadataEnhancementResult(
                improvedSummary = improvedSummary,
                suggestedCoverUri = suggestedCoverUri,
                authorIntroduction = authorIntroduction,
                tags = tags,
                cached = cached,
            )
    }

    private fun String.toCachedPayload(json: Json): CachedMetadataPayload {
        val trimmed = trim()
        if (trimmed.startsWith("{")) {
            runCatching {
                return json.decodeFromString(CachedMetadataPayload.serializer(), trimmed)
            }
        }
        return CachedMetadataPayload(improvedSummary = trimmed.ifBlank { null })
    }

    private fun String.sha256(): String =
        MessageDigest.getInstance("SHA-256")
            .digest(toByteArray())
            .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }

    private companion object {
        const val CAPABILITY_ID = "book.metadata.enhance"
    }
}
