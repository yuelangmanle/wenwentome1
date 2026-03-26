package io.legado.app.ui.wenwen

import io.legado.app.constant.BookType
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.help.book.addType
import io.legado.app.help.book.isType
import io.legado.app.help.book.removeType
import io.legado.app.help.http.CookieManager.cookieJarHeader
import io.legado.app.help.http.newCallStrResponse
import io.legado.app.help.http.okHttpClient
import io.legado.app.utils.GSON
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.wenwentome.reader.core.model.BrowserFindPreferences
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import splitties.init.appCtx

internal const val EXTRA_WENWEN_BROWSER_MODE_ENABLED = "wenwentome.browser_mode_enabled"
internal const val EXTRA_WENWEN_BROWSER_SEARCH_QUERY = "wenwentome.browser_search_query"
internal const val EXTRA_WENWEN_BROWSER_AUTO_OPTIMIZE = "wenwentome.browser_auto_optimize"
internal const val EXTRA_WENWEN_BROWSER_SHOW_MANUAL_BUTTON = "wenwentome.browser_show_manual_button"
internal const val WENWEN_BROWSER_BOOK_ORIGIN = "wenwentome::browser"

data class WenwenBrowserSearchRequest(
    val query: String,
    val url: String,
    val title: String,
    val sourceName: String,
    val enableWenwenBrowserMode: Boolean,
    val autoOptimizeReading: Boolean,
    val showManualOptimizeFloatingButton: Boolean,
)

object WenwenBrowserSearchRequestFactory {

    fun create(
        query: String,
        preferences: BrowserFindPreferences,
    ): WenwenBrowserSearchRequest? {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isEmpty()) return null
        val engine = preferences.activeSearchEngine()
        return WenwenBrowserSearchRequest(
            query = normalizedQuery,
            url = preferences.buildSearchUrl(normalizedQuery),
            title = "${engine.label} 搜索",
            sourceName = "浏览器找书 · ${preferences.browserMode.label}",
            enableWenwenBrowserMode = true,
            autoOptimizeReading = preferences.autoOptimizeReading,
            showManualOptimizeFloatingButton = preferences.showManualOptimizeFloatingButton,
        )
    }
}

enum class WenwenBrowserOptimizeTrigger {
    AUTO,
    MANUAL,
}

data class WenwenBrowserArticle(
    val url: String,
    val title: String,
    val content: String,
    val sourceLabel: String,
    val coverUrl: String? = null,
    val searchQuery: String? = null,
    val nextPageUrl: String? = null,
    val tocEntries: List<WenwenBrowserTocEntry> = emptyList(),
)

data class WenwenBrowserTocEntry(
    val title: String,
    val url: String,
    val orderIndex: Int,
)

internal data class WenwenBrowserReadabilityPayload(
    val title: String,
    val textContent: String,
    val contentHtml: String? = null,
    val excerpt: String? = null,
    val byline: String? = null,
    val siteName: String? = null,
    val coverUrl: String? = null,
    val nextPageUrl: String? = null,
    val tocEntries: List<WenwenBrowserTocEntry> = emptyList(),
)

internal object WenwenBrowserReadabilityPayloadDecoder {

    fun decode(raw: String?): WenwenBrowserReadabilityPayload? {
        if (raw.isNullOrBlank() || raw == "null") return null
        return kotlin.runCatching {
            val firstPass = JsonParser.parseString(raw)
            val json =
                when {
                    firstPass.isJsonObject -> firstPass.asJsonObject
                    firstPass.isJsonPrimitive && firstPass.asJsonPrimitive.isString ->
                        JsonParser.parseString(firstPass.asString).takeIf { it.isJsonObject }?.asJsonObject
                    else -> null
                } ?: return null
            WenwenBrowserReadabilityPayload(
                title = json.stringValue("title").orEmpty(),
                textContent = json.stringValue("textContent").orEmpty(),
                contentHtml = json.stringValue("content")?.ifBlank { null },
                excerpt = json.stringValue("excerpt")?.ifBlank { null },
                byline = json.stringValue("byline")?.ifBlank { null },
                siteName = json.stringValue("siteName")?.ifBlank { null },
                coverUrl = json.stringValue("coverUrl")?.ifBlank { null },
                nextPageUrl = json.stringValue("nextPageUrl")?.ifBlank { null },
                tocEntries = json.getAsJsonArray("tocEntries")?.let { array ->
                        buildList {
                            array.forEach { element ->
                                val item = element.takeIf { it.isJsonObject }?.asJsonObject ?: return@forEach
                                val title = item.stringValue("title").orEmpty().trim()
                                val url = item.stringValue("url").orEmpty().trim()
                                if (title.isBlank() || url.isBlank()) return@forEach
                                add(
                                    WenwenBrowserTocEntry(
                                        title = title,
                                        url = url,
                                        orderIndex = item.intValue("orderIndex") ?: size,
                                    )
                                )
                            }
                        }
                    }.orEmpty(),
            )
        }.getOrNull()
    }

