package io.legado.app.ui.wenwen

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.runtime.Composable
import androidx.lifecycle.lifecycleScope
import io.legado.app.constant.BookType
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.help.book.addType
import io.legado.app.help.book.isLocal
import io.legado.app.help.book.isNotShelf
import io.legado.app.model.BookCover
import io.legado.app.model.webBook.WebBook
import io.legado.app.utils.startActivityForBook
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WenwenBookDetailActivity : ComponentActivity() {

    private var uiState by mutableStateOf(WenwenBookDetailUiState())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                WenwenBookDetailScreen(
                    state = uiState,
                    onBackClick = ::finish,
                    onReadClick = ::openReader,
                )
            }
        }

        loadBookDetail()
    }

    private fun loadBookDetail() {
        val searchCandidate = WenwenBookSearchCandidate.fromIntent(intent)
        val lookup = WenwenUiBridge.resolveBookLookup(
            bookUrl = intent.getStringExtra(WenwenUiBridge.EXTRA_BOOK_URL),
            name = intent.getStringExtra(WenwenUiBridge.EXTRA_NAME),
            author = intent.getStringExtra(WenwenUiBridge.EXTRA_AUTHOR),
        )

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                val localMatch =
                    when (lookup) {
                    is WenwenBookLookup.ByBookUrl -> {
                        val book = appDb.bookDao.getBook(lookup.bookUrl)
                        book?.let { loadedBook ->
                            WenwenLoadedBookDetail(
                                book = loadedBook,
                                chapters = appDb.bookChapterDao.getChapterList(loadedBook.bookUrl),
                            )
                        }
                    }

                    is WenwenBookLookup.ByNameAuthor -> {
                        val book = appDb.bookDao.getBook(lookup.name, lookup.author)
                        book?.let { loadedBook ->
                            WenwenLoadedBookDetail(
                                book = loadedBook,
                                chapters = appDb.bookChapterDao.getChapterList(loadedBook.bookUrl),
                            )
                        }
                    }

                    WenwenBookLookup.Missing -> null
                }
                val fetchPlan =
                    WenwenBookDetailFetchPlan.resolve(
                        existingBook = localMatch?.book,
                        searchCandidate = searchCandidate,
                    )
                val resolved =
                    if (fetchPlan.refreshCurrentSource) {
                        refreshBookDetailFromCurrentSource(localMatch)
                    } else if (fetchPlan.tryPreciseSearch) {
                        preciseSearchBookDetail(
                            searchCandidate = searchCandidate,
                            localMatch = localMatch,
                        )
                    } else {
                        localMatch
                    }
                resolved ?: localMatch
            }

            uiState = if (result == null) {
                WenwenBookDetailUiState(
                    isLoading = false,
                    errorMessage = "没有找到对应书籍，当前入口没有解析到有效书籍信息。",
                )
            } else {
                val book = result.book
                val currentChapterTitle = result.chapters
                    .firstOrNull { it.index == book.durChapterIndex }
                    ?.title
                    ?: book.durChapterTitle

                WenwenBookDetailUiState(
                    isLoading = false,
                    book = book,
                    chapters = result.chapters,
                    progressPercent = normalizedProgress(book),
                    currentChapterTitle = currentChapterTitle,
                    latestChapterTitle = book.latestChapterTitle,
                )
            }
        }
    }

    private suspend fun refreshBookDetailFromCurrentSource(
        localMatch: WenwenLoadedBookDetail?,
    ): WenwenLoadedBookDetail? {
        val current = localMatch ?: return null
        val book = current.book
        if (book.isLocal) return current
        val source = appDb.bookSourceDao.getBookSource(book.origin) ?: return current
        return runCatching {
            WebBook.getBookInfoAwait(source, book, canReName = false)
            appDb.bookDao.insert(book)
            current.copy(book = book)
        }.getOrElse {
            current
        }
    }

    private suspend fun preciseSearchBookDetail(
        searchCandidate: WenwenBookSearchCandidate?,
        localMatch: WenwenLoadedBookDetail?,
    ): WenwenLoadedBookDetail? {
        val candidate = searchCandidate ?: return null
        val sourceParts = appDb.bookSourceDao.allTextEnabledPart
        for (sourcePart in sourceParts) {
            val source = sourcePart.getBookSource() ?: continue
            val foundBook =
                WebBook.preciseSearchAwait(
                    bookSource = source,
                    name = candidate.name,
                    author = candidate.author,
                ).getOrNull() ?: continue
            return runCatching {
                WebBook.getBookInfoAwait(source, foundBook, canReName = false)
                val localBook = localMatch?.book
                if (localBook != null && localBook.isLocal) {
                    val mergedBook =
                        localBook.copy(
                            name = localBook.name.ifBlank { foundBook.name },
                            author = localBook.author.ifBlank { foundBook.author },
                            coverUrl = foundBook.getDisplayCover(),
                            intro = foundBook.getDisplayIntro(),
                            kind = foundBook.kind,
                            latestChapterTitle = foundBook.latestChapterTitle,
                            totalChapterNum = foundBook.totalChapterNum,
                            lastCheckTime = System.currentTimeMillis(),
                        )
                    appDb.bookDao.insert(mergedBook)
                    WenwenLoadedBookDetail(
                        book = mergedBook,
                        chapters = appDb.bookChapterDao.getChapterList(mergedBook.bookUrl),
                    )
                } else {
                    foundBook.addType(BookType.notShelf)
                    appDb.bookDao.insert(foundBook)
                    WenwenLoadedBookDetail(
                        book = foundBook,
                        chapters = appDb.bookChapterDao.getChapterList(foundBook.bookUrl),
                    )
                }
            }.getOrNull()
        }
        return null
    }

    private fun openReader(book: Book) {
        startActivityForBook(book) {
            putExtra("inBookshelf", !book.isNotShelf)
            putExtra("chapterChanged", false)
        }
    }
}

