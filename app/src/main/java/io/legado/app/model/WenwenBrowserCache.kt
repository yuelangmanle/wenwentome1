package io.legado.app.model

import android.content.Context
import io.legado.app.constant.BookType
import io.legado.app.constant.EventBus
import io.legado.app.constant.IntentAction
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.help.book.BookHelp
import io.legado.app.help.book.isType
import io.legado.app.service.WenwenBrowserCacheService
import io.legado.app.ui.wenwen.WENWEN_BROWSER_BOOK_ORIGIN
import io.legado.app.ui.wenwen.WenwenBrowserBackgroundFetcher
import io.legado.app.ui.wenwen.WenwenBrowserBookBridge
import io.legado.app.ui.wenwen.WenwenBrowserBookMergePlanner
import io.legado.app.ui.wenwen.readWenwenBrowserMetadata
import io.legado.app.utils.postEvent
import io.legado.app.utils.startService
import java.util.concurrent.ConcurrentHashMap

object WenwenBrowserCache {

    val cacheBookMap = ConcurrentHashMap<String, BrowserCacheModel>()
    val successDownloadSet = linkedSetOf<String>()
    val errorDownloadMap = hashMapOf<String, Int>()

    val isRun: Boolean
        get() = cacheBookMap.values.any { !it.isStop() }

    val waitCount: Int
        get() = cacheBookMap.values.sumOf { it.waitCount }

    val onDownloadCount: Int
        get() = cacheBookMap.values.sumOf { it.onDownloadCount }

    val downloadSummary: String
        get() = "网页缓存 下载中:$onDownloadCount|等待中:$waitCount|失败:${errorDownloadMap.count()}|成功:${successDownloadSet.size}"

    fun isBrowserBook(book: Book?): Boolean {
        return book?.origin?.startsWith(WENWEN_BROWSER_BOOK_ORIGIN) == true
    }

    @Synchronized
    fun getOrCreate(book: Book): BrowserCacheModel {
        cacheBookMap[book.bookUrl]?.let {
            it.book = book
            return it
        }
        return BrowserCacheModel(book).also {
            cacheBookMap[book.bookUrl] = it
        }
    }

    fun start(
        context: Context,
        book: Book,
        start: Int,
        end: Int,
    ) {
        if (!isBrowserBook(book)) return
        context.startService<WenwenBrowserCacheService> {
            action = IntentAction.start
            putExtra("bookUrl", book.bookUrl)
            putExtra("start", start)
            putExtra("end", end)
        }
    }

    fun remove(
        context: Context,
        bookUrl: String,
    ) {
        context.startService<WenwenBrowserCacheService> {
            action = IntentAction.remove
            putExtra("bookUrl", bookUrl)
        }
    }

    fun stop(context: Context) {
        if (isRun) {
            context.startService<WenwenBrowserCacheService> {
                action = IntentAction.stop
            }
        }
    }

    fun close() {
        cacheBookMap.values.forEach { it.stop() }
        cacheBookMap.clear()
        successDownloadSet.clear()
        errorDownloadMap.clear()
    }