    private fun JsonObject.stringValue(name: String): String? {
        val element = get(name) ?: return null
        return if (element.isJsonNull) null else element.asString
    }

    private fun JsonObject.intValue(name: String): Int? {
        val element = get(name) ?: return null
        return if (element.isJsonNull) null else element.asInt
    }
}

internal object WenwenBrowserReadabilityBridge {

    private const val READABILITY_ASSET_PATH = "wenwen/browser/Readability.js"
    private const val PURIFY_ASSET_PATH = "wenwen/browser/purify.min.js"

    @Volatile
    private var cachedBootstrapScript: String? = null

    fun bootstrapScript(): String {
        cachedBootstrapScript?.let { return it }
        return synchronized(this) {
            cachedBootstrapScript ?: buildBootstrapScript().also { cachedBootstrapScript = it }
        }
    }

    fun extractCallScript(): String {
        return """
            (function() {
              try {
                if (typeof window.__WENWEN_EXTRACT_READER_PAYLOAD__ !== 'function') {
                  return null;
                }
                return window.__WENWEN_EXTRACT_READER_PAYLOAD__();
              } catch (error) {
                return null;
              }
            })();
        """.trimIndent()
    }

    private fun buildBootstrapScript(): String {
        val readabilityJs = readAssetText(READABILITY_ASSET_PATH)
        val purifyJs = readAssetText(PURIFY_ASSET_PATH)
        return """
            (function() {
              if (window.__WENWEN_READABILITY_READY__ === true) {
                return true;
              }
              $readabilityJs
              $purifyJs
              window.__WENWEN_ABSOLUTE_URL__ = function(value) {
                if (!value) {
                  return '';
                }
                try {
                  return new URL(value, location.href).href;
                } catch (error) {
                  return '';
                }
              };
              window.__WENWEN_FIND_NEXT_PAGE__ = function() {
                var anchors = Array.from(document.querySelectorAll('a[href]'));
                var matched = anchors.find(function(anchor) {
                  var text = (anchor.textContent || '').trim();
                  return /(下一章|下一页|下页|next|›|»)/i.test(text);
                });
                if (!matched) {
                  return '';
                }
                return window.__WENWEN_ABSOLUTE_URL__(matched.getAttribute('href'));
              };
              window.__WENWEN_FIND_TOC__ = function() {
                var chapterPattern = /(第.{1,16}(章|节|回|卷)|chapter\\s*\\d+|序章|楔子|尾声|番外)/i;
                var entries = [];
                var seen = {};
                Array.from(document.querySelectorAll('a[href]')).forEach(function(anchor) {
                  var text = (anchor.textContent || '').replace(/\\s+/g, ' ').trim();
                  if (!text || text.length > 40 || !chapterPattern.test(text)) {
                    return;
                  }
                  var href = window.__WENWEN_ABSOLUTE_URL__(anchor.getAttribute('href'));
                  if (!href || seen[href]) {
                    return;
                  }
                  seen[href] = true;
                  entries.push({
                    title: text,
                    url: href,
                    orderIndex: entries.length
                  });
                });
                return entries.length >= 3 ? entries.slice(0, 200) : [];
              };
              window.__WENWEN_FIND_COVER__ = function() {
                return (
                  document.querySelector('meta[property="og:image"]')?.content ||
                  document.querySelector('meta[name="og:image"]')?.content ||
                  document.querySelector('meta[name="twitter:image"]')?.content ||
                  document.querySelector('img')?.src ||
                  ''
                );
              };
              window.__WENWEN_EXTRACT_READER_PAYLOAD__ = function() {
                var article = null;
                try {
                  article = new Readability(document.cloneNode(true), {
                    charThreshold: 80,
                    keepClasses: false
                  }).parse();
                } catch (error) {
                  article = null;
                }
                var contentHtml = article && article.content ? article.content : '';
                if (contentHtml && typeof DOMPurify !== 'undefined') {
                  contentHtml = DOMPurify.sanitize(contentHtml, {
                    USE_PROFILES: { html: true }
                  });
                }
                var textContent =
                  article && article.textContent
                    ? article.textContent
                    : (document.body ? (document.body.innerText || '') : '');
                return JSON.stringify({
                  title: article && article.title ? article.title : (document.title || ''),
                  textContent: textContent || '',
                  content: contentHtml || '',
                  excerpt: article && article.excerpt ? article.excerpt : '',
                  byline: article && article.byline ? article.byline : '',
                  siteName: article && article.siteName ? article.siteName : (location.host || ''),
                  coverUrl: window.__WENWEN_ABSOLUTE_URL__(window.__WENWEN_FIND_COVER__()),
                  nextPageUrl: window.__WENWEN_FIND_NEXT_PAGE__(),
                  tocEntries: window.__WENWEN_FIND_TOC__()
                });
              };
              window.__WENWEN_READABILITY_READY__ = true;
              return true;
            })();
        """.trimIndent()
    }

