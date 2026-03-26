package io.legado.app.ui.wenwen

import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import android.text.Spanned
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.HtmlCompat
import androidx.lifecycle.lifecycleScope
import io.legado.app.constant.EventBus
import io.legado.app.constant.BookType
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.BookSource
import io.legado.app.help.book.BookHelp
import io.legado.app.help.book.isLocal
import io.legado.app.help.book.removeType
import io.legado.app.help.config.ReadBookConfig
import io.legado.app.model.CacheBook
import io.legado.app.model.localBook.LocalBook
import io.legado.app.model.webBook.WebBook
import io.legado.app.ui.browser.WebViewActivity
import io.legado.app.ui.book.cache.CacheActivity
import io.legado.app.ui.book.changesource.ChangeChapterSourceDialog
import io.legado.app.ui.font.FontSelectDialog
import io.legado.app.utils.applyWenwenBookCloseTransition
import io.legado.app.utils.defaultSharedPreferences
import io.legado.app.utils.observeEvent
import io.legado.app.utils.startActivity
import io.legado.app.utils.toastOnUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WenwenReaderActivity : AppCompatActivity(),
    ChangeChapterSourceDialog.CallBack,
    FontSelectDialog.CallBack {

    private var uiState by mutableStateOf(WenwenReaderUiState())
    private val prefs by lazy { defaultSharedPreferences }

    override val oldBook: Book?
        get() = uiState.book

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        applySavedPresentationPrefs()

        setContent {
            MaterialTheme {
                WenwenReaderScreen(
                    state = uiState,
                    onBackClick = ::finish,
                    onOpenToc = { uiState = uiState.copy(showTocSheet = true) },
                    onCloseToc = { uiState = uiState.copy(showTocSheet = false) },
                    onOpenSettings = { uiState = uiState.copy(showSettingsSheet = true) },
                    onCloseSettings = { uiState = uiState.copy(showSettingsSheet = false) },
                    onOpenActions = { uiState = uiState.copy(showActionSheet = true) },
                    onCloseActions = { uiState = uiState.copy(showActionSheet = false) },
                    onOpenDownload = {
                        uiState = uiState.copy(
                            showActionSheet = false,
                            showDownloadSheet = true,
                        )
                    },
                    onOpenFontDialog = ::openFontDialog,
                    onCloseDownload = { uiState = uiState.copy(showDownloadSheet = false) },
                    onChapterSelected = ::openChapter,
                    onPreviousChapter = ::openPreviousChapter,
                    onNextChapter = ::openNextChapter,
                    onDetailClick = ::openBookDetail,
                    onOpenCacheManager = ::openCacheManager,
                    onOpenChangeSource = ::openChangeSource,
                    onDownloadOptionSelected = ::startDownload,
                    onFontSizeChanged = { updatePresentationPrefs(uiState.copy(fontSizeSp = it)) },
                    onLineHeightChanged = { updatePresentationPrefs(uiState.copy(lineHeightEm = it)) },
                    onLetterSpacingChanged = { updatePresentationPrefs(uiState.copy(letterSpacingEm = it)) },
                    onParagraphSpacingChanged = { updatePresentationPrefs(uiState.copy(paragraphSpacingDp = it)) },
                    onPaletteSelected = { updatePresentationPrefs(uiState.copy(activePalette = it)) },
                )
            }
        }

        initEventObservers()
        loadBook()
    }

    override fun changeTo(
        source: BookSource,
        book: Book,
        toc: List<BookChapter>,
    ) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val currentBook = uiState.book ?: return@withContext
                currentBook.migrateTo(book, toc)
                book.removeType(BookType.updateError)
                currentBook.delete()
                appDb.bookDao.insert(book)
                appDb.bookChapterDao.delByBook(book.bookUrl)
                appDb.bookChapterDao.insert(*toc.toTypedArray())
            }
            loadBook(book.bookUrl)
        }
    }

    override fun replaceContent(content: String) {
        val book = uiState.book ?: return
        val chapter = uiState.chapters.getOrNull(uiState.currentChapterIndex) ?: return
        lifecycleScope.launch(Dispatchers.IO) {
            BookHelp.saveText(book, chapter, content)
        }
        uiState = uiState.copy(
            chapterContent = formatChapterContent(content),
            cachedChapterCount = (uiState.cachedChapterCount + 1).coerceAtMost(uiState.chapters.size),
        )
    }

    override val curFontPath: String
        get() = ReadBookConfig.textFont

    override fun selectFont(path: String) {
        if (path != ReadBookConfig.textFont || path.isEmpty()) {
            ReadBookConfig.textFont = path
            ReadBookConfig.save()
            uiState = uiState.copy(fontLabel = formatFontLabel(path))
            toastOnUi("字体导入配置已保存")
        }
    }

    private fun initEventObservers() {
        observeEvent<String>(EventBus.UP_DOWNLOAD, EventBus.UP_DOWNLOAD_STATE) {
            refreshDownloadStatus()
        }
        observeEvent<Pair<Book, BookChapter>>(EventBus.SAVE_CONTENT) { (book, chapter) ->
            if (book.bookUrl == uiState.book?.bookUrl) {
                val chapterAlreadyCounted = uiState.chapters.getOrNull(uiState.currentChapterIndex)?.url == chapter.url
                uiState = uiState.copy(
                    cachedChapterCount = if (chapterAlreadyCounted) {
                        uiState.cachedChapterCount
                    } else {
                        (uiState.cachedChapterCount + 1).coerceAtMost(uiState.chapters.size)
                    }
                )
                refreshDownloadStatus()
            }
        }
    }

    private fun applySavedPresentationPrefs() {
        val saved = WenwenReaderPrefs.load(prefs)
        uiState = uiState.copy(
            fontSizeSp = saved.fontSizeSp,
            lineHeightEm = saved.lineHeightEm,
            letterSpacingEm = saved.letterSpacingEm,
            paragraphSpacingDp = saved.paragraphSpacingDp,
            activePalette = ReaderPalette.entries.firstOrNull { it.name == saved.paletteName }
                ?: ReaderPalette.Paper,
        )
    }

    private fun updatePresentationPrefs(newState: WenwenReaderUiState) {
        uiState = newState
        WenwenReaderPrefs.save(
            prefs,
            WenwenReaderPresentationPrefs(
                fontSizeSp = newState.fontSizeSp,
                lineHeightEm = newState.lineHeightEm,
                letterSpacingEm = newState.letterSpacingEm,
                paragraphSpacingDp = newState.paragraphSpacingDp,
                paletteName = newState.activePalette.name,
            )
        )
    }

    private fun loadBook(requestedBookUrl: String? = null) {
        val bookUrl = requestedBookUrl ?: intent.getStringExtra(WenwenReaderRouting.EXTRA_BOOK_URL).orEmpty()
        if (bookUrl.isBlank()) {
            uiState = uiState.copy(
                isLoading = false,
                errorMessage = "缺少书籍参数，无法打开阅读页。",
            )
            return
        }

        uiState = uiState.copy(isLoading = true, errorMessage = null)

        lifecycleScope.launch {
            val loadedState = withContext(Dispatchers.IO) {
                val book = appDb.bookDao.getBook(bookUrl) ?: return@withContext null
                val readyBook = prepareBook(book)
                val chapters = appDb.bookChapterDao.getChapterList(readyBook.bookUrl)
                val initialIndex = readyBook.durChapterIndex
                    .coerceIn(0, (chapters.size - 1).coerceAtLeast(0))
                val content = loadChapterContent(readyBook, chapters, initialIndex)
                val cachedChapterCount = countCachedChapters(readyBook, chapters)
                WenwenReaderUiState(
                    isLoading = false,
                    book = readyBook,
                    chapters = chapters,
                    currentChapterIndex = initialIndex,
                    currentChapterTitle = chapters.getOrNull(initialIndex)?.title ?: readyBook.durChapterTitle,
                    chapterContent = content,
                    fontSizeSp = uiState.fontSizeSp,
                    lineHeightEm = uiState.lineHeightEm,
                    letterSpacingEm = uiState.letterSpacingEm,
                    paragraphSpacingDp = uiState.paragraphSpacingDp,
                    activePalette = uiState.activePalette,
                    fontLabel = formatFontLabel(ReadBookConfig.textFont),
                    cachedChapterCount = cachedChapterCount,
                    downloadSummary = buildDownloadSummary(
                        book = readyBook,
                        chapterCount = chapters.size,
                        cachedChapterCount = cachedChapterCount,
                    ),
                )
            }

            uiState = loadedState?.copy(batteryPercent = readBatteryPercent()) ?: WenwenReaderUiState(
                isLoading = false,
                errorMessage = "没有找到这本书，当前入口没有解析到有效书籍信息。",
                batteryPercent = readBatteryPercent(),
            )
        }
    }

    private suspend fun prepareBook(book: Book): Book {
        if (book.isLocal) {
            if (appDb.bookChapterDao.getChapterCount(book.bookUrl) == 0) {
                val chapters = LocalBook.getChapterList(book)
                appDb.bookChapterDao.delByBook(book.bookUrl)
                appDb.bookChapterDao.insert(*chapters.toTypedArray())
                appDb.bookDao.update(book)
            }
            return book
        }

        val source = appDb.bookSourceDao.getBookSource(book.origin) ?: return book
        val oldBook = book.copy()
        if (book.tocUrl.isEmpty()) {
            WebBook.getBookInfoAwait(source, book, canReName = false)
        }
        if (appDb.bookChapterDao.getChapterCount(oldBook.bookUrl) == 0) {
            WebBook.getChapterListAwait(source, book, true)
                .onSuccess { chapterList ->
                    if (oldBook.bookUrl == book.bookUrl) {
                        appDb.bookDao.update(book)
                    } else {
                        appDb.bookDao.replace(oldBook, book)
                        BookHelp.updateCacheFolder(oldBook, book)
                    }
                    appDb.bookChapterDao.delByBook(oldBook.bookUrl)
                    appDb.bookChapterDao.insert(*chapterList.toTypedArray())
                }
                .getOrElse { throw it }
        } else if (oldBook.bookUrl != book.bookUrl) {
            appDb.bookDao.replace(oldBook, book)
        } else {
            appDb.bookDao.update(book)
        }
        return book
    }

    private suspend fun loadChapterContent(
        book: Book,
        chapters: List<BookChapter>,
        index: Int,
    ): String {
        val chapter = chapters.getOrNull(index) ?: return ""
        BookHelp.getContent(book, chapter)?.let {
            return formatChapterContent(it)
        }
        if (book.isLocal) {
            return ""
        }
        val source = appDb.bookSourceDao.getBookSource(book.origin) ?: return ""
        val nextChapterUrl = chapters.getOrNull(index + 1)?.url
        return formatChapterContent(
            WebBook.getContentAwait(
                bookSource = source,
                book = book,
                bookChapter = chapter,
                nextChapterUrl = nextChapterUrl,
                needSave = true,
            )
        )
    }

    private fun openPreviousChapter() {
        openChapter((uiState.currentChapterIndex - 1).coerceAtLeast(0))
    }

    private fun openNextChapter() {
        if (uiState.currentChapterIndex < uiState.chapters.lastIndex) {
            openChapter(uiState.currentChapterIndex + 1)
            return
        }
        val browserMetadata = readWenwenBrowserMetadata(uiState.book)
        if (!browserMetadata?.nextPageUrl.isNullOrBlank()) {
            startActivity<WebViewActivity> {
                putExtra("title", "继续抓取下一章")
                putExtra("url", browserMetadata?.nextPageUrl)
                putExtra("sourceName", "浏览器找书 · 智能阅读")
                putExtra(EXTRA_WENWEN_BROWSER_MODE_ENABLED, true)
                putExtra(EXTRA_WENWEN_BROWSER_AUTO_OPTIMIZE, true)
                putExtra(EXTRA_WENWEN_BROWSER_SHOW_MANUAL_BUTTON, true)
                putExtra(EXTRA_WENWEN_BROWSER_SEARCH_QUERY, browserMetadata?.searchQuery)
            }
            return
        }
        openChapter(uiState.currentChapterIndex.coerceAtLeast(0))
    }

    private fun openChapter(index: Int) {
        val book = uiState.book ?: return
        lifecycleScope.launch {
            val content = withContext(Dispatchers.IO) {
                loadChapterContent(book, uiState.chapters, index)
            }
            val chapter = uiState.chapters.getOrNull(index) ?: return@launch
            uiState = uiState.copy(
                currentChapterIndex = index,
                currentChapterTitle = chapter.title,
                chapterContent = content,
                showTocSheet = false,
                batteryPercent = readBatteryPercent(),
                downloadSummary = buildDownloadSummary(
                    book = book,
                    chapterCount = uiState.chapters.size,
                    cachedChapterCount = uiState.cachedChapterCount,
                ),
            )
            saveReadingProgress(book, chapter, index)
        }
    }

    private fun saveReadingProgress(
        book: Book,
        chapter: BookChapter,
        index: Int,
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            book.durChapterIndex = index
            book.durChapterTitle = chapter.title
            book.durChapterPos = 0
            book.durChapterTime = System.currentTimeMillis()
            book.save()
        }
    }

    private fun openBookDetail() {
        uiState.book?.let { book ->
            startActivity(
                WenwenUiBridge.detailIntent(
                    context = this,
                    bookUrl = book.bookUrl,
                    name = book.name,
                    author = book.author,
                )
            )
        }
    }

    private fun openCacheManager() {
        uiState = uiState.copy(
            showActionSheet = false,
            showDownloadSheet = false,
        )
        startActivity(Intent(this, CacheActivity::class.java))
    }

    private fun openChangeSource() {
        val book = uiState.book ?: return
        if (book.isLocal) {
            toastOnUi("本地书不需要换源")
            return
        }
        uiState = uiState.copy(showActionSheet = false)
        ChangeChapterSourceDialog(
            book.name,
            book.author,
            uiState.currentChapterIndex,
            uiState.currentChapterTitle.orEmpty(),
        ).show(supportFragmentManager, "wenwen-change-chapter-source")
    }

    private fun openFontDialog() {
        FontSelectDialog().show(supportFragmentManager, "wenwen-font-select")
    }

    private fun startDownload(option: DownloadOption) {
        val book = uiState.book ?: return
        if (book.isLocal) {
            toastOnUi("本地书不需要下载缓存")
            uiState = uiState.copy(showDownloadSheet = false)
            return
        }
        val plan = resolveDownloadPlan(book, uiState.currentChapterIndex, uiState.chapters.size, option)
        CacheBook.start(this, book, plan.startIndex, plan.endIndex)
        toastOnUi(plan.message?.let { "已加入下载缓存队列，$it" } ?: "已加入下载缓存队列")
        uiState = uiState.copy(
            showDownloadSheet = false,
            downloadSummary = buildDownloadSummary(
                book = book,
                chapterCount = uiState.chapters.size,
                cachedChapterCount = uiState.cachedChapterCount,
            ),
        )
    }

    private fun refreshDownloadStatus() {
        val book = uiState.book ?: return
        lifecycleScope.launch {
            val status = withContext(Dispatchers.IO) {
                val cached = countCachedChapters(book, uiState.chapters)
                cached to buildDownloadSummary(book, uiState.chapters.size, cached)
            }
            uiState = uiState.copy(
                cachedChapterCount = status.first,
                downloadSummary = status.second,
            )
        }
    }

    private fun countCachedChapters(
        book: Book,
        chapters: List<BookChapter>,
    ): Int {
        if (chapters.isEmpty()) return 0
        return chapters.count { chapter ->
            chapter.isVolume || BookHelp.hasContent(book, chapter)
        }
    }

    private fun buildDownloadSummary(
        book: Book,
        chapterCount: Int,
        cachedChapterCount: Int,
    ): String {
        val waiting = CacheBook.waitCount(book.bookUrl)
        val downloading = CacheBook.onDownloadCount(book.bookUrl)
        val success = CacheBook.successCount(book.bookUrl)
        val failed = CacheBook.errorCount(book.bookUrl)
        return if (waiting == 0 && downloading == 0 && success == 0 && failed == 0) {
            "已缓存 ${cachedChapterCount}/${chapterCount.coerceAtLeast(1)} 章"
        } else {
            "已缓存 ${cachedChapterCount}/${chapterCount.coerceAtLeast(1)} 章 · 下载中 $downloading · 等待 $waiting · 失败 $failed · 成功 $success"
        }
    }

    private fun readBatteryPercent(): Int {
        val batteryStatus = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        if (level <= 0 || scale <= 0) return 0
        return (level * 100 / scale).coerceIn(0, 100)
    }

    private fun formatFontLabel(path: String): String {
        if (path.isBlank()) return "系统字体"
        return path.substringAfterLast('/').ifBlank { "自定义字体" }
    }

    override fun finish() {
        super.finish()
        applyWenwenBookCloseTransition()
    }
}