private data class WenwenLoadedBookDetail(
    val book: Book,
    val chapters: List<BookChapter>,
)

internal data class WenwenBookSearchCandidate(
    val name: String,
    val author: String,
) {
    companion object {
        fun fromIntent(intent: android.content.Intent): WenwenBookSearchCandidate? {
            val name = intent.getStringExtra(WenwenUiBridge.EXTRA_NAME)?.trim().orEmpty()
            val author = intent.getStringExtra(WenwenUiBridge.EXTRA_AUTHOR)?.trim().orEmpty()
            if (name.isBlank() || author.isBlank()) return null
            return WenwenBookSearchCandidate(name = name, author = author)
        }
    }
}

internal data class WenwenBookDetailFetchPlan(
    val refreshCurrentSource: Boolean,
    val tryPreciseSearch: Boolean,
) {
    companion object {
        fun resolve(
            existingBook: Book?,
            searchCandidate: WenwenBookSearchCandidate?,
        ): WenwenBookDetailFetchPlan {
            return when {
                existingBook != null && !existingBook.isLocal ->
                    WenwenBookDetailFetchPlan(
                        refreshCurrentSource = true,
                        tryPreciseSearch = false,
                    )

                searchCandidate != null ->
                    WenwenBookDetailFetchPlan(
                        refreshCurrentSource = false,
                        tryPreciseSearch = true,
                    )

                else ->
                    WenwenBookDetailFetchPlan(
                        refreshCurrentSource = false,
                        tryPreciseSearch = false,
                    )
            }
        }
    }
}

private data class WenwenBookDetailUiState(
    val isLoading: Boolean = true,
    val book: Book? = null,
    val chapters: List<BookChapter> = emptyList(),
    val progressPercent: Float = 0f,
    val currentChapterTitle: String? = null,
    val latestChapterTitle: String? = null,
    val errorMessage: String? = null,
)

private fun normalizedProgress(book: Book): Float {
    val total = book.totalChapterNum.coerceAtLeast(1)
    return (book.durChapterIndex.toFloat() / total.toFloat()).coerceIn(0f, 1f)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WenwenBookDetailScreen(
    state: WenwenBookDetailUiState,
    onBackClick: () -> Unit,
    onReadClick: (Book) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("文文tome") },
                navigationIcon = {
                    TextButton(onClick = onBackClick) {
                        Text("返回")
                    }
                },
            )
        },
        containerColor = Color(0xFFF4EEE6),
    ) { padding ->
        when {
            state.isLoading ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("正在整理书籍详情…", style = MaterialTheme.typography.titleMedium)
                }

            state.book == null ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        state.errorMessage ?: "书籍详情加载失败",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }

            else ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFF4EEE6),
                                    Color(0xFFFDF9F4),
                                )
                            )
                        ),
                    contentPadding = PaddingValues(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item {
                        DetailHero(
                            book = state.book,
                            progressPercent = state.progressPercent,
                            currentChapterTitle = state.currentChapterTitle,
                            onReadClick = { onReadClick(state.book) },
                        )
                    }

                    item {
                        DetailMetaCard(
                            summary = state.book.getDisplayIntro().orEmpty(),
                            latestChapterTitle = state.latestChapterTitle,
                        )
                    }

                    item {
                        Card(
                            shape = RoundedCornerShape(28.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Text(
                                    text = "阅读体验",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = "详情页已支持补全简介、封面与来源信息；点击阅读会进入文文阅读界面，底层继续复用 Legado 的 TXT、EPUB 与网文能力。",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF5F5A54),
                                )
                            }
                        }
                    }

                    item {
                        ChapterPreviewCard(chapters = state.chapters)
                    }
                }
        }
    }
}

@Composable
private fun DetailHero(
    book: Book,
    progressPercent: Float,
    currentChapterTitle: String?,
    onReadClick: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBF6)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(
                    modifier = Modifier
                        .width(108.dp)
                        .height(146.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFFBA8A5A), Color(0xFF6F4E37))
                            )
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    WenwenBookCover(book = book)
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = book.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = book.author.ifBlank { "未知作者" },
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF705F4C),
                    )
                    Text(
                        text = "当前阅读 ${currentChapterTitle ?: "尚未开始"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF5F5A54),
                    )
                }
            }

            LinearProgressIndicator(
                progress = { progressPercent },
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = onReadClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (book.durChapterTime > 0L) "继续阅读" else "开始阅读")
            }
        }
    }
}

@Composable
private fun WenwenBookCover(
    book: Book,
) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            AppCompatImageView(context).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
        },
        update = { imageView ->
            BookCover.load(
                context = imageView.context,
                path = book.getDisplayCover(),
                sourceOrigin = book.origin,
            ).into(imageView)
        },
    )
}

@Composable
private fun DetailMetaCard(
    summary: String,
    latestChapterTitle: String?,
) {
    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "书籍简介",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = if (summary.isBlank()) "暂时还没有同步到简介信息。" else summary,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF5F5A54),
            )
            latestChapterTitle?.takeIf { it.isNotBlank() }?.let {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFFF7F0E8),
                ) {
                    Text(
                        text = "最新章节 $it",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF7A5E44),
                    )
                }
            }
        }
    }
}

@Composable
private fun ChapterPreviewCard(
    chapters: List<BookChapter>,
) {
    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "目录预览",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (chapters.isEmpty()) {
                Text(
                    text = "当前还没有可用目录。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF5F5A54),
                )
            } else {
                chapters.take(8).forEach { chapter ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = chapter.title,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = "#${chapter.index + 1}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF8B7B69),
                        )
                    }
                }
            }
        }
    }
}