    private fun readAssetText(path: String): String {
        return appCtx.assets.open(path).bufferedReader().use { it.readText() }
    }
}

internal data class WenwenBrowserSiteRule(
    val hostSuffixes: Set<String>,
    val titleSelectors: List<String> = emptyList(),
    val contentSelectors: List<String> = emptyList(),
    val nextSelectors: List<String> = emptyList(),
)

internal object WenwenBrowserSiteRuleRegistry {

    private val rules =
        listOf(
            WenwenBrowserSiteRule(
                hostSuffixes = setOf("fanqienovel.com"),
                titleSelectors = listOf("h1", ".muye-reader-title", ".chapter-title"),
                contentSelectors = listOf(".read-content", ".j_readContent", ".muye-reader-content"),
                nextSelectors = listOf("a.next", "a:matchesOwn((下一章|下一页|下页))"),
            ),
            WenwenBrowserSiteRule(
                hostSuffixes = setOf("qidian.com"),
                titleSelectors = listOf(".j_chapterName", ".content-wrap h3"),
                contentSelectors = listOf(".read-content", ".content-wrap .main-text-wrap"),
                nextSelectors = listOf("a:matchesOwn((下一章|下一页|下页))"),
            ),
        )

    fun match(url: String): WenwenBrowserSiteRule? {
        val host = kotlin.runCatching { URI(url).host?.lowercase()?.removePrefix("m.") }.getOrNull()
        return rules.firstOrNull { rule ->
            host != null && rule.hostSuffixes.any { host == it || host.endsWith(".$it") }
        }
    }
}

internal object WenwenBrowserHtmlPageExtractor {

    private val genericTitleSelectors =
        listOf(
            "h1",
            ".bookname h1",
            ".content h1",
            ".chapter-title",
            ".article-title",
            ".title",
        )
    private val genericContentSelectors =
        listOf(
            "#content",
            "#chaptercontent",
            "#nr",
            "#nr1",
            ".content",
            ".article",
            ".article-content",
            ".read-content",
            ".chapter-content",
            ".yd_text2",
            ".txtnav",
            "article",
            "main",
        )
    private val genericNextSelectors =
        listOf(
            "a[rel=next]",
            "a.next",
            "a.nextpage",
            "a:matchesOwn((下一章|下一页|下页|next|›|»))",
        )

    fun extract(
        url: String,
        html: String,
        trigger: WenwenBrowserOptimizeTrigger,
        searchQuery: String? = null,
    ): WenwenBrowserArticle? {
        val doc = Jsoup.parse(html, url)
        val rule = WenwenBrowserSiteRuleRegistry.match(url)
        val title = extractTitle(doc, rule).ifBlank { doc.title().ifBlank { "网页正文" } }
        val contentElement = extractContentElement(doc, rule)
        val contentText = extractContentText(contentElement ?: doc.body())
        val nextPageUrl = extractNextPageUrl(doc, url, rule)
        val coverUrl = extractCoverUrl(doc, url)
        val tocEntries = extractTocEntries(doc, url)
        return WenwenBrowserArticleExtractor.extract(
            url = url,
            title = title,
            rawContent = contentText,
            trigger = trigger,
            coverUrl = coverUrl,
            searchQuery = searchQuery,
            nextPageUrl = nextPageUrl,
            tocEntries = tocEntries,
        )
    }