private data class WenwenReaderUiState(
    val isLoading: Boolean = true,
    val book: Book? = null,
    val chapters: List<BookChapter> = emptyList(),
    val currentChapterIndex: Int = 0,
    val currentChapterTitle: String? = null,
    val chapterContent: String = "",
    val fontSizeSp: Float = 20f,
    val lineHeightEm: Float = 1.75f,
    val letterSpacingEm: Float = 0.02f,
    val paragraphSpacingDp: Float = 14f,
    val activePalette: ReaderPalette = ReaderPalette.Paper,
    val fontLabel: String = "系统字体",
    val batteryPercent: Int = 0,
    val cachedChapterCount: Int = 0,
    val downloadSummary: String = "",
    val showTocSheet: Boolean = false,
    val showSettingsSheet: Boolean = false,
    val showActionSheet: Boolean = false,
    val showDownloadSheet: Boolean = false,
    val errorMessage: String? = null,
)

internal data class WenwenReaderActionAvailability(
    val showDownloadShortcut: Boolean,
    val showDownloadEntry: Boolean,
    val showChangeSourceEntry: Boolean,
) {
    companion object {
        fun fromBook(book: Book?): WenwenReaderActionAvailability {
            val isBrowserCapturedBook =
                book?.origin?.startsWith(WENWEN_BROWSER_BOOK_ORIGIN) == true
            val isOnlineBook = book != null && !book.isLocal
            return WenwenReaderActionAvailability(
                showDownloadShortcut = isOnlineBook,
                showDownloadEntry = isOnlineBook,
                showChangeSourceEntry = isOnlineBook && !isBrowserCapturedBook,
            )
        }
    }
}

