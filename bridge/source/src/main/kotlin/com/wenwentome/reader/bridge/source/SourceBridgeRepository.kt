package com.wenwentome.reader.bridge.source

import com.wenwentome.reader.bridge.source.model.NormalizedSourceDefinition
import com.wenwentome.reader.bridge.source.model.ParsedSearchUrlTemplate
import com.wenwentome.reader.bridge.source.model.ResponseKind
import com.wenwentome.reader.bridge.source.model.RemoteBookDetail
import com.wenwentome.reader.bridge.source.model.RemoteChapter
import com.wenwentome.reader.bridge.source.model.RemoteChapterContent
import com.wenwentome.reader.bridge.source.model.RemoteSearchResult
import com.wenwentome.reader.bridge.source.model.RuleEngine
import com.wenwentome.reader.bridge.source.model.RuleExpression
import com.wenwentome.reader.bridge.source.model.TextCleaner
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URI
import java.net.URLEncoder

interface SourceDefinitionProvider {
    suspend fun getAll(): List<NormalizedSourceDefinition>
    suspend fun getById(sourceId: String): NormalizedSourceDefinition? =
        getAll().firstOrNull { it.id == sourceId }
}

class StaticSourceDefinitionProvider(
    private val definitions: List<NormalizedSourceDefinition>,
) : SourceDefinitionProvider {
    override suspend fun getAll(): List<NormalizedSourceDefinition> = definitions

    companion object {
        fun fromRawJson(rawJson: String, parser: SourceRuleParser = SourceRuleParser()): StaticSourceDefinitionProvider =
            StaticSourceDefinitionProvider(parser.parseAll(rawJson))
    }
}

interface SourceBridgeRepository {
    suspend fun search(query: String, sourceIds: List<String> = emptyList()): List<RemoteSearchResult>
    suspend fun fetchBookDetail(sourceId: String, remoteBookId: String): RemoteBookDetail
    suspend fun fetchToc(sourceId: String, remoteBookId: String): List<RemoteChapter>
    suspend fun fetchChapterContent(sourceId: String, chapterRef: String): RemoteChapterContent
}