    private fun extractTitle(
        doc: org.jsoup.nodes.Document,
        rule: WenwenBrowserSiteRule?,
    ): String {
        val selectors = buildList {
            addAll(rule?.titleSelectors.orEmpty())
            addAll(genericTitleSelectors)
        }
        selectors.forEach { selector ->
            doc.selectFirst(selector)?.text()?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
        }
        return doc.title().trim()
    }

    private fun extractContentElement(
        doc: org.jsoup.nodes.Document,
        rule: WenwenBrowserSiteRule?,
    ): Element? {
        val selectors = buildList {
            addAll(rule?.contentSelectors.orEmpty())
            addAll(genericContentSelectors)
        }
        selectors.forEach { selector ->
            doc.select(selector)
                .filterNot(::isNoiseElement)
                .maxByOrNull { scoreElement(it) }
                ?.takeIf { scoreElement(it) >= 80 }
                ?.let { return it }
        }
        return doc.select("article, main, section, div")
            .filterNot(::isNoiseElement)
            .maxByOrNull(::scoreElement)
            ?.takeIf { scoreElement(it) >= 120 }
    }

    private fun extractContentText(element: Element?): String {
        val raw =
            element?.wholeText()?.ifBlank { element.text() }
                ?: return ""
        return raw
            .replace('\u00A0', ' ')
            .replace("\r\n", "\n")
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString("\n\n")
    }

    private fun extractNextPageUrl(
        doc: org.jsoup.nodes.Document,
        baseUrl: String,
        rule: WenwenBrowserSiteRule?,
    ): String? {
        val selectors = buildList {
            addAll(rule?.nextSelectors.orEmpty())
            addAll(genericNextSelectors)
        }
        selectors.forEach { selector ->
            doc.select(selector)
                .firstNotNullOfOrNull { anchor ->
                    normalizeHref(baseUrl, anchor.attr("href"))
                }?.let { return it }
        }
        return doc.select("a[href]")
            .firstNotNullOfOrNull { anchor ->
                val text = anchor.text().trim()
                if (!Regex("(下一章|下一页|下页|next|›|»)", RegexOption.IGNORE_CASE).containsMatchIn(text)) {
                    return@firstNotNullOfOrNull null
                }
                normalizeHref(baseUrl, anchor.attr("href"))
            }
    }

    private fun extractCoverUrl(
        doc: org.jsoup.nodes.Document,
        baseUrl: String,
    ): String? {
        return listOf(
            doc.selectFirst("meta[property=og:image]")?.attr("content"),
            doc.selectFirst("meta[name=og:image]")?.attr("content"),
            doc.selectFirst("meta[name=twitter:image]")?.attr("content"),
            doc.selectFirst("img")?.attr("src"),
        ).firstNotNullOfOrNull { normalizeHref(baseUrl, it) }
    }

    private fun normalizeHref(
        baseUrl: String,
        href: String?,
    ): String? {
        val normalized = href?.trim().orEmpty()
        if (normalized.isEmpty()) return null
        return kotlin.runCatching {
            URI(baseUrl).resolve(normalized).toString()
        }.getOrNull()
    }