internal data class WenwenReaderDownloadPlan(
    val startIndex: Int,
    val endIndex: Int,
    val message: String? = null,
)

internal fun resolveDownloadPlan(
    book: Book,
    currentChapterIndex: Int,
    chapterCount: Int,
    option: DownloadOption,
): WenwenReaderDownloadPlan {
    val isBrowserCapturedBook = book.origin.startsWith(WENWEN_BROWSER_BOOK_ORIGIN)
    if (isBrowserCapturedBook) {
        val catalogChapterCount = readWenwenBrowserMetadata(book)?.tocEntries?.size ?: 0
        if (catalogChapterCount > 0) {
            val lastIndex = (maxOf(chapterCount, catalogChapterCount) - 1).coerceAtLeast(0)
            return when (option) {
                DownloadOption.Next50 ->
                    WenwenReaderDownloadPlan(
                        startIndex = (currentChapterIndex + 1).coerceAtLeast(0),
                        endIndex = (currentChapterIndex + 50).coerceAtMost(lastIndex),
                    )

                DownloadOption.Next100 ->
                    WenwenReaderDownloadPlan(
                        startIndex = (currentChapterIndex + 1).coerceAtLeast(0),
                        endIndex = (currentChapterIndex + 100).coerceAtMost(lastIndex),
                    )

                DownloadOption.ToEnd ->
                    WenwenReaderDownloadPlan(
                        startIndex = (currentChapterIndex + 1).coerceAtLeast(0),
                        endIndex = lastIndex,
                    )

                DownloadOption.FullBook ->
                    WenwenReaderDownloadPlan(
                        startIndex = 0,
                        endIndex = lastIndex,
                        message = "浏览器找书已按识别目录整本缓存",
                    )
            }
        }
        val nextStartIndex = (currentChapterIndex + 1).coerceAtLeast(0)
        return when (option) {
            DownloadOption.Next50 -> WenwenReaderDownloadPlan(nextStartIndex, currentChapterIndex + 50)
            DownloadOption.Next100 -> WenwenReaderDownloadPlan(nextStartIndex, currentChapterIndex + 100)
            DownloadOption.ToEnd -> WenwenReaderDownloadPlan(nextStartIndex, -1)
            DownloadOption.FullBook ->
                WenwenReaderDownloadPlan(
                    startIndex = nextStartIndex,
                    endIndex = -1,
                    message = "浏览器找书暂按当前进度向后缓存到结尾处理",
                )
        }
    }
    val lastIndex = (chapterCount - 1).coerceAtLeast(0)
    return when (option) {
        DownloadOption.Next50 ->
            WenwenReaderDownloadPlan(
                startIndex = currentChapterIndex,
                endIndex = (currentChapterIndex + 50).coerceAtMost(lastIndex),
            )

        DownloadOption.Next100 ->
            WenwenReaderDownloadPlan(
                startIndex = currentChapterIndex,
                endIndex = (currentChapterIndex + 100).coerceAtMost(lastIndex),
            )

        DownloadOption.ToEnd ->
            WenwenReaderDownloadPlan(
                startIndex = currentChapterIndex,
                endIndex = lastIndex,
            )

        DownloadOption.FullBook ->
            WenwenReaderDownloadPlan(
                startIndex = 0,
                endIndex = lastIndex,
            )
    }
}

