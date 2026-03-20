package com.wenwentome.reader.navigation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.compose.material3.Text
import com.wenwentome.reader.appProjectInfo
import com.wenwentome.reader.core.model.BookFormat
import com.wenwentome.reader.core.model.OriginType
import com.wenwentome.reader.core.model.ReaderChapter
import com.wenwentome.reader.di.AppContainer
import com.wenwentome.reader.core.database.toEntity
import com.wenwentome.reader.core.database.toModel
import com.wenwentome.reader.feature.discover.AddRemoteBookToShelfUseCase
import com.wenwentome.reader.feature.discover.DiscoverScreen
import com.wenwentome.reader.feature.discover.DiscoverUiState
import com.wenwentome.reader.feature.discover.DiscoverViewModel
import com.wenwentome.reader.feature.discover.ImportSourcesUseCase
import com.wenwentome.reader.feature.discover.SourceManagementScreen
import com.wenwentome.reader.feature.discover.SourceManagementUiState
import com.wenwentome.reader.feature.discover.SourceManagementViewModel
import com.wenwentome.reader.feature.library.LibraryScreen
import com.wenwentome.reader.feature.library.LibraryUiState
import com.wenwentome.reader.feature.library.LibraryViewModel
import com.wenwentome.reader.feature.library.ObserveBookshelfUseCase
import com.wenwentome.reader.feature.reader.BookDetailScreen
import com.wenwentome.reader.feature.reader.ReaderScreen
import com.wenwentome.reader.feature.reader.ReaderUiState
import com.wenwentome.reader.feature.reader.ReaderViewModel
import com.wenwentome.reader.feature.settings.SettingsScreen
import com.wenwentome.reader.feature.settings.ChangelogScreen
import com.wenwentome.reader.feature.settings.ChangelogUiState
import com.wenwentome.reader.feature.settings.ChangelogViewModel
import com.wenwentome.reader.feature.settings.SyncSettingsUiState
import com.wenwentome.reader.feature.settings.SyncSettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val BookDetailRoute = "book/{bookId}"
private const val ReaderRoute = "reader/{bookId}"
private const val DiscoverSourcesRoute = "discover/sources"
private const val SettingsChangelogRoute = "settings/changelog"