    private fun extractTocEntries(
        doc: org.jsoup.nodes.Document,
        baseUrl: String,
    ): List<WenwenBrowserTocEntry> {
        val chapterPattern = Regex("(第.{1,16}(章|节|回|卷)|chapter\\s*\\d+|序章|楔子|尾声|番外)", RegexOption.IGNORE_CASE)
        fun Element.chapterAnchors(): List<WenwenBrowserTocEntry> {
            return select("a[href]")
                .mapNotNull { anchor ->
                    val title = anchor.text().replace(Regex("\\s+"), " ").trim()
                    val url = normalizeHref(baseUrl, anchor.attr("href"))
                    if (title.isBlank() || title.length > 40 || url.isNullOrBlank()) return@mapNotNull null
                    if (!chapterPattern.containsMatchIn(title)) return@mapNotNull null
                    WenwenBrowserTocEntry(
                        title = title,
                        url = url,
                        orderIndex = 0,
                    )
                }
                .distinctBy { it.url }
        }

        val candidateContainers =
            doc.select(
                "#list, #chapterlist, .chapter-list, .catalog, .catalog-list, .book-list, .volume, ul, ol, nav, section, div"
            )
        val bestEntries =
            candidateContainers
                .map { it.chapterAnchors() }
                .filter { it.size >= 3 }
                .maxByOrNull { entries -> entries.size }
                ?: doc.body()?.chapterAnchors().orEmpty()
        if (bestEntries.size < 3) return emptyList()
        return bestEntries
            .take(200)
            .mapIndexed { index, entry -> entry.copy(orderIndex = index) }
    }

    private fun scoreElement(element: Element): Int {
        val textLength = element.text().trim().length
        val linkTextLength = element.select("a").sumOf { it.text().trim().length }
        val paragraphCount = element.select("p, br").size
        return textLength + paragraphCount * 24 - linkTextLength * 2
    }

    private fun isNoiseElement(element: Element): Boolean {
        val marker = buildString {
            append(element.tagName())
            append(' ')
            append(element.id())
            append(' ')
            append(element.className())
        }.lowercase()
        if (Regex("(nav|header|footer|menu|toolbar|comment|ads|advert|copyright|share|recommend)")
                .containsMatchIn(marker)
        ) {
            return true
        }
        return false
    }
}

internal object WenwenBrowserBackgroundFetcher {

    suspend fun fetchArticle(
        url: String,
        searchQuery: String? = null,
        referer: String? = null,
    ): WenwenBrowserArticle? {
        val response =
            okHttpClient.newCallStrResponse {
                url(url)
                addHeader(cookieJarHeader, "true")
                referer?.takeIf { it.isNotBlank() }?.let {
                    header("Referer", it)
                }
            }
        if (!response.isSuccessful()) return null
        val responseBody = response.body ?: return null
        return WenwenBrowserHtmlPageExtractor.extract(
            url = response.url,
            html = responseBody,
            trigger = WenwenBrowserOptimizeTrigger.MANUAL,
            searchQuery = searchQuery,
        )
    }
}

object WenwenBrowserArticleExtractor {

    private val searchEngineHosts =
        setOf(
            "bing.com",
            "baidu.com",
            "sogou.com",
            "toutiao.com",
            "sm.cn",
            "google.com",
        )

    fun shouldSkipOptimization(url: String): Boolean {
        val host = extractHost(url) ?: return false
        return searchEngineHosts.any { host == it || host.endsWith(".$it") }
    }

    fun extract(
        url: String,
        title: String,
        rawContent: String,
        trigger: WenwenBrowserOptimizeTrigger,
        coverUrl: String? = null,
        searchQuery: String? = null,
        nextPageUrl: String? = null,
        tocEntries: List<WenwenBrowserTocEntry> = emptyList(),
    ): WenwenBrowserArticle? {
        if (shouldSkipOptimization(url)) return null

        val normalizedTitle = normalizeTitle(title, url)
        val normalizedContent = normalizeContent(rawContent, normalizedTitle)
        val normalizedTocEntries =
            tocEntries
                .mapNotNull { entry ->
                    val normalizedEntryTitle = entry.title.trim()
                    val normalizedEntryUrl = entry.url.trim()
                    if (normalizedEntryTitle.isBlank() || normalizedEntryUrl.isBlank()) {
                        null
                    } else {
                        entry.copy(
                            title = normalizedEntryTitle,
                            url = normalizedEntryUrl,
                        )
                    }
                }
                .distinctBy { it.url }
                .sortedBy { it.orderIndex }
                .mapIndexed { index, entry -> entry.copy(orderIndex = index) }
        if (!passesThreshold(normalizedContent, trigger)) return null

        return WenwenBrowserArticle(
            url = url,
            title = normalizedTitle,
            content = normalizedContent,
            sourceLabel = extractHost(url) ?: "网页正文",
            coverUrl = coverUrl?.trim()?.takeIf { it.isNotEmpty() },
            searchQuery = searchQuery?.trim()?.takeIf { it.isNotEmpty() },
            nextPageUrl = nextPageUrl?.trim()?.takeIf { it.isNotEmpty() },
            tocEntries = normalizedTocEntries,
        )
    }

