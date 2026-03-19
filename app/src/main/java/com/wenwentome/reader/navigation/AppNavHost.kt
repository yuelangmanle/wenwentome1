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
import com.wenwentome.reader.di.AppContainer
import com.wenwentome.reader.core.database.toEntity
import com.wenwentome.reader.core.database.toModel
import com.wenwentome.reader.core.model.BookRecord
import com.wenwentome.reader.core.model.OriginType
import com.wenwentome.reader.data.localbooks.ReaderContent
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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

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
                        chapterRef = state.chapterTitle,
                        progressPercent = progress,
                    )
                },
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

    val observeBook = remember(bookId, appContainer) {
        bookRecordDao.observeById(bookId).map { entity ->
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
                appContainer = appContainer,
            )
        }
    }

    return remember(bookId, appContainer) {
        ReaderViewModel(
            bookId = bookId,
            observeBook = observeBook,
            observeReadingState = observeReadingState,
            observeContent = observeContent,
            updateReadingState = { state -> readingStateDao.upsert(state.toEntity()) },
        )
    }
}

private fun readerContentFlow(
    book: BookRecord?,
    bookId: String,
    observeReadingState: kotlinx.coroutines.flow.Flow<com.wenwentome.reader.core.model.ReadingState?>,
    appContainer: AppContainer,
): kotlinx.coroutines.flow.Flow<ReaderContent> =
    when (book?.originType) {
        OriginType.LOCAL -> observeReadingState.flatMapLatest { state ->
            flow {
                emit(
                    runCatching {
                        appContainer.localBookContentRepository.load(bookId, state?.locator)
                    }.getOrElse { error ->
                        ReaderContent(
                            chapterTitle = book.title,
                            paragraphs = listOf(error.message ?: "正文加载失败"),
                        )
                    }
                )
            }
        }

        OriginType.WEB, OriginType.MIXED -> flowOf(
            ReaderContent(
                chapterTitle = book.title,
                paragraphs = listOf(book.summary ?: "网文正文桥接将在后续任务接入。"),
            )
        )

        null -> flowOf(ReaderContent(chapterTitle = "", paragraphs = emptyList()))
    }
