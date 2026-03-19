package com.wenwentome.reader.bridge.source

import com.wenwentome.reader.bridge.source.model.BookInfoRule
import com.wenwentome.reader.bridge.source.model.ContentRule
import com.wenwentome.reader.bridge.source.model.NormalizedSourceDefinition
import com.wenwentome.reader.bridge.source.model.RemoteBookDetail
import com.wenwentome.reader.bridge.source.model.RemoteChapter
import com.wenwentome.reader.bridge.source.model.RemoteChapterContent
import com.wenwentome.reader.bridge.source.model.RemoteSearchResult
import com.wenwentome.reader.bridge.source.model.SearchRule
import com.wenwentome.reader.bridge.source.model.TocRule
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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
    private val json = Json { ignoreUnknownKeys = true }

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
        rawTemplate: String,
        query: String,
        page: Int = 1,
    ): String {
        require(!rawTemplate.startsWith("<js>")) {
            "JS templates are not supported in the core bridge yet"
        }
        val urlPart = rawTemplate.substringBefore(",{")
        val configPart = rawTemplate.substringAfter(urlPart, "").removePrefix(",")
        val resolvedUrl = resolveUrl(
            source.baseUrl,
            interpolate(urlPart, query, page),
        )
        val request = if (configPart.startsWith("{")) {
            val config = json.parseToJsonElement(configPart).jsonObject
            val method = config["method"]?.jsonPrimitive?.contentOrNull?.uppercase().orEmpty()
            if (method == "POST") {
                val body = interpolate(config["body"]?.jsonPrimitive?.contentOrNull.orEmpty(), query, page)
                Request.Builder()
                    .url(resolvedUrl)
                    .post(body.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
                    .build()
            } else {
                Request.Builder().url(resolvedUrl).get().build()
            }
        } else {
            Request.Builder().url(resolvedUrl).get().build()
        }
        return executeRequest(request)
    }

    private suspend fun executeSearch(
        source: NormalizedSourceDefinition,
        document: Document,
    ): List<RemoteSearchResult> {
        val rule = source.searchRule ?: return emptyList()
        if (rule.bookList.startsWith("$")) {
            return emptyList()
        }
        return jsoupRuleExecutor.selectElements(document, rule.bookList).mapNotNull { item ->
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
        )
    }

    private fun executeToc(source: NormalizedSourceDefinition, document: Document): List<RemoteChapter> {
        val rule = source.tocRule ?: return emptyList()
        return jsoupRuleExecutor.selectElements(document, rule.chapterList).mapNotNull { element ->
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
        val content = rule?.replaceRegex?.takeIf { it.isNotBlank() }?.let { regex ->
            rawContent.replace(Regex(regex), "")
        } ?: rawContent
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

    private fun Document.extract(expression: String?): String? =
        expression?.let { jsoupRuleExecutor.select(this, it).firstOrNull() }

    private fun org.jsoup.nodes.Element.extract(expression: String?): String? =
        expression?.let { jsoupRuleExecutor.select(this, it).firstOrNull() }
}