enum class ReaderPalette(
    val label: String,
    val background: Color,
    val card: Color,
    val text: Color,
    val muted: Color,
) {
    Paper(
        label = "纸感米白",
        background = Color(0xFFF5EEDF),
        card = Color(0xFFF9F4EA),
        text = Color(0xFF2C241B),
        muted = Color(0xFF766553),
    ),
    Moss(
        label = "雾绿纸页",
        background = Color(0xFFE9F0E7),
        card = Color(0xFFF4F8F1),
        text = Color(0xFF243126),
        muted = Color(0xFF607166),
    ),
    NightInk(
        label = "墨夜深蓝",
        background = Color(0xFF171C24),
        card = Color(0xFF202734),
        text = Color(0xFFE7EDF8),
        muted = Color(0xFF95A2B8),
    ),
}

internal enum class DownloadOption(
    val label: String,
    val description: String,
) {
    Next50("下载后 50 章", "继续阅读附近优先缓存"),
    Next100("下载后 100 章", "更适合长时间离线阅读"),
    ToEnd("下载后全部", "从当前章节一直缓存到结尾"),
    FullBook("下载整本书", "从第一章开始完整缓存"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WenwenReaderScreen(
    state: WenwenReaderUiState,
    onBackClick: () -> Unit,
    onOpenToc: () -> Unit,
    onCloseToc: () -> Unit,
    onOpenSettings: () -> Unit,
    onCloseSettings: () -> Unit,
    onOpenActions: () -> Unit,
    onCloseActions: () -> Unit,
    onOpenDownload: () -> Unit,
    onOpenFontDialog: () -> Unit,
    onCloseDownload: () -> Unit,
    onChapterSelected: (Int) -> Unit,
    onPreviousChapter: () -> Unit,
    onNextChapter: () -> Unit,
    onDetailClick: () -> Unit,
    onOpenCacheManager: () -> Unit,
    onOpenChangeSource: () -> Unit,
    onDownloadOptionSelected: (DownloadOption) -> Unit,
    onFontSizeChanged: (Float) -> Unit,
    onLineHeightChanged: (Float) -> Unit,
    onLetterSpacingChanged: (Float) -> Unit,
    onParagraphSpacingChanged: (Float) -> Unit,
    onPaletteSelected: (ReaderPalette) -> Unit,
) {
    val palette = state.activePalette
    val actionAvailability = WenwenReaderActionAvailability.fromBook(state.book)
    val scrollState = rememberScrollState()
    val chapterProgress = calculateChapterProgress(
        hasContent = state.chapterContent.isNotBlank(),
        scrollValue = scrollState.value,
        maxScrollValue = scrollState.maxValue,
    )
    val bookProgress = calculateBookProgress(
        chapterIndex = state.currentChapterIndex,
        chapterCount = state.chapters.size,
        chapterProgress = chapterProgress,
    )

    LaunchedEffect(state.book?.bookUrl, state.currentChapterIndex) {
        scrollState.scrollTo(0)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = palette.background,
        contentColor = palette.text,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("正在整理正文…", style = MaterialTheme.typography.titleMedium)
                    }
                }

                state.book == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            state.errorMessage ?: "正文加载失败",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }

                else -> {
                    Scaffold(
                        containerColor = Color.Transparent,
                        contentColor = palette.text,
                        topBar = {
                            TopAppBar(
                                modifier = Modifier.statusBarsPadding(),
                                title = {
                                    Column {
                                        Text(
                                            text = state.book.name.ifBlank { "文文tome" },
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = state.currentChapterTitle ?: "未命名章节",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = palette.muted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                },
                                navigationIcon = {
                                    TextButton(onClick = onBackClick) {
                                        Text("返回", color = palette.text)
                                    }
                                },
                                actions = {
                                    TextButton(onClick = onOpenToc) {
                                        Text("目录", color = palette.text)
                                    }
                                    TextButton(onClick = onOpenSettings) {
                                        Text("排版", color = palette.text)
                                    }
                                    if (actionAvailability.showDownloadShortcut) {
                                        TextButton(onClick = onOpenDownload) {
                                            Text("下载", color = palette.text)
                                        }
                                    }
                                    TextButton(onClick = onOpenActions) {
                                        Text("更多", color = palette.text)
                                    }
                                },
                            )
                        },
                        bottomBar = {
                            ReaderBottomBar(
                                modifier = Modifier.navigationBarsPadding(),
                                palette = palette,
                                bookProgress = bookProgress,
                                chapterProgress = chapterProgress,
                                batteryPercent = state.batteryPercent,
                                onPreviousChapter = onPreviousChapter,
                                onNextChapter = onNextChapter,
                            )
                        },
                    ) { padding ->
                        ReaderContent(
                            state = state,
                            scrollState = scrollState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding)
                                .safeDrawingPadding(),
                        )
                    }
                }
            }

            if (state.showTocSheet && state.book != null) {
                ModalBottomSheet(
                    onDismissRequest = onCloseToc,
                    containerColor = palette.card,
                ) {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        item {
                            Text(
                                text = "目录",
                                style = MaterialTheme.typography.titleLarge,
                                color = palette.text,
                            )
                        }
                        itemsIndexed(state.chapters) { index, chapter ->
                            val selected = index == state.currentChapterIndex
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onChapterSelected(index) },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selected) palette.background else palette.card,
                                ),
                                shape = RoundedCornerShape(18.dp),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = chapter.title,
                                        modifier = Modifier.weight(1f),
                                        color = palette.text,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    if (selected) {
                                        Text(
                                            text = "当前",
                                            color = palette.muted,
                                            style = MaterialTheme.typography.labelMedium,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (state.showSettingsSheet) {
                ModalBottomSheet(
                    onDismissRequest = onCloseSettings,
                    containerColor = palette.card,
                ) {
                    ReaderSettingsSheet(
                        state = state,
                        palette = palette,
                        onOpenFontDialog = onOpenFontDialog,
                        onFontSizeChanged = onFontSizeChanged,
                        onLineHeightChanged = onLineHeightChanged,
                        onLetterSpacingChanged = onLetterSpacingChanged,
                        onParagraphSpacingChanged = onParagraphSpacingChanged,
                        onPaletteSelected = onPaletteSelected,
                    )
                }
            }

            if (state.showActionSheet && state.book != null) {
                ModalBottomSheet(
                    onDismissRequest = onCloseActions,
                    containerColor = palette.card,
                ) {
                    ReaderActionSheet(
                        palette = palette,
                        availability = actionAvailability,
                        onDetailClick = onDetailClick,
                        onDownloadClick = onOpenDownload,
                        onCacheManagerClick = onOpenCacheManager,
                        onChangeSourceClick = onOpenChangeSource,
                    )
                }
            }

            if (state.showDownloadSheet && state.book != null) {
                ModalBottomSheet(
                    onDismissRequest = onCloseDownload,
                    containerColor = palette.card,
                ) {
                    ReaderDownloadSheet(
                        palette = palette,
                        options = DownloadOption.entries,
                        onOptionSelected = onDownloadOptionSelected,
                        onCacheManagerClick = onOpenCacheManager,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReaderContent(
    state: WenwenReaderUiState,
    scrollState: androidx.compose.foundation.ScrollState,
    modifier: Modifier = Modifier,
) {
    val palette = state.activePalette
    val paragraphs = splitParagraphs(state.chapterContent)

    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(state.paragraphSpacingDp.dp),
    ) {
        LinearProgressIndicator(
            progress = { normalizedBookProgress(state) },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = state.currentChapterTitle ?: "未命名章节",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
            color = palette.text,
        )
        if (state.downloadSummary.isNotBlank()) {
            Text(
                text = state.downloadSummary,
                style = MaterialTheme.typography.bodySmall,
                color = palette.muted,
            )
        }
        if (paragraphs.isEmpty()) {
            Text(
                text = "这一章暂时没有正文内容。",
                style = MaterialTheme.typography.bodyLarge,
                color = palette.muted,
            )
        } else {
            paragraphs.forEach { paragraph ->
                Text(
                    text = paragraph,
                    style = TextStyle(
                        fontSize = state.fontSizeSp.sp,
                        lineHeight = (state.fontSizeSp * state.lineHeightEm).sp,
                        letterSpacing = state.letterSpacingEm.sp,
                        color = palette.text,
                    ),
                )
            }
        }
    }
}

@Composable
private fun ReaderBottomBar(
    modifier: Modifier,
    palette: ReaderPalette,
    bookProgress: Float,
    chapterProgress: Float,
    batteryPercent: Int,
    onPreviousChapter: () -> Unit,
    onNextChapter: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.background.copy(alpha = 0.94f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onPreviousChapter) {
            Text("上一章", color = palette.text)
        }
        Text(
            text = "电量 ${batteryPercent}%  ·  全书 ${(bookProgress * 100).toInt()}%  ·  本章 ${(chapterProgress * 100).toInt()}%",
            color = palette.muted,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
        )
        TextButton(onClick = onNextChapter) {
            Text("下一章", color = palette.text)
        }
    }
}

@Composable
private fun ReaderActionSheet(
    palette: ReaderPalette,
    availability: WenwenReaderActionAvailability,
    onDetailClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onCacheManagerClick: () -> Unit,
    onChangeSourceClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "阅读操作",
            style = MaterialTheme.typography.titleLarge,
            color = palette.text,
        )
        ReaderActionItem(
            title = "书籍详情",
            description = "进入详情页查看简介、封面和书籍信息",
            palette = palette,
            onClick = onDetailClick,
        )
        if (availability.showDownloadEntry) {
            ReaderActionItem(
                title = "下载缓存",
                description = "将后续章节加入离线缓存队列",
                palette = palette,
                onClick = onDownloadClick,
            )
        }
        if (availability.showChangeSourceEntry) {
            ReaderActionItem(
                title = "换源",
                description = "当前章节或后续阅读异常时切换可用书源",
                palette = palette,
                onClick = onChangeSourceClick,
            )
        }
        ReaderActionItem(
            title = "缓存管理",
            description = "打开统一缓存管理页查看网络书下载情况",
            palette = palette,
            onClick = onCacheManagerClick,
        )
    }
}

@Composable
private fun ReaderDownloadSheet(
    palette: ReaderPalette,
    options: List<DownloadOption>,
    onOptionSelected: (DownloadOption) -> Unit,
    onCacheManagerClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "下载缓存",
            style = MaterialTheme.typography.titleLarge,
            color = palette.text,
        )
        options.forEach { option ->
            ReaderActionItem(
                title = option.label,
                description = option.description,
                palette = palette,
                onClick = { onOptionSelected(option) },
            )
        }
        ReaderActionItem(
            title = "打开缓存管理",
            description = "跳转到统一缓存页查看和管理下载任务",
            palette = palette,
            onClick = onCacheManagerClick,
        )
    }
}

