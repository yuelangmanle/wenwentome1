package com.wenwentome.reader.bridge.source

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceCompatibilityPhase1FixtureTest {
    private val json = Json { ignoreUnknownKeys = true }
    private val sourceRuleParser = SourceRuleParser()

    @Test
    fun phase1TargetFixtures_coverAtLeastTenSupportedSources() {
        val manifest = loadFixtureManifest()

        assertTrue(manifest.size >= 10)
        assertTrue(manifest.count { it.primaryEngine == "HTML_CSS" } >= 4)
        assertTrue(manifest.count { it.hasJsonRule } >= 2)
        assertTrue(manifest.count { it.hasXPathRule } >= 2)
        assertTrue(manifest.count { it.hasCleanerOrScript } >= 2)
    }

    @Test
    fun eachTargetFixture_canRunSearchDetailTocAndContent() = runTest {
        loadFixtureManifest().forEach { target ->
            assertSourceFixtureRoundTrip(target)
        }
    }

    private suspend fun assertSourceFixtureRoundTrip(target: Phase1TargetManifest) {
        val definition = sourceRuleParser.parse(target.toSourceJson())
        val responses = linkedMapOf<String, String>()
        responses[target.requests.search.url] = phase1Fixture(target.requests.search.bodyFile)
        responses[target.requests.detail.url] = phase1Fixture(target.requests.detail.bodyFile)
        responses[target.requests.toc.url] = phase1Fixture(target.requests.toc.bodyFile)
        responses[target.requests.content.url] = phase1Fixture(target.requests.content.bodyFile)
        val repository = RealSourceBridgeRepository(
            sourceProvider = StaticSourceDefinitionProvider(listOf(definition)),
            httpClient = phase1Client(responses),
        )

        val results = repository.search(target.query, listOf(definition.id))
        assertFalse("search results empty for ${target.sourceName}", results.isEmpty())
        assertEquals(target.expected.searchTitle, results.first().title)
        assertEquals(target.expected.searchAuthor, results.first().author)
        assertEquals(target.requests.detail.url, results.first().detailUrl)

        val detail = repository.fetchBookDetail(definition.id, target.requests.detail.url)
        assertEquals(target.expected.detailTitle, detail.title)
        assertEquals(target.expected.detailAuthor, detail.author)
        assertEquals(target.expected.lastChapter, detail.lastChapter)

        val toc = repository.fetchToc(definition.id, target.requests.toc.url)
        assertFalse("toc empty for ${target.sourceName}", toc.isEmpty())
        assertEquals(target.expected.firstChapterTitle, toc.first().title)
        assertEquals(target.expected.firstChapterRef, toc.first().chapterRef)

        val content = repository.fetchChapterContent(definition.id, toc.first().chapterRef)
        assertTrue(
            "content snippet missing for ${target.sourceName}",
            content.content.contains(target.expected.contentSnippet),
        )
    }

    private fun loadFixtureManifest(): List<Phase1TargetManifest> {
        val index = phase1Fixture("source-phase1/index.json")
        val manifestPaths = json.decodeFromString<List<String>>(index)
        return manifestPaths.map { path ->
            json.decodeFromString<Phase1TargetManifest>(phase1Fixture("source-phase1/$path"))
        }
    }

    private fun Phase1TargetManifest.toSourceJson(): String =
        buildJsonObject {
            put("bookSourceName", JsonPrimitive(sourceName))
            put("bookSourceUrl", JsonPrimitive(sourceId))
            put("searchUrl", JsonPrimitive(source.searchUrl))
            put("ruleSearch", source.ruleSearch.toJsonObject())
            put("ruleBookInfo", source.ruleBookInfo.toJsonObject())
            put("ruleToc", source.ruleToc.toJsonObject())
            put("ruleContent", source.ruleContent.toJsonObject())
        }.toString()

    private fun Map<String, String>.toJsonObject(): JsonObject =
        buildJsonObject {
            entries.sortedBy { it.key }.forEach { (key, value) ->
                put(key, JsonPrimitive(value))
            }
        }
}

@Serializable
private data class Phase1TargetManifest(
    val slug: String,
    val sourceId: String,
    val sourceName: String,
    val sampleBookTitle: String,
    val query: String,
    val primaryEngine: String,
    val hasJsonRule: Boolean,
    val hasXPathRule: Boolean,
    val hasCleanerOrScript: Boolean,
    val source: Phase1SourceDefinition,
    val requests: Phase1RequestBundle,
    val expected: Phase1ExpectedRoundTrip,
)

@Serializable
private data class Phase1SourceDefinition(
    val searchUrl: String,
    val ruleSearch: Map<String, String>,
    val ruleBookInfo: Map<String, String>,
    val ruleToc: Map<String, String>,
    val ruleContent: Map<String, String>,
)

@Serializable
private data class Phase1RequestBundle(
    val search: Phase1Request,
    val detail: Phase1Request,
    val toc: Phase1Request,
    val content: Phase1Request,
)

@Serializable
private data class Phase1Request(
    val url: String,
    val bodyFile: String,
)

@Serializable
private data class Phase1ExpectedRoundTrip(
    val searchTitle: String,
    val searchAuthor: String? = null,
    val detailTitle: String,
    val detailAuthor: String? = null,
    val lastChapter: String? = null,
    val firstChapterTitle: String,
    val firstChapterRef: String,
    val contentSnippet: String,
)

private fun phase1Client(successBodies: Map<String, String>): OkHttpClient =
    OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request()
            val body = successBodies[request.url.toString()]
            if (body == null) {
                phase1FailureResponse(request)
            } else {
                phase1SuccessResponse(request, body)
            }
        }
        .build()

private fun phase1SuccessResponse(
    request: Request,
    body: String,
): Response =
    Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(200)
        .message("OK")
        .body(body.toResponseBody("text/plain".toMediaType()))
        .build()

private fun phase1FailureResponse(request: Request): Response =
    Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(500)
        .message("Boom")
        .body("error".toResponseBody("text/plain".toMediaType()))
        .build()

private fun phase1Fixture(name: String): String =
    requireNotNull(SourceCompatibilityPhase1FixtureTest::class.java.getResourceAsStream("/fixtures/$name")) {
        "Missing fixture $name"
    }.bufferedReader().use { it.readText() }
