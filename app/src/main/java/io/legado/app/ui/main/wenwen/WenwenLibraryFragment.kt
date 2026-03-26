package io.legado.app.ui.main.wenwen

import android.os.Bundle
import android.view.View
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookGroup
import io.legado.app.help.book.isEpub
import io.legado.app.help.book.isLocal
import io.legado.app.help.book.isNotShelf
import io.legado.app.ui.book.cache.CacheActivity
import io.legado.app.ui.book.import.local.ImportBookActivity
import io.legado.app.ui.book.manage.BookshelfManageActivity
import io.legado.app.ui.main.MainFragmentInterface
import io.legado.app.ui.wenwen.startWenwenBookDetailActivity
import io.legado.app.utils.startActivity
import io.legado.app.utils.startActivityForBook
import com.wenwentome.reader.core.model.BookFormat
import com.wenwentome.reader.core.model.BookRecord
import com.wenwentome.reader.core.model.OriginType
import com.wenwentome.reader.feature.library.LibraryBookItem
import com.wenwentome.reader.feature.library.LibraryFilter
import com.wenwentome.reader.feature.library.LibraryScreen
import com.wenwentome.reader.feature.library.LibrarySort
import com.wenwentome.reader.feature.library.LibraryUiState
import kotlin.math.roundToInt

class WenwenLibraryFragment() : Fragment(), MainFragmentInterface {

    constructor(position: Int) : this() {
        arguments = Bundle().apply {
            putInt("position", position)
        }
    }

    override val position: Int?
        get() = arguments?.getInt("position")

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme {
                    WenwenLibraryRoute()
                }
            }
        }
    }

    @Composable
    private fun WenwenLibraryRoute() {
        val booksFlow = appDb.bookDao.flowByGroup(BookGroup.IdRoot)
        val books by booksFlow.collectAsState(initial = emptyList())
        var selectedFilter by remember { mutableStateOf(LibraryFilter.DEFAULT) }
        var selectedSort by remember { mutableStateOf(LibrarySort.LAST_READ_DESC) }
        val state = remember(books, selectedFilter, selectedSort) {
            buildUiState(books, selectedFilter, selectedSort)
        }
        val bookMap = remember(books) { books.associateBy { it.bookUrl } }

        LibraryScreen(
            state = state,
            onImportClick = {
                requireContext().startActivity<ImportBookActivity>()
            },
            onOpenCacheManager = {
                requireContext().startActivity<CacheActivity>()
            },
            onOpenBatchManage = {
                requireContext().startActivity<BookshelfManageActivity>()
            },
            onContinueReadingClick = { bookUrl ->
                bookMap[bookUrl]?.let { book ->
                    requireContext().startActivityForBook(book)
                }
            },
            onBookClick = { bookUrl ->
                bookMap[bookUrl]?.let { book ->
                    requireContext().startWenwenBookDetailActivity(book)
                }
            },
            onRefreshCatalog = { bookUrl ->
                bookMap[bookUrl]?.let { book ->
                    requireContext().startWenwenBookDetailActivity(book)
                }
            },
            onRefreshCover = { bookUrl ->
                bookMap[bookUrl]?.let { book ->
                    requireContext().startWenwenBookDetailActivity(book)
                }
            },
            onImportPhoto = { bookUrl ->
                bookMap[bookUrl]?.let { book ->
                    requireContext().startWenwenBookDetailActivity(book)
                }
            },
            onRestoreAutomaticCover = { bookUrl ->
                bookMap[bookUrl]?.let { book ->
                    requireContext().startWenwenBookDetailActivity(book)
                }
            },
            onFilterChange = { selectedFilter = it },
            onSortChange = { selectedSort = it },
        )
    }

    private fun buildUiState(
        books: List<Book>,
        filter: LibraryFilter,
        sort: LibrarySort,
    ): LibraryUiState {
        val shelfBooks = books.filterNot { it.isNotShelf }
        val items = shelfBooks.map { it.toLibraryItem() }
        val filteredItems =
            when (filter) {
                LibraryFilter.DEFAULT -> items
                LibraryFilter.LOCAL_ONLY -> items.filter { it.book.originType == OriginType.LOCAL }
                LibraryFilter.WEB_ONLY -> items.filter { it.book.originType == OriginType.WEB }
            }
        val continueReading = items
            .filter { it.lastReadAt > 0L }
            .maxByOrNull { it.lastReadAt }

        return LibraryUiState(
            filter = filter,
            sort = sort,
            continueReading = continueReading,
            visibleBooks = sort.apply(filteredItems),
            isImporting = false,
            importErrorMessage = null,
        )
    }

    private fun Book.toLibraryItem(): LibraryBookItem {
        val progressPercent = if (totalChapterNum > 0) {
            ((durChapterIndex + 1).toFloat() / totalChapterNum.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
        return LibraryBookItem(
            book = BookRecord(
                id = bookUrl,
                title = name,
                author = author,
                originType = if (isLocal) OriginType.LOCAL else OriginType.WEB,
                primaryFormat = when {
                    isEpub -> BookFormat.EPUB
                    isLocal -> BookFormat.TXT
                    else -> BookFormat.WEB
                },
                cover = getDisplayCover(),
                summary = getDisplayIntro(),
            ),
            effectiveCover = getDisplayCover(),
            progressPercent = progressPercent,
            progressLabel = "${(progressPercent * 100).roundToInt()}%",
            hasUpdates = getUnreadChapterNum() > 0,
            canRestoreAutomaticCover = !customCoverUrl.isNullOrBlank(),
            lastReadAt = durChapterTime,
        )
    }
}
