package com.wenwentome.reader.bridge.source

import com.wenwentome.reader.bridge.source.model.NormalizedSourceDefinition
import com.wenwentome.reader.bridge.source.model.ParsedSearchUrlTemplate
import com.wenwentome.reader.bridge.source.model.ResponseKind
import com.wenwentome.reader.bridge.source.model.RemoteBookDetail
import com.wenwentome.reader.bridge.source.model.RemoteChapter
import com.wenwentome.reader.bridge.source.model.RemoteChapterContent
import com.wenwentome.reader.bridge.source.model.RemoteSearchResult
import com.wenwentome.reader.bridge.source.model.RuleTarget
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.jsoup.Jsoup
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
    private val ruleExecutionEngine: RuleExecutionEngine = RuleExecutionEngine(),
    private val diagnosticReporter: SourceBridgeDiagnosticReporter = NoOpSourceBridgeDiagnosticReporter,
) : SourceBridgeRepository {
    override suspend fun search(query: String, sourceIds: List<String>): List<RemoteSearchResult> {
        val sources = sourceProvider.getAll()
            .filter { it.enabled && (sourceIds.isEmpty() || it.id in sourceIds) }
        return sources.flatMap { source ->
            runCatching {
                val response = executeTemplateRequest(source, requireNotNull(source.searchUrlTemplate), query)
                executeSearch(source, response.body, response.url)
            }.getOrDefault(emptyList())
        }
    }

    override suspend fun fetchBookDetail(sourceId: String, remoteBookId: String): RemoteBookDetail {
        val source = requireSource(sourceId)
        val detailUrl = resolveUrl(source.baseUrl, remoteBookId)
        val body = executeRequest(Request.Builder().url(detailUrl).get().build())
        return executeDetail(source, body, detailUrl)
    }

    override suspend fun fetchToc(sourceId: String, remoteBookId: String): List<RemoteChapter> {
        val source = requireSource(sourceId)
        val tocUrl = resolveUrl(source.baseUrl, remoteBookId)
        val body = executeRequest(Request.Builder().url(tocUrl).get().build())
        return executeToc(source, body, tocUrl)
    }

    override suspend fun fetchChapterContent(sourceId: String, chapterRef: String): RemoteChapterContent {
        val source = requireSource(sourceId)
        val contentUrl = resolveUrl(source.baseUrl, chapterRef)
        val body = executeRequest(Request.Builder().url(contentUrl).get().build())
        return executeContent(source, body, contentUrl)
    }

    private suspend fun requireSource(sourceId: String): NormalizedSourceDefinition =
        requireNotNull(sourceProvider.getById(sourceId)) { "Source $sourceId not found" }

    private suspend fun executeTemplateRequest(
        source: NormalizedSourceDefinition,
        template: ParsedSearchUrlTemplate,
        query: String,
        page: Int = 1,
    ): RequestExecutionResult {
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
        return RequestExecutionResult(
            url = resolvedUrl,
            body = executeRequest(request),
        )
    }

    private suspend fun executeSearch(
        source: NormalizedSourceDefinition,
        body: String,
        pageUrl: String,
    ): List<RemoteSearchResult> {
        val rule = source.searchRule ?: return emptyList()
        val diagnostics = mutableListOf<RuleExecutionDiagnostic>()
        val context = RuleExecutionContext(
            baseUrl = source.baseUrl,
            pageUrl = pageUrl,
        )
        val target = rule.responseKind.toRuleTarget(body, pageUrl)
        val items = ruleExecutionEngine.extractList(target, rule.bookList, context).captureInto(diagnostics)
        val results = items.mapNotNull { item ->
            val detailUrl = ruleExecutionEngine.extractValue(item, rule.bookUrl, context).captureInto(diagnostics)
                ?: return@mapNotNull null
            val title = ruleExecutionEngine.extractValue(item, rule.name, context).captureInto(diagnostics)
                ?: return@mapNotNull null
            val resolvedDetailUrl = resolveUrl(source.baseUrl, detailUrl)
            RemoteSearchResult(
                id = resolvedDetailUrl,
                sourceId = source.id,
                title = title,
                author = rule.author?.let { ruleExecutionEngine.extractValue(item, it, context).captureInto(diagnostics) },
                detailUrl = resolvedDetailUrl,
                coverUrl = rule.coverUrl
                    ?.let { ruleExecutionEngine.extractValue(item, it, context).captureInto(diagnostics) }
                    ?.let { resolveUrl(source.baseUrl, it) },
                intro = rule.intro?.let { ruleExecutionEngine.extractValue(item, it, context).captureInto(diagnostics) },
            )
        }
        reportDiagnostics(source.id, pageUrl, diagnostics)
        return results
    }

    private fun executeDetail(
        source: NormalizedSourceDefinition,
        body: String,
        pageUrl: String,
    ): RemoteBookDetail {
        val rule = source.bookInfoRule ?: return RemoteBookDetail(title = pageTitle(body, pageUrl))
        val diagnostics = mutableListOf<RuleExecutionDiagnostic>()
        val context = RuleExecutionContext(
            baseUrl = source.baseUrl,
            pageUrl = pageUrl,
        )
        val target = rule.responseKind.toRuleTarget(body, pageUrl)
        val detail = RemoteBookDetail(
            title = rule.name?.let { ruleExecutionEngine.extractValue(target, it, context).captureInto(diagnostics) }
                ?: pageTitle(body, pageUrl),
            author = rule.author?.let { ruleExecutionEngine.extractValue(target, it, context).captureInto(diagnostics) },
            summary = rule.intro?.let { ruleExecutionEngine.extractValue(target, it, context).captureInto(diagnostics) },
            coverUrl = rule.coverUrl
                ?.let { ruleExecutionEngine.extractValue(target, it, context).captureInto(diagnostics) }
                ?.let { resolveUrl(source.baseUrl, it) },
            lastChapter = rule.lastChapter?.let {
                ruleExecutionEngine.extractValue(target, it, context).captureInto(diagnostics)
            },
        )
        reportDiagnostics(source.id, pageUrl, diagnostics)
        return detail
    }

    private fun executeToc(
        source: NormalizedSourceDefinition,
        body: String,
        pageUrl: String,
    ): List<RemoteChapter> {
        val rule = source.tocRule ?: return emptyList()
        val diagnostics = mutableListOf<RuleExecutionDiagnostic>()
        val context = RuleExecutionContext(
            baseUrl = source.baseUrl,
            pageUrl = pageUrl,
        )
        val target = rule.responseKind.toRuleTarget(body, pageUrl)
        val items = ruleExecutionEngine.extractList(target, rule.chapterList, context).captureInto(diagnostics)
        val chapters = items.mapNotNull { item ->
            val title = ruleExecutionEngine.extractValue(item, rule.chapterName, context).captureInto(diagnostics)
                ?: return@mapNotNull null
            val url = ruleExecutionEngine.extractValue(item, rule.chapterUrl, context).captureInto(diagnostics)
                ?: return@mapNotNull null
            RemoteChapter(
                chapterRef = resolveUrl(source.baseUrl, url),
                title = title,
            )
        }
        reportDiagnostics(source.id, pageUrl, diagnostics)
        return chapters
    }

    private fun executeContent(
        source: NormalizedSourceDefinition,
        body: String,
        pageUrl: String,
    ): RemoteChapterContent {
        val rule = source.contentRule
            ?: return RemoteChapterContent(
                chapterRef = pageUrl,
                title = pageTitle(body, pageUrl),
                content = fallbackContent(body, ResponseKind.HTML),
            )
        val diagnostics = mutableListOf<RuleExecutionDiagnostic>()
        val context = RuleExecutionContext(
            baseUrl = source.baseUrl,
            pageUrl = pageUrl,
        )
        val target = rule.responseKind.toRuleTarget(body, pageUrl)
        val content = ruleExecutionEngine.extractValue(target, rule.content, context).captureInto(diagnostics)
            ?: fallbackContent(body, rule.responseKind)
        val chapterContent = RemoteChapterContent(
            chapterRef = pageUrl,
            title = pageTitle(body, pageUrl),
            content = content,
        )
        reportDiagnostics(source.id, pageUrl, diagnostics)
        return chapterContent
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

    private fun ResponseKind.toRuleTarget(
        body: String,
        pageUrl: String,
    ): RuleTarget =
        when (this) {
            ResponseKind.HTML -> RuleTarget.Html(Jsoup.parse(body, pageUrl))
            ResponseKind.JSON -> RuleTarget.Json(body)
        }

    private fun pageTitle(
        body: String,
        fallback: String,
    ): String =
        Jsoup.parse(body).title().ifBlank { fallback }

    private fun fallbackContent(
        body: String,
        responseKind: ResponseKind,
    ): String =
        when (responseKind) {
            ResponseKind.HTML -> Jsoup.parse(body).body().text()
            ResponseKind.JSON -> body
        }

    private fun reportDiagnostics(
        sourceId: String,
        pageUrl: String,
        diagnostics: List<RuleExecutionDiagnostic>,
    ) {
        if (diagnostics.isNotEmpty()) {
            diagnosticReporter.report(sourceId, pageUrl, diagnostics)
        }
    }

    private data class RequestExecutionResult(
        val url: String,
        val body: String,
    )
}