    private fun normalizeTitle(
        title: String,
        url: String,
    ): String {
        val normalized =
            title
                .replace('\n', ' ')
                .replace(Regex("\\s+"), " ")
                .trim()
        return normalized.ifBlank { extractHost(url) ?: "网页正文" }
    }

    private fun normalizeContent(
        rawContent: String,
        title: String,
    ): String {
        val paragraphs =
            rawContent
                .replace("\u0000", "")
                .replace("\r\n", "\n")
                .replace('\u00A0', ' ')
                .lineSequence()
                .map { it.replace(Regex("\\s+"), " ").trim() }
                .filter { it.isNotBlank() }
                .toMutableList()

        while (paragraphs.isNotEmpty() && paragraphs.first() == title) {
            paragraphs.removeAt(0)
        }

        return paragraphs.joinToString("\n\n")
    }

    private fun passesThreshold(
        content: String,
        trigger: WenwenBrowserOptimizeTrigger,
    ): Boolean {
        val paragraphs = content.split("\n\n").filter { it.isNotBlank() }
        val charCount = content.count { !it.isWhitespace() }
        val longParagraphs = paragraphs.count { it.length >= 24 }
        return when (trigger) {
            WenwenBrowserOptimizeTrigger.AUTO -> charCount >= 120 && longParagraphs >= 3
            WenwenBrowserOptimizeTrigger.MANUAL -> charCount >= 60 && longParagraphs >= 2
        }
    }

    private fun extractHost(url: String): String? {
        return runCatching {
            URI(url).host?.lowercase()?.removePrefix("m.")
        }.getOrNull()
    }
}

internal data class WenwenBrowserSavedBook(
    val book: Book,
    val chapter: BookChapter,
    val content: String,
)

internal data class WenwenBrowserBookMergePlan(
    val book: Book,
    val chapter: BookChapter,
)

internal data class WenwenBrowserBookMetadata(
    val originalUrl: String,
    val sourceLabel: String,
    val searchQuery: String? = null,
    val nextPageUrl: String? = null,
    val tocEntries: List<WenwenBrowserTocEntry> = emptyList(),
)

internal fun readWenwenBrowserMetadata(book: Book?): WenwenBrowserBookMetadata? {
    if (book == null || !book.origin.startsWith(WENWEN_BROWSER_BOOK_ORIGIN)) return null
    return kotlin.runCatching {
        GSON.fromJson(book.variable, WenwenBrowserBookMetadata::class.java)
    }.getOrNull()
}

internal object WenwenBrowserBookBridge {

    fun createBook(
        article: WenwenBrowserArticle,
        addToShelf: Boolean,
    ): WenwenBrowserSavedBook {
        val normalizedTitle = article.title.trim().ifBlank { "网页正文" }
        val normalizedBookName = article.searchQuery?.trim()?.takeIf { it.isNotEmpty() } ?: normalizedTitle
        val stableBookUrl = buildStableBookUrl(article, normalizedBookName)
        val summary =
            article.content
                .lineSequence()
                .map { it.trim() }
                .firstOrNull { it.isNotBlank() }
                .orEmpty()
        val book =
            Book(
                bookUrl = stableBookUrl,
                origin = WENWEN_BROWSER_BOOK_ORIGIN,
                originName = article.sourceLabel,
                name = normalizedBookName,
                author = article.sourceLabel,
                coverUrl = article.coverUrl,
                intro = summary,
                type = BookType.text,
                variable =
                    GSON.toJson(
                        WenwenBrowserBookMetadata(
                            originalUrl = article.url,
                            sourceLabel = article.sourceLabel,
                            searchQuery = article.searchQuery,
                            nextPageUrl = article.nextPageUrl,
                            tocEntries = article.tocEntries,
                        )
                    ),
            ).apply {
                if (!addToShelf) {
                    addType(BookType.notShelf)
                }
            }
        val chapter =
            BookChapter(
                url = article.url,
                title = normalizedTitle,
                baseUrl = article.url,
                bookUrl = stableBookUrl,
                index = 0,
            )
        return WenwenBrowserSavedBook(
            book = book,
            chapter = chapter,
            content = article.content,
        )
    }

