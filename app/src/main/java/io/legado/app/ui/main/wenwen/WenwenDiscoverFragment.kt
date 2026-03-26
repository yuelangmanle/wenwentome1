package io.legado.app.ui.main.wenwen

import android.os.Bundle
import android.view.View
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import com.wenwentome.reader.core.database.datastore.ReaderPreferencesStore
import com.wenwentome.reader.core.model.BrowserFindPreferences
import io.legado.app.ui.book.search.SearchActivity
import io.legado.app.ui.book.source.manage.BookSourceActivity
import io.legado.app.ui.main.MainFragmentInterface
import io.legado.app.utils.startActivity
import io.legado.app.utils.toastOnUi
import com.wenwentome.reader.feature.discover.DiscoverScreen
import com.wenwentome.reader.feature.discover.DiscoverBrowserEngine
import com.wenwentome.reader.feature.discover.DiscoverUiState
import io.legado.app.ui.wenwen.WenwenDiscoverSettingsActivity
import io.legado.app.ui.wenwen.startWenwenBrowserSearch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class WenwenDiscoverFragment() : Fragment(), MainFragmentInterface {

    constructor(position: Int) : this() {
        arguments = Bundle().apply {
            putInt("position", position)
        }
    }

    override val position: Int?
        get() = arguments?.getInt("position")

    private val preferencesStore by lazy {
        ReaderPreferencesStore(requireContext().applicationContext)
    }
    private var browserPrefs: BrowserFindPreferences = BrowserFindPreferences()

    private var uiState by mutableStateOf(
        DiscoverUiState(
            enhancementHint = "支持站内搜索与浏览器找书双模式，浏览器正文可自动识别后切入阅读。",
            lastRefreshHint = "右上角可管理书源、搜索引擎与浏览器模式设置。",
            browserSearchEngineLabel = "必应",
            browserModeLabel = "智能阅读",
            autoOptimizeReadingLabel = "已开启",
        )
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                preferencesStore.browserFindPrefs.collectLatest { prefs ->
                    browserPrefs = prefs
                    uiState =
                        uiState.copy(
                            browserSearchEngineLabel = prefs.activeSearchEngine().label,
                            browserModeLabel = prefs.browserMode.label,
                            autoOptimizeReadingLabel = if (prefs.autoOptimizeReading) "已开启" else "已关闭",
                            browserAvailableEngines =
                                prefs.availableSearchEngines().map {
                                    DiscoverBrowserEngine(id = it.id, label = it.label)
                                },
                        )
                }
            }
        }
    }

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme {
                    DiscoverScreen(
                        state = uiState,
                        onQueryChange = { query ->
                            uiState = uiState.copy(draftQuery = query)
                        },
                        onBrowserQueryChange = { query ->
                            uiState = uiState.copy(browserDraftQuery = query)
                        },
                        onSubmitSearch = {
                            SearchActivity.start(requireContext(), uiState.draftQuery)
                        },
                        onOpenBrowserSearch = {
                            val query = uiState.browserDraftQuery.ifBlank { uiState.draftQuery }
                            if (query.isBlank()) {
                                requireContext().toastOnUi("先输入要搜索的小说名称。")
                            } else {
                                requireContext().startWenwenBrowserSearch(query, browserPrefs)
                            }
                        },
                        onPreview = {},
                        onAddToShelf = {},
                        onRefreshSelected = {},
                        onReadLatest = {
                            SearchActivity.start(requireContext(), uiState.draftQuery)
                        },
                        onManageSources = {
                            requireContext().startActivity<BookSourceActivity>()
                        },
                        onManageSearchEngines = {
                            WenwenDiscoverSettingsActivity.open(
                                requireContext(),
                                WenwenDiscoverSettingsActivity.SECTION_ENGINES,
                            )
                        },
                        onOpenBrowserSettings = {
                            WenwenDiscoverSettingsActivity.open(
                                requireContext(),
                                WenwenDiscoverSettingsActivity.SECTION_BROWSER,
                            )
                        },
                        onQuickSwitchEngine = { engine ->
                            viewLifecycleOwner.lifecycleScope.launch {
                                preferencesStore.saveBrowserFindPrefs(
                                    browserPrefs.copy(defaultSearchEngineId = engine),
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}