    suspend fun processBook(bookUrl: String) {
        while (true) {
            val model = cacheBookMap[bookUrl] ?: return
            if (model.isStop()) {
                cacheBookMap.remove(bookUrl)
                postEvent(EventBus.UP_DOWNLOAD, bookUrl)
                return
            }
            val book = appDb.bookDao.getBook(bookUrl) ?: run {
                cacheBookMap.remove(bookUrl)
                return
            }
            model.book = book
            val chapters = appDb.bookChapterDao.getChapterList(bookUrl)
            val metadata = readWenwenBrowserMetadata(book)
            val pendingChapter =
                chapters.firstOrNull { chapter ->
                    chapter.index >= model.startIndex &&
                        model.containsIndex(chapter.index) &&
                        !chapter.isVolume &&
                        !BookHelp.hasContent(book, chapter)
                }
            val fetchIndex = pendingChapter?.index ?: ((chapters.maxOfOrNull { it.index } ?: -1) + 1)
            val requestKey = buildRequestKey(bookUrl, fetchIndex)
            val fetchUrl = pendingChapter?.url ?: metadata?.nextPageUrl
            if (fetchUrl.isNullOrBlank()) {
                model.finish()
                cacheBookMap.remove(bookUrl)
                postEvent(EventBus.UP_DOWNLOAD, bookUrl)
                return
            }
            if (pendingChapter == null && !model.shouldExtendBeyondKnown(chapters.maxOfOrNull { it.index } ?: -1, metadata?.nextPageUrl)) {
                model.finish()
                cacheBookMap.remove(bookUrl)
                postEvent(EventBus.UP_DOWNLOAD, bookUrl)
                return
            }
            val referer =
                pendingChapter
                    ?.let { target ->
                        chapters.lastOrNull { it.index < target.index }?.url
                    }
                    ?: chapters.lastOrNull()?.url
                    ?: metadata?.originalUrl
            model.begin(fetchIndex)
            try {
                val addToShelf = !book.isType(BookType.notShelf)
                val article =
                    WenwenBrowserBackgroundFetcher.fetchArticle(
                        url = fetchUrl,
                        searchQuery = metadata?.searchQuery,
                        referer = referer,
                    ) ?: throw IllegalStateException("未识别到可阅读正文")
                if (chapters.any { it.url == article.url && it.url != pendingChapter?.url }) {
                    throw IllegalStateException("识别到重复章节，已停止继续缓存")
                }
                val incoming = WenwenBrowserBookBridge.createBook(article, addToShelf = addToShelf)
                val tocChapters = WenwenBrowserBookBridge.createTocChapters(article, incoming.book.bookUrl)
                val mergedChapterList =
                    buildList {
                        addAll(chapters)
                        tocChapters.forEach { tocChapter ->
                            if (none { it.url == tocChapter.url }) {
                                add(tocChapter)
                            }
                        }
                    }.sortedBy { it.index }
                val duplicateByNameAuthor =
                    appDb.bookDao.getBook(incoming.book.name, incoming.book.author)
                        ?.takeIf { it.bookUrl != book.bookUrl }
                val merged =
                    WenwenBrowserBookMergePlanner.merge(
                        incoming = incoming,
                        existingBook = book,
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
                BookHelp.saveText(merged.book, merged.chapter, incoming.content)
                successDownloadSet.add(merged.chapter.primaryStr())
                errorDownloadMap.remove(requestKey)
                model.completeSuccess(merged.book, merged.chapter)
            } catch (t: Throwable) {
                errorDownloadMap[requestKey] = (errorDownloadMap[requestKey] ?: 0) + 1
                model.completeError()
                if ((errorDownloadMap[requestKey] ?: 0) >= 3) {
                    model.finish()
                    cacheBookMap.remove(bookUrl)
                    postEvent(EventBus.UP_DOWNLOAD, bookUrl)
                    return
                }
            }
        }
    }

    private fun buildRequestKey(
        bookUrl: String,
        index: Int,
    ): String = "$bookUrl@$index"

    class BrowserCacheModel(var book: Book) {
        private var targetStartIndex: Int = 0
        private var targetEndIndex: Int? = null
        private var keepDownloadingToEnd: Boolean = false
        private var downloadingIndex: Int? = null
        private var stopped = false

        init {
            notifyDownloadChanged()
        }

        val waitCount: Int
            get() {
                if (stopped) return 0
                val waitingStartIndex =
                    downloadingIndex
                        ?.let { maxOf(targetStartIndex, it + 1) }
                        ?: targetStartIndex
                return when {
                    keepDownloadingToEnd && targetEndIndex == null -> 1
                    keepDownloadingToEnd -> {
                        (targetEndIndex!! - waitingStartIndex + 1).coerceAtLeast(0) + 1
                    }
                    targetEndIndex != null -> {
                        (targetEndIndex!! - waitingStartIndex + 1).coerceAtLeast(0)
                    }
                    else -> 0
                }
            }

        val onDownloadCount: Int
            get() = if (downloadingIndex != null) 1 else 0

        fun addDownload(
            start: Int,
            end: Int,
        ) {
            stopped = false
            val hasActiveTarget = keepDownloadingToEnd || targetEndIndex != null || downloadingIndex != null
            targetStartIndex =
                if (hasActiveTarget) {
                    minOf(targetStartIndex, start)
                } else {
                    start
                }
            if (end < 0) {
                keepDownloadingToEnd = true
                targetEndIndex =
                    targetEndIndex
                        ?.let { maxOf(it, start) }
            } else {
                targetEndIndex =
                    maxOf(
                        targetEndIndex ?: Int.MIN_VALUE,
                        end,
                        start,
                    )
            }
            notifyDownloadChanged()
        }

        fun begin(index: Int) {
            downloadingIndex = index
            notifyDownloadChanged()
        }

        fun completeSuccess(
            mergedBook: Book,
            chapter: BookChapter,
        ) {
            book = mergedBook
            targetStartIndex = maxOf(targetStartIndex, chapter.index + 1)
            if (!keepDownloadingToEnd && targetEndIndex != null && targetStartIndex > targetEndIndex!!) {
                targetEndIndex = null
            }
            downloadingIndex = null
            notifyContentSaved(mergedBook, chapter)
            notifyDownloadChanged()
        }

        fun completeError() {
            downloadingIndex = null
            notifyDownloadChanged()
        }

        val startIndex: Int
            get() = targetStartIndex

        fun containsIndex(index: Int): Boolean {
            if (stopped) return false
            if (index < targetStartIndex) return false
            return keepDownloadingToEnd || targetEndIndex == null || index <= targetEndIndex!!
        }

        fun shouldExtendBeyondKnown(
            lastKnownIndex: Int,
            nextPageUrl: String?,
        ): Boolean {
            if (stopped) return false
            if (keepDownloadingToEnd) {
                return !nextPageUrl.isNullOrBlank()
            }
            val target = targetEndIndex ?: return false
            return lastKnownIndex < target && !nextPageUrl.isNullOrBlank()
        }

        fun isStop(): Boolean {
            return stopped || (!keepDownloadingToEnd && targetEndIndex == null && downloadingIndex == null)
        }

        fun stop() {
            stopped = true
            downloadingIndex = null
            targetStartIndex = 0
            keepDownloadingToEnd = false
            targetEndIndex = null
            notifyDownloadChanged()
        }

        fun finish() {
            downloadingIndex = null
            targetStartIndex = 0
            keepDownloadingToEnd = false
            targetEndIndex = null
            stopped = true
        }

        private fun notifyDownloadChanged() {
            runCatching {
                postEvent(EventBus.UP_DOWNLOAD, book.bookUrl)
            }
        }

        private fun notifyContentSaved(
            mergedBook: Book,
            chapter: BookChapter,
        ) {
            runCatching {
                postEvent(EventBus.SAVE_CONTENT, Pair(mergedBook, chapter))
            }
        }
    }
}
