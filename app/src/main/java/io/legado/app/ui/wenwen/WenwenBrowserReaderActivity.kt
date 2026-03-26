package io.legado.app.ui.wenwen

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.lifecycle.lifecycleScope
import io.legado.app.data.appDb
import io.legado.app.help.book.BookHelp
import io.legado.app.help.book.isType
import io.legado.app.ui.browser.WebViewActivity
import io.legado.app.utils.GSON
import io.legado.app.utils.applyWenwenBookCloseTransition
import io.legado.app.utils.startActivityForBook
import io.legado.app.utils.openUrl
import io.legado.app.utils.startActivity
import io.legado.app.utils.toastOnUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WenwenBrowserReaderActivity : AppCompatActivity() {

    private var isSaving by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val article = readArticleFromIntent()
        if (article == null) {
            finish()
            return
        }
        setContent {
            MaterialTheme {
                WenwenBrowserReaderScreen(
                    article = article,
                    isSaving = isSaving,
                    onBackClick = ::finish,
                    onOpenOriginalClick = { openUrl(article.url) },
                    onAddToShelfClick = { persistArticle(article, addToShelf = true, openReader = false) },
                    onOpenReaderClick = { persistArticle(article, addToShelf = false, openReader = true) },
                    onCaptureNextClick = { openNextCapture(article) },
                    onOpenTocChapter = { entry -> openTocChapter(article, entry) },
                )
            }
        }
    }

    private fun readArticleFromIntent(): WenwenBrowserArticle? {
        val url = intent.getStringExtra(EXTRA_URL).orEmpty()
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val content = intent.getStringExtra(EXTRA_CONTENT).orEmpty()
        val sourceLabel = intent.getStringExtra(EXTRA_SOURCE_LABEL).orEmpty()
        val coverUrl = intent.getStringExtra(EXTRA_COVER_URL)
        val searchQuery = intent.getStringExtra(EXTRA_SEARCH_QUERY)
        val nextPageUrl = intent.getStringExtra(EXTRA_NEXT_PAGE_URL)
        if (url.isBlank() || title.isBlank() || content.isBlank()) return null
        return WenwenBrowserArticle(
            url = url,
            title = title,
            content = content,
            sourceLabel = sourceLabel.ifBlank { "网页正文" },
            coverUrl = coverUrl,
            searchQuery = searchQuery,
            nextPageUrl = nextPageUrl,
            tocEntries =
                intent.getStringExtra(EXTRA_TOC_JSON)
                    ?.let { tocJson ->
                        runCatching {
                            GSON.fromJson(tocJson, Array<WenwenBrowserTocEntry>::class.java)
                                ?.toList()
                                .orEmpty()
                        }.getOrNull()
                    }
                    .orEmpty(),
        )
    }

    private fun openNextCapture(article: WenwenBrowserArticle) {
        val nextUrl = article.nextPageUrl
        if (nextUrl.isNullOrBlank()) {
            toastOnUi("当前页面没有识别到下一章入口")
            return
        }
        startActivity<WebViewActivity> {
            putExtra("title", "继续抓取下一章")
            putExtra("url", nextUrl)
            putExtra("sourceName", "浏览器找书 · 智能阅读")
            putExtra(EXTRA_WENWEN_BROWSER_MODE_ENABLED, true)
            putExtra(EXTRA_WENWEN_BROWSER_AUTO_OPTIMIZE, true)
            putExtra(EXTRA_WENWEN_BROWSER_SHOW_MANUAL_BUTTON, true)
            putExtra(EXTRA_WENWEN_BROWSER_SEARCH_QUERY, article.searchQuery)
        }
    }

    private fun openTocChapter(
        article: WenwenBrowserArticle,
        entry: WenwenBrowserTocEntry,
    ) {
        startActivity<WebViewActivity> {
            putExtra("title", entry.title)
            putExtra("url", entry.url)
            putExtra("sourceName", "浏览器找书 · 目录跳章")
            putExtra(EXTRA_WENWEN_BROWSER_MODE_ENABLED, true)
            putExtra(EXTRA_WENWEN_BROWSER_AUTO_OPTIMIZE, true)
            putExtra(EXTRA_WENWEN_BROWSER_SHOW_MANUAL_BUTTON, true)
            putExtra(EXTRA_WENWEN_BROWSER_SEARCH_QUERY, article.searchQuery)
        }
    }

    private fun persistArticle(
        article: WenwenBrowserArticle,
        addToShelf: Boolean,
        openReader: Boolean,
    ) {
        if (isSaving) return
        isSaving = true
        lifecycleScope.launch {
            try {
                val savedBook = withContext(Dispatchers.IO) {
                    val bridge = WenwenBrowserBookBridge.createBook(article, addToShelf)
                    val tocChapters = WenwenBrowserBookBridge.createTocChapters(article, bridge.book.bookUrl)
                    val existing = appDb.bookDao.getBook(bridge.book.bookUrl)
                    val duplicateByNameAuthor =
                        appDb.bookDao.getBook(bridge.book.name, bridge.book.author)
                            ?.takeIf { it.bookUrl != bridge.book.bookUrl }
                    val existingChapters = existing?.let { appDb.bookChapterDao.getChapterList(it.bookUrl) }.orEmpty()
                    val mergedChapterList =
                        buildList {
                            addAll(existingChapters)
                            tocChapters.forEach { tocChapter ->
                                if (none { it.url == tocChapter.url }) {
                                    add(tocChapter)
                                }
                            }
                        }.sortedBy { it.index }
                    val merged =
                        WenwenBrowserBookMergePlanner.merge(
                            incoming = bridge,
                            existingBook = existing,
                            existingChapters = mergedChapterList,
                            duplicateByNameAuthor = duplicateByNameAuthor,
                            addToShelf = addToShelf,
                            minShelfOrder = runCatching { appDb.bookDao.minOrder }.getOrDefault(0),
                        )
                    appDb.bookDao.insert(merged.book)
                    val mergedTocChapters =
                        tocChapters.map { tocChapter ->
                            tocChapter.copy(bookUrl = merged.book.bookUrl)
                        }
                    if (mergedTocChapters.isNotEmpty()) {
                        appDb.bookChapterDao.insert(*mergedTocChapters.toTypedArray())
                    }
                    appDb.bookChapterDao.insert(merged.chapter)
                    BookHelp.saveText(merged.book, merged.chapter, bridge.content)
                    merged.book
                }
                if (addToShelf) {
                    toastOnUi("已加入书架")
                }
                if (openReader) {
                    startActivityForBook(savedBook) {
                        putExtra(
                            "inBookshelf",
                            !savedBook.isType(io.legado.app.constant.BookType.notShelf)
                        )
                        putExtra("chapterChanged", false)
                    }
                }
            } catch (t: Throwable) {
                toastOnUi("保存浏览器正文失败: ${t.localizedMessage ?: "未知错误"}")
            } finally {
                isSaving = false
            }
        }
    }

    companion object {
        private const val EXTRA_URL = "wenwentome.browser_reader.url"
        private const val EXTRA_TITLE = "wenwentome.browser_reader.title"
        private const val EXTRA_CONTENT = "wenwentome.browser_reader.content"
        private const val EXTRA_SOURCE_LABEL = "wenwentome.browser_reader.source_label"
        private const val EXTRA_COVER_URL = "wenwentome.browser_reader.cover_url"
        private const val EXTRA_SEARCH_QUERY = "wenwentome.browser_reader.search_query"
        private const val EXTRA_NEXT_PAGE_URL = "wenwentome.browser_reader.next_page_url"
        private const val EXTRA_TOC_JSON = "wenwentome.browser_reader.toc_json"

        fun start(
            context: Context,
            article: WenwenBrowserArticle,
        ) {
            context.startActivity<WenwenBrowserReaderActivity> {
                putExtra(EXTRA_URL, article.url)
                putExtra(EXTRA_TITLE, article.title)
                putExtra(EXTRA_CONTENT, article.content)
                putExtra(EXTRA_SOURCE_LABEL, article.sourceLabel)
                putExtra(EXTRA_COVER_URL, article.coverUrl)
                putExtra(EXTRA_SEARCH_QUERY, article.searchQuery)
                putExtra(EXTRA_NEXT_PAGE_URL, article.nextPageUrl)
                putExtra(EXTRA_TOC_JSON, GSON.toJson(article.tocEntries))
            }
            (context as? AppCompatActivity)?.overridePendingTransition(
                io.legado.app.R.anim.wenwen_book_open_enter,
                io.legado.app.R.anim.wenwen_book_open_exit,
            )
        }
    }

    override fun finish() {
        super.finish()
        applyWenwenBookCloseTransition()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WenwenBrowserReaderScreen(
    article: WenwenBrowserArticle,
    isSaving: Boolean,
    onBackClick: () -> Unit,
    onOpenOriginalClick: () -> Unit,
    onAddToShelfClick: () -> Unit,
    onOpenReaderClick: () -> Unit,
    onCaptureNextClick: () -> Unit,
    onOpenTocChapter: (WenwenBrowserTocEntry) -> Unit,
) {
    val scrollState = rememberScrollState()
    var showTocSheet by mutableStateOf(false)
    Scaffold(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color(0xFFF5EFD9))
                .safeDrawingPadding(),
        containerColor = Color(0xFFF5EFD9),
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = {
                    Column {
                        Text(
                            text = article.title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = article.sourceLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF7B684B),
                        )
                    }
                },
                navigationIcon = {
                    TextButton(onClick = onBackClick) {
                        Text("返回")
                    }
                },
                actions = {
                    if (article.tocEntries.isNotEmpty()) {
                        TextButton(onClick = { showTocSheet = true }) {
                            Text("目录")
                        }
                    }
                    TextButton(
                        onClick = onAddToShelfClick,
                        enabled = !isSaving,
                    ) {
                        Text(if (isSaving) "处理中" else "加入书架")
                    }
                    TextButton(onClick = onOpenOriginalClick) {
                        Text("原网页")
                    }
                },
            )
        },
    ) { innerPadding ->
        SelectionContainer {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(scrollState)
                        .padding(horizontal = 20.dp, vertical = 18.dp),
            ) {
                Card(
                    colors =
                        CardDefaults.cardColors(
                            containerColor = Color(0xFFFFFBF0),
                        ),
                    shape = RoundedCornerShape(28.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 26.dp),
                    ) {
                        Text(
                            text = article.title,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2C2418),
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = article.sourceLabel,
                                style = MaterialTheme.typography.labelLarge,
                                color = Color(0xFF8C7657),
                            )
                            Text(
                                text = "浏览器优化阅读",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color(0xFFB25D28),
                            )
                        }
                        if (article.tocEntries.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "已识别目录 ${article.tocEntries.size} 章，可直接跳章或整本缓存。",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF7B684B),
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = article.content,
                            style =
                                TextStyle(
                                    fontSize = 20.sp,
                                    lineHeight = 38.sp,
                                    letterSpacing = 0.3.sp,
                                    color = Color(0xFF332A1C),
                                ),
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = onOpenReaderClick,
                                enabled = !isSaving,
                            ) {
                                Text(if (isSaving) "处理中..." else "统一阅读器打开")
                            }
                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = onAddToShelfClick,
                                enabled = !isSaving,
                            ) {
                                Text(if (isSaving) "处理中..." else "仅加入书架")
                            }
                        }
                        if (article.tocEntries.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { showTocSheet = true },
                                enabled = !isSaving,
                            ) {
                                Text(if (isSaving) "处理中..." else "打开目录跳章")
                            }
                        }
                        if (!article.nextPageUrl.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = onCaptureNextClick,
                                enabled = !isSaving,
                            ) {
                                Text(if (isSaving) "处理中..." else "继续抓取下一章")
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
    if (showTocSheet) {
        ModalBottomSheet(
            onDismissRequest = { showTocSheet = false },
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(article.tocEntries, key = { it.url }) { entry ->
                    TextButton(
                        onClick = {
                            showTocSheet = false
                            onOpenTocChapter(entry)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                    ) {
                        Text(entry.title, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}