    fun createTocChapters(
        article: WenwenBrowserArticle,
        bookUrl: String,
    ): List<BookChapter> {
        return article.tocEntries
            .distinctBy { it.url }
            .map { entry ->
                BookChapter(
                    url = entry.url,
                    title = entry.title,
                    baseUrl = entry.url,
                    bookUrl = bookUrl,
                    index = entry.orderIndex,
                )
            }
    }

    private fun buildStableBookUrl(
        article: WenwenBrowserArticle,
        bookName: String,
    ): String {
        val host =
            kotlin.runCatching {
                URI(article.url).host?.lowercase()?.removePrefix("m.")
            }.getOrNull()?.takeIf { it.isNotBlank() }
        val encodedBookName = encodePathSegment(bookName)
        return if (host != null) {
            "wenwentome-browser://$host/$encodedBookName"
        } else {
            val encodedSource = encodePathSegment(article.sourceLabel.ifBlank { "browser" })
            "wenwentome-browser://source/$encodedSource/$encodedBookName"
        }
    }

    private fun encodePathSegment(value: String): String {
        return URLEncoder.encode(value, StandardCharsets.UTF_8.toString()).replace("+", "%20")
    }
}

internal object WenwenBrowserBookMergePlanner {

    fun merge(
        incoming: WenwenBrowserSavedBook,
        existingBook: Book?,
        existingChapters: List<BookChapter>,
        duplicateByNameAuthor: Book?,
        addToShelf: Boolean,
        minShelfOrder: Int,
    ): WenwenBrowserBookMergePlan {
        val now = System.currentTimeMillis()
        val existingChapter = existingChapters.firstOrNull { it.url == incoming.chapter.url }
        val nextChapterIndex =
            existingChapter?.index
                ?: ((existingChapters.maxOfOrNull { it.index } ?: -1) + 1)
        val mergedChapter =
            incoming.chapter.copy(
                bookUrl = incoming.book.bookUrl,
                index = nextChapterIndex,
                baseUrl = incoming.chapter.baseUrl.ifBlank { incoming.chapter.url },
            )
        val mergedChapterCount =
            if (existingChapter != null) {
                existingChapters.size.coerceAtLeast(1)
            } else {
                existingChapters.size + 1
            }
        val mergedBook =
            existingBook?.copy(
                origin = incoming.book.origin,
                originName = incoming.book.originName,
                coverUrl = incoming.book.coverUrl ?: existingBook.coverUrl,
                intro = incoming.book.intro?.takeIf { it.isNotBlank() } ?: existingBook.intro,
                variable = incoming.book.variable,
                latestChapterTitle = mergedChapter.title,
                latestChapterTime = now,
                lastCheckTime = now,
                totalChapterNum = mergedChapterCount,
                durChapterIndex = mergedChapter.index,
                durChapterTitle = mergedChapter.title,
                durChapterPos = 0,
                durChapterTime = now,
            )?.apply {
                if (addToShelf) {
                    removeType(BookType.notShelf)
                    if (existingBook.isType(BookType.notShelf)) {
                        order = minShelfOrder - 1
                    }
                } else if (existingBook.isType(BookType.notShelf)) {
                    addType(BookType.notShelf)
                }
            }
                ?: incoming.book.copy(
                    latestChapterTitle = mergedChapter.title,
                    latestChapterTime = now,
                    lastCheckTime = now,
                    totalChapterNum = mergedChapterCount,
                    durChapterIndex = mergedChapter.index,
                    durChapterTitle = mergedChapter.title,
                    durChapterPos = 0,
                    durChapterTime = now,
                    order = if (addToShelf) minShelfOrder - 1 else 0,
                ).apply {
                    if (duplicateByNameAuthor != null && duplicateByNameAuthor.bookUrl != bookUrl) {
                        author = "${author} · ${bookUrl.hashCode().toString(16).takeLast(6)}"
                    }
                    if (addToShelf) {
                        removeType(BookType.notShelf)
                    } else {
                        addType(BookType.notShelf)
                    }
                }
        return WenwenBrowserBookMergePlan(
            book = mergedBook,
            chapter = mergedChapter.copy(bookUrl = mergedBook.bookUrl),
        )
    }
}