class RealSourceBridgeRepository(
    private val sourceProvider: SourceDefinitionProvider,
    private val httpClient: OkHttpClient = OkHttpClient(),
    private val jsoupRuleExecutor: JsoupRuleExecutor = JsoupRuleExecutor(),
) : SourceBridgeRepository {
    override suspend fun search(query: String, sourceIds: List<String>): List<RemoteSearchResult> {
        val sources = sourceProvider.getAll()
            .filter { it.enabled && (sourceIds.isEmpty() || it.id in sourceIds) }
        return sources.flatMap { source ->
            runCatching {
                val body = executeTemplateRequest(source, requireNotNull(source.searchUrlTemplate), query)
                val document = Jsoup.parse(body, source.baseUrl)
                executeSearch(source, document)
            }.getOrDefault(emptyList())
        }
    }

    override suspend fun fetchBookDetail(sourceId: String, remoteBookId: String): RemoteBookDetail {
        val source = requireSource(sourceId)
        val detailUrl = resolveUrl(source.baseUrl, remoteBookId)
        val document = executeDocument(detailUrl)
        return executeDetail(source, document)
    }

    override suspend fun fetchToc(sourceId: String, remoteBookId: String): List<RemoteChapter> {
        val source = requireSource(sourceId)
        val tocUrl = resolveUrl(source.baseUrl, remoteBookId)
        val document = executeDocument(tocUrl)
        return executeToc(source, document)
    }

    override suspend fun fetchChapterContent(sourceId: String, chapterRef: String): RemoteChapterContent {
        val source = requireSource(sourceId)
        val document = executeDocument(resolveUrl(source.baseUrl, chapterRef))
        return executeContent(source, document, chapterRef)
    }

    private suspend fun requireSource(sourceId: String): NormalizedSourceDefinition =
        requireNotNull(sourceProvider.getById(sourceId)) { "Source $sourceId not found" }

    private suspend fun executeDocument(url: String): Document =
        Jsoup.parse(executeRequest(Request.Builder().url(url).get().build()), url)

    private suspend fun executeTemplateRequest(
        source: NormalizedSourceDefinition,
        template: ParsedSearchUrlTemplate,
        query: String,
        page: Int = 1,
    ): String {
        require(!template.requiresJsTemplate) {
            "JS templates are not supported in the core bridge yet"
        }
        val resolvedUrl = resolveUrl(
            source.baseUrl,
            interpolate(template.urlTemplate, query, page),
        )
        val request = template.requestConfig?.let { config ->
            val method = config.method.uppercase()
            if (method == "POST") {
                val body = interpolate(config.bodyTemplate.orEmpty(), query, page)
                Request.Builder()
                    .url(resolvedUrl)
                    .post(body.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
                    .build()
            } else {
                Request.Builder().url(resolvedUrl).get().build()
            }
        } ?: Request.Builder().url(resolvedUrl).get().build()
        return executeRequest(request)
    }

    private suspend fun executeSearch(
        source: NormalizedSourceDefinition,
        document: Document,
    ): List<RemoteSearchResult> {
        val rule = source.searchRule ?: return emptyList()
        if (rule.responseKind != ResponseKind.HTML || rule.bookList.engine != RuleEngine.HTML_CSS) {
            return emptyList()
        }
        return jsoupRuleExecutor.selectElements(document, rule.bookList.toLegacyExpression()).mapNotNull { item ->
            val detailUrl = item.extract(rule.bookUrl) ?: return@mapNotNull null
            val title = item.extract(rule.name) ?: return@mapNotNull null
            RemoteSearchResult(
                id = resolveUrl(source.baseUrl, detailUrl),
                sourceId = source.id,
                title = title,
                author = item.extract(rule.author),
                detailUrl = resolveUrl(source.baseUrl, detailUrl),
            )
        }
    }

    private fun executeDetail(source: NormalizedSourceDefinition, document: Document): RemoteBookDetail {
        val rule = source.bookInfoRule ?: return RemoteBookDetail(title = document.title())
        return RemoteBookDetail(
            title = document.extract(rule.name) ?: document.title(),
            author = document.extract(rule.author),
            summary = document.extract(rule.intro),
            coverUrl = document.extract(rule.coverUrl)?.let { resolveUrl(source.baseUrl, it) },
            lastChapter = document.extract(rule.lastChapter),
        )
    }

    private fun executeToc(source: NormalizedSourceDefinition, document: Document): List<RemoteChapter> {
        val rule = source.tocRule ?: return emptyList()
        if (rule.responseKind != ResponseKind.HTML || rule.chapterList.engine != RuleEngine.HTML_CSS) {
            return emptyList()
        }
        return jsoupRuleExecutor.selectElements(document, rule.chapterList.toLegacyExpression()).mapNotNull { element ->
            val title = element.extract(rule.chapterName) ?: return@mapNotNull null
            val url = element.extract(rule.chapterUrl) ?: return@mapNotNull null
            RemoteChapter(
                chapterRef = resolveUrl(source.baseUrl, url),
                title = title,
            )
        }
    }

    private fun executeContent(
        source: NormalizedSourceDefinition,
        document: Document,
        chapterRef: String,
    ): RemoteChapterContent {
        val rule = source.contentRule
        val rawContent = document.extract(rule?.content) ?: document.body().text()
        val content = rule?.let { applyCleaners(rawContent, it.content.cleaners) } ?: rawContent
        return RemoteChapterContent(
            chapterRef = chapterRef,
            title = document.title().ifBlank { chapterRef },
            content = content,
        )
    }

    private suspend fun executeRequest(request: Request): String =
        httpClient.newCall(request).execute().use { response ->
            require(response.isSuccessful) {
                "Source request failed: ${response.code}"
            }
            requireNotNull(response.body).string()
        }

    private fun interpolate(template: String, query: String, page: Int): String =
        template
            .replace("{{key}}", urlEncode(query))
            .replace("{{page}}", page.toString())

    private fun resolveUrl(baseUrl: String, value: String): String =
        try {
            URI(baseUrl).resolve(value).toString()
        } catch (_: IllegalArgumentException) {
            value
        }

    private fun urlEncode(value: String): String =
        URLEncoder.encode(value, Charsets.UTF_8.name())

    private fun Document.extract(expression: RuleExpression?): String? =
        expression
            ?.takeIf { it.engine == RuleEngine.HTML_CSS }
            ?.let { jsoupRuleExecutor.select(this, it.toLegacyExpression()).firstOrNull() }

    private fun org.jsoup.nodes.Element.extract(expression: RuleExpression?): String? =
        expression
            ?.takeIf { it.engine == RuleEngine.HTML_CSS }
            ?.let { jsoupRuleExecutor.select(this, it.toLegacyExpression()).firstOrNull() }

    private fun RuleExpression.toLegacyExpression(): String =
        if (extractor.isNullOrBlank()) selector else "$selector@$extractor"

    private fun applyCleaners(raw: String, cleaners: List<TextCleaner>): String =
        cleaners.fold(raw) { current, cleaner ->
            when (cleaner) {
                is TextCleaner.RemoveRegex -> current.replace(Regex(cleaner.pattern), "")
                is TextCleaner.ReplaceRegex -> current.replace(Regex(cleaner.pattern), cleaner.replacement)
            }
        }
}