@Composable
private fun ReaderActionItem(
    title: String,
    description: String,
    palette: ReaderPalette,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = palette.background),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(title, color = palette.text, style = MaterialTheme.typography.titleMedium)
            Text(description, color = palette.muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ReaderSettingsSheet(
    state: WenwenReaderUiState,
    palette: ReaderPalette,
    onOpenFontDialog: () -> Unit,
    onFontSizeChanged: (Float) -> Unit,
    onLineHeightChanged: (Float) -> Unit,
    onLetterSpacingChanged: (Float) -> Unit,
    onParagraphSpacingChanged: (Float) -> Unit,
    onPaletteSelected: (ReaderPalette) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text(
            text = "阅读排版",
            style = MaterialTheme.typography.titleLarge,
            color = palette.text,
        )
        ReaderSliderRow(
            title = "字号",
            value = state.fontSizeSp,
            valueText = "%.1fsp".format(state.fontSizeSp),
            valueRange = 12f..36f,
            onValueChange = onFontSizeChanged,
            palette = palette,
        )
        ReaderSliderRow(
            title = "行距",
            value = state.lineHeightEm,
            valueText = "%.2f".format(state.lineHeightEm),
            valueRange = 1.2f..2.4f,
            onValueChange = onLineHeightChanged,
            palette = palette,
        )
        ReaderSliderRow(
            title = "字距",
            value = state.letterSpacingEm,
            valueText = "%.2f".format(state.letterSpacingEm),
            valueRange = -0.05f..0.20f,
            onValueChange = onLetterSpacingChanged,
            palette = palette,
        )
        ReaderSliderRow(
            title = "段间距",
            value = state.paragraphSpacingDp,
            valueText = "%.0fdp".format(state.paragraphSpacingDp),
            valueRange = 0f..32f,
            onValueChange = onParagraphSpacingChanged,
            palette = palette,
        )
        Text(
            text = "阅读背景调色盘",
            style = MaterialTheme.typography.titleMedium,
            color = palette.text,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ReaderPalette.entries.forEach { item ->
                FilterChip(
                    selected = state.activePalette == item,
                    onClick = { onPaletteSelected(item) },
                    label = { Text(item.label) },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(item.background, CircleShape),
                        )
                    },
                )
            }
        }
        ReaderActionItem(
            title = "导入字体",
            description = "当前：${state.fontLabel}",
            palette = palette,
            onClick = onOpenFontDialog,
        )
        Text(
            text = "自动适配和更深的字体渲染继续复用 Legado 的字体配置链路。",
            color = palette.muted,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun ReaderSliderRow(
    title: String,
    value: Float,
    valueText: String,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    palette: ReaderPalette,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, color = palette.text, style = MaterialTheme.typography.titleMedium)
            Text(valueText, color = palette.muted, style = MaterialTheme.typography.bodySmall)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
        )
    }
}

internal fun calculateBookProgress(
    chapterIndex: Int,
    chapterCount: Int,
    chapterProgress: Float,
): Float {
    if (chapterCount <= 0) return 0f
    val safeIndex = chapterIndex.coerceIn(0, chapterCount - 1)
    val safeProgress = chapterProgress.coerceIn(0f, 1f)
    return ((safeIndex + safeProgress) / chapterCount.toFloat()).coerceIn(0f, 1f)
}

internal fun calculateChapterProgress(
    hasContent: Boolean,
    scrollValue: Int,
    maxScrollValue: Int,
): Float {
    if (!hasContent) return 0f
    if (maxScrollValue <= 0) return 1f
    return (scrollValue.toFloat() / maxScrollValue.toFloat()).coerceIn(0f, 1f)
}

private fun splitParagraphs(content: String): List<String> {
    return content.split("\n")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
}

private fun formatChapterContent(content: String?): String {
    if (content.isNullOrBlank()) return ""
    val spanned: Spanned = HtmlCompat.fromHtml(content, HtmlCompat.FROM_HTML_MODE_LEGACY)
    return spanned.toString()
}