@Composable
fun AppNavHost(
    navController: NavHostController,
    paddingValues: PaddingValues,
    appContainer: AppContainer,
) {
    val uriHandler = LocalUriHandler.current
    val libraryViewModel: LibraryViewModel = remember(appContainer) {
        LibraryViewModel(
            observeBookshelf = ObserveBookshelfUseCase.from(appContainer.database.bookRecordDao()),
            importLocalBook = { uri -> appContainer.importLocalBook(uri) },
        )
    }
    val libraryState by libraryViewModel.uiState.collectAsState(initial = LibraryUiState())
    val importScope = rememberCoroutineScope()
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        importScope.launch {
            libraryViewModel.import(uri)
        }
    }
    val settingsViewModel: SyncSettingsViewModel = remember(appContainer) {
        SyncSettingsViewModel(
            configStore = appContainer.syncSettingsConfigStore,
            syncService = appContainer.gitHubSyncRepository,
        )
    }
    val settingsState by settingsViewModel.uiState.collectAsState(initial = SyncSettingsUiState())
    val projectInfo = remember { appProjectInfo() }
    val sourceDefinitionDao = appContainer.database.sourceDefinitionDao()
    val discoverViewModel: DiscoverViewModel = remember(appContainer) {
        DiscoverViewModel(
            sourceBridgeRepository = appContainer.sourceBridgeRepository,
            addRemoteBookToShelf = AddRemoteBookToShelfUseCase(
                sourceBridgeRepository = appContainer.sourceBridgeRepository,
                bookRecordDao = appContainer.database.bookRecordDao(),
                remoteBindingDao = appContainer.database.remoteBindingDao(),
            ),
        )
    }
    val discoverState by discoverViewModel.uiState.collectAsState(initial = DiscoverUiState())
    val sourceManagementViewModel: SourceManagementViewModel = remember(appContainer) {
        SourceManagementViewModel(
            observeSources = sourceDefinitionDao.observeAll().map { list -> list.map { it.toModel() } },
            toggleSourceEnabled = { sourceId -> sourceDefinitionDao.toggleEnabled(sourceId) },
        )
    }
    val sourceManagementState by sourceManagementViewModel.uiState.collectAsState(initial = SourceManagementUiState())
    val importSourcesUseCase = remember(appContainer) {
        ImportSourcesUseCase(
            sourceRuleParser = appContainer.sourceRuleParser,
            sourceDefinitionDao = sourceDefinitionDao,
        )
    }
    val sourceImportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        importScope.launch {
            val rawJson = appContainer.appContext.contentResolver
                .openInputStream(uri)
                ?.bufferedReader()
                ?.use { it.readText() }
                ?: return@launch
            importSourcesUseCase(rawJson)
        }
    }

    NavHost(
        navController = navController,
        startDestination = TopLevelDestination.BOOKSHELF.route,
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
    ) {
        composable(TopLevelDestination.BOOKSHELF.route) {
            LibraryScreen(
                state = libraryState,
                onImportClick = {
                    importLauncher.launch(arrayOf("text/plain", "application/epub+zip"))
                },
                onBookClick = { bookId ->
                    navController.navigate("book/$bookId")
                },
            )
        }
        composable(TopLevelDestination.DISCOVER.route) {
            DiscoverScreen(
                state = discoverState,
                onSearch = discoverViewModel::search,
                onAddToShelf = discoverViewModel::addToShelf,
                onManageSources = { navController.navigate(DiscoverSourcesRoute) },
            )
        }
        composable(DiscoverSourcesRoute) {
            SourceManagementScreen(
                state = sourceManagementState,
                onImportJson = {
                    sourceImportLauncher.launch(arrayOf("application/json", "text/plain"))
                },
                onToggleSource = sourceManagementViewModel::toggleEnabled,
            )
        }
        composable(TopLevelDestination.SETTINGS.route) {
            SettingsScreen(
                state = settingsState,
                projectInfo = projectInfo,
                onStateChange = settingsViewModel::setDraft,
                onSaveConfig = settingsViewModel::saveConfig,
                onPush = settingsViewModel::pushNow,
                onPull = settingsViewModel::pullNow,
                onOpenProject = { uriHandler.openUri(projectInfo.projectUrl) },
                onOpenChangelog = { navController.navigate(SettingsChangelogRoute) },
            )
        }
        composable(SettingsChangelogRoute) {
            val viewModel = remember(appContainer) {
                ChangelogViewModel(repository = appContainer.changelogRepository)
            }
            val state by viewModel.uiState.collectAsState(initial = ChangelogUiState())
            ChangelogScreen(state = state)
        }
        composable(BookDetailRoute) { backStackEntry ->
            val bookId = requireNotNull(backStackEntry.arguments?.getString("bookId"))
            val viewModel = rememberReaderViewModel(bookId = bookId, appContainer = appContainer)
            val state by viewModel.uiState.collectAsState(initial = ReaderUiState())
            val book = state.book
            if (book == null) {
                Text(text = "书籍加载中")
            } else {
                BookDetailScreen(
                    book = book,
                    onReadClick = { navController.navigate("reader/$bookId") },
                    onSyncClick = { navController.navigate(TopLevelDestination.SETTINGS.route) },
                )
            }
        }
        composable(ReaderRoute) { backStackEntry ->
            val bookId = requireNotNull(backStackEntry.arguments?.getString("bookId"))
            val viewModel = rememberReaderViewModel(bookId = bookId, appContainer = appContainer)
            val state by viewModel.uiState.collectAsState(initial = ReaderUiState())
            ReaderScreen(
                state = state,
                onLocatorChanged = { locator, progress ->
                    viewModel.updateLocator(
                        locator = locator,
                        chapterRef = state.chapterRef,
                        progressPercent = progress,
                    )
                },
                onReaderModeChange = viewModel::setReaderMode,
                onThemeChange = { theme ->
                    viewModel.updatePresentation(state.presentation.copy(theme = theme))
                },
                onFontSizeChange = { fontSize ->
                    viewModel.updatePresentation(state.presentation.copy(fontSizeSp = fontSize))
                },
                onLineHeightChange = { lineHeight ->
                    viewModel.updatePresentation(state.presentation.copy(lineHeightMultiplier = lineHeight))
                },
                onBrightnessChange = { brightness ->
                    viewModel.updatePresentation(state.presentation.copy(brightnessPercent = brightness))
                },
                onChapterSelected = viewModel::jumpToChapter,
            )
        }
    }
}

@Composable
private fun rememberReaderViewModel(
    bookId: String,
    appContainer: AppContainer,
): ReaderViewModel {
    val bookRecordDao = appContainer.database.bookRecordDao()
    val readingStateDao = appContainer.database.readingStateDao()
    val localBookContentRepository = appContainer.localBookContentRepository
    val remoteBindingDao = appContainer.database.remoteBindingDao()
    val sourceBridgeRepository = appContainer.sourceBridgeRepository
    val preferencesStore = appContainer.preferencesStore

    val observeBook = remember(bookId, appContainer) {
        bookRecordDao.observeById(bookId).map { entity ->
            entity?.toModel()
        }
    }
    val observeBinding = remember(bookId, appContainer) {
        remoteBindingDao.observeByBookId(bookId).map { entity ->
            entity?.toModel()
        }
    }
    val observeReadingState = remember(bookId, appContainer) {
        readingStateDao.observeByBookId(bookId).map { entity ->
            entity?.toModel()
        }
    }
    val observeContent = remember(bookId, appContainer) {
        observeBook.flatMapLatest { book ->
            readerContentFlow(
                book = book,
                bookId = bookId,
                observeReadingState = observeReadingState,
                localBookContentRepository = localBookContentRepository,
                remoteBindingDao = remoteBindingDao,
                sourceBridgeRepository = sourceBridgeRepository,
            )
        }
    }
    val observeChapters = remember(bookId, appContainer) {
        observeBook.flatMapLatest { book ->
            when (book?.originType) {
                null -> flowOf(emptyList())
                OriginType.LOCAL -> {
                    flow {
                        emit(
                            runCatching {
                                withContext(Dispatchers.IO) {
                                    localBookContentRepository.loadChapters(bookId)
                                }
                            }.getOrElse {
                                fallbackLocalChapters(book)
                            }
                        )
                    }
                }
                OriginType.WEB, OriginType.MIXED ->
                    observeBinding.flatMapLatest { binding ->
                        if (binding == null) {
                            flowOf(emptyList())
                        } else {
                            flow {
                                emit(
                                    runCatching {
                                        withContext(Dispatchers.IO) {
                                            sourceBridgeRepository.fetchToc(
                                                sourceId = binding.sourceId,
                                                remoteBookId = binding.remoteBookId,
                                            ).mapIndexed { index, chapter ->
                                                ReaderChapter(
                                                    chapterRef = chapter.chapterRef,
                                                    title = chapter.title.ifBlank { "章节 ${index + 1}" },
                                                    orderIndex = index,
                                                    sourceType = BookFormat.WEB,
                                                    locatorHint = chapter.chapterRef,
                                                    isLatest = chapter.chapterRef == binding.latestKnownChapterRef,
                                                )
                                            }
                                        }
                                    }.getOrDefault(emptyList())
                                )
                            }
                        }
                    }
            }
        }
    }
    val observeLatestChapterRef = remember(bookId, appContainer) {
        observeBook.flatMapLatest { book ->
            when (book?.originType) {
                OriginType.WEB, OriginType.MIXED ->
                    observeBinding.map { binding ->
                        binding?.latestKnownChapterRef?.takeIf { it.isNotBlank() }
                    }
                else -> flowOf(null)
            }
        }
    }

    return remember(bookId, appContainer) {
        ReaderViewModel(
            bookId = bookId,
            observeBook = observeBook,
            observeReadingState = observeReadingState,
            observeContent = observeContent,
            observeReaderMode = preferencesStore.readerMode,
            observePresentationPrefs = preferencesStore.presentationPrefs,
            observeChapters = observeChapters,
            observeLatestChapterRef = observeLatestChapterRef,
            saveReaderMode = preferencesStore::saveReaderMode,
            savePresentationPrefs = preferencesStore::savePresentationPrefs,
            updateReadingState = { state -> readingStateDao.upsert(state.toEntity()) },
        )
    }
}

private fun fallbackLocalChapters(book: com.wenwentome.reader.core.model.BookRecord?): List<ReaderChapter> =
    when (book?.primaryFormat) {
        BookFormat.TXT ->
            listOf(
                ReaderChapter(
                    chapterRef = "txt-body",
                    title = "正文",
                    orderIndex = 0,
                    sourceType = BookFormat.TXT,
                    locatorHint = "0",
                )
            )
        BookFormat.EPUB, BookFormat.WEB, null -> emptyList()
    }
