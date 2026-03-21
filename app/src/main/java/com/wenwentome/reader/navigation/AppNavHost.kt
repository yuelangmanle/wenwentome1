package com.wenwentome.reader.navigation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.compose.material3.Text
import com.wenwentome.reader.appProjectInfo
import com.wenwentome.reader.core.database.entity.BookAssetEntity
import com.wenwentome.reader.core.model.BookFormat
import com.wenwentome.reader.core.model.AssetRole
import com.wenwentome.reader.core.model.BookRecord
import com.wenwentome.reader.core.model.OriginType
import com.wenwentome.reader.core.model.ReaderChapter
import com.wenwentome.reader.di.AppContainer
import com.wenwentome.reader.core.database.toEntity
import com.wenwentome.reader.core.database.toModel
import com.wenwentome.reader.feature.discover.AddRemoteBookToShelfUseCase
import com.wenwentome.reader.feature.discover.DiscoverEvent
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
import com.wenwentome.reader.feature.reader.BookDetailEvent
import com.wenwentome.reader.feature.reader.BookDetailScreen
import com.wenwentome.reader.feature.reader.BookDetailUiState
import com.wenwentome.reader.feature.reader.BookDetailViewModel
import com.wenwentome.reader.feature.reader.ReaderScreen
import com.wenwentome.reader.feature.reader.ReaderUiState
import com.wenwentome.reader.feature.reader.ReaderViewModel
import com.wenwentome.reader.feature.settings.SettingsScreen
import com.wenwentome.reader.feature.settings.ChangelogScreen
import com.wenwentome.reader.feature.settings.ChangelogUiState
import com.wenwentome.reader.feature.settings.ChangelogViewModel
import com.wenwentome.reader.feature.settings.SyncSettingsUiState
import com.wenwentome.reader.feature.settings.SyncSettingsViewModel
import com.wenwentome.reader.data.localbooks.LocalBookFileStore
import com.wenwentome.reader.data.localbooks.ParsedAsset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest

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
    val readingStateDao = appContainer.database.readingStateDao()
    val remoteBindingDao = appContainer.database.remoteBindingDao()
    val libraryViewModel: LibraryViewModel = remember(appContainer) {
        LibraryViewModel(
            observeBookshelf = ObserveBookshelfUseCase.from(
                bookRecordDao = appContainer.database.bookRecordDao(),
                readingStateDao = readingStateDao,
                remoteBindingDao = remoteBindingDao,
                bookAssetDao = appContainer.database.bookAssetDao(),
            ),
            importLocalBook = { uri -> appContainer.importLocalBook(uri) },
            refreshCatalogAction = { bookId ->
                appContainer.refreshRemoteBook(bookId)
                Unit
            },
        )
    }
    val libraryState by libraryViewModel.uiState.collectAsState(initial = LibraryUiState())
    val importScope = rememberCoroutineScope()
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        libraryViewModel.import(uri)
    }
    var coverImportTargetBookId by remember { mutableStateOf<String?>(null) }
    val coverPickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            val bookId = coverImportTargetBookId
            if (uri == null || bookId == null) {
                coverImportTargetBookId = null
                return@rememberLauncherForActivityResult
            }
            importScope.launch {
                val bytes = withContext(Dispatchers.IO) {
                    appContainer.appContext.contentResolver.openInputStream(uri)?.use { input ->
                        input.readBytes()
                    }
                } ?: return@launch
                val mime = appContainer.appContext.contentResolver.getType(uri) ?: "image/jpeg"
                importManualCover(
                    bookId = bookId,
                    bytes = bytes,
                    mime = mime,
                    appContainer = appContainer,
                )
                coverImportTargetBookId = null
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
                remoteBindingDao = remoteBindingDao,
            ),
            resolveShelfBookId = { result ->
                remoteBindingDao.getByRemoteBook(result.sourceId, result.id)?.bookId
            },
            refreshRemoteBook = appContainer.refreshRemoteBook::invoke,
            loadReadingState = { bookId ->
                readingStateDao.observeByBookId(bookId).first()?.toModel()
            },
            updateReadingState = { state ->
                readingStateDao.upsert(state.toEntity())
            },
            ioDispatcher = appContainer.discoverIoDispatcher,
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
                onContinueReadingClick = { bookId ->
                    navController.navigate("reader/$bookId")
                },
                onBookClick = { bookId ->
                    navController.navigate("book/$bookId")
                },
                onImportPhoto = { bookId ->
                    coverImportTargetBookId = bookId
                    coverPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onRefreshCover = { bookId ->
                    importScope.launch {
                        refreshBookCover(bookId = bookId, appContainer = appContainer)
                    }
                },
                onRestoreAutomaticCover = { bookId ->
                    importScope.launch {
                        restoreAutomaticCover(bookId = bookId, appContainer = appContainer)
                    }
                },
                onRefreshCatalog = { bookId ->
                    libraryViewModel.refreshCatalog(bookId)
                },
            )
        }
        composable(TopLevelDestination.DISCOVER.route) {
            LaunchedEffect(discoverViewModel) {
                discoverViewModel.events.collectLatest { event ->
                    when (event) {
                        is DiscoverEvent.OpenBookDetail -> navController.navigate("book/${event.bookId}")
                        is DiscoverEvent.OpenReader -> navController.navigate("reader/${event.bookId}")
                    }
                }
            }
            DiscoverScreen(
                state = discoverState,
                onSearch = discoverViewModel::search,
                onPreview = discoverViewModel::selectResult,
                onAddToShelf = discoverViewModel::addToShelf,
                onRefreshSelected = discoverViewModel::refreshSelected,
                onReadLatest = discoverViewModel::readLatest,
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
            val viewModel = rememberBookDetailViewModel(bookId = bookId, appContainer = appContainer)
            val state by viewModel.uiState.collectAsState(initial = BookDetailUiState())

            LaunchedEffect(viewModel) {
                viewModel.events.collectLatest { event ->
                    when (event) {
                        is BookDetailEvent.OpenReader -> navController.navigate("reader/${event.bookId}")
                    }
                }
            }

            if (state.book == null) {
                Text(text = "书籍加载中")
            } else {
                BookDetailScreen(
                    state = state,
                    onReadClick = viewModel::openReader,
                    onToggleCatalog = {},
                    onChapterClick = viewModel::openChapter,
                    onRefreshCatalogClick = viewModel::refreshCatalog,
                    onJumpToLatestClick = viewModel::jumpToLatest,
                    onRefreshCoverClick = viewModel::refreshCover,
                    onImportPhotoClick = {
                        coverImportTargetBookId = bookId
                        coverPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onRestoreAutomaticCoverClick = viewModel::restoreAutomaticCover,
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
    val readingStateDao = appContainer.database.readingStateDao()
    val preferencesStore = appContainer.preferencesStore
    val observeBook = rememberObserveBook(bookId = bookId, appContainer = appContainer)
    val observeBinding = rememberObserveBinding(bookId = bookId, appContainer = appContainer)
    val observeReadingState = rememberObserveReadingState(bookId = bookId, appContainer = appContainer)
    val observeContent = rememberObserveContent(
        bookId = bookId,
        appContainer = appContainer,
        observeBook = observeBook,
        observeReadingState = observeReadingState,
    )
    val observeChapters = rememberObserveChapters(
        bookId = bookId,
        appContainer = appContainer,
        observeBook = observeBook,
        observeBinding = observeBinding,
    )
    val observeLatestChapterRef = rememberObserveLatestChapterRef(
        observeBook = observeBook,
        observeBinding = observeBinding,
    )

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

@Composable
private fun rememberBookDetailViewModel(
    bookId: String,
    appContainer: AppContainer,
): BookDetailViewModel {
    val readingStateDao = appContainer.database.readingStateDao()
    val bookAssetDao = appContainer.database.bookAssetDao()
    val observeBook = rememberObserveBook(bookId = bookId, appContainer = appContainer)
    val observeBinding = rememberObserveBinding(bookId = bookId, appContainer = appContainer)
    val observeReadingState = rememberObserveReadingState(bookId = bookId, appContainer = appContainer)
    val observeChapters = rememberObserveChapters(
        bookId = bookId,
        appContainer = appContainer,
        observeBook = observeBook,
        observeBinding = observeBinding,
    )
    val observeLatestChapterRef = rememberObserveLatestChapterRef(
        observeBook = observeBook,
        observeBinding = observeBinding,
    )
    val observeCoverAsset = remember(bookId, appContainer) {
        bookAssetDao.observeByBookId(bookId).map { assets ->
            assets.firstOrNull { it.assetRole == AssetRole.COVER }
        }
    }
    val observeAutomaticCover = remember(bookId, appContainer) {
        combine(observeBook, observeCoverAsset) { book, coverAsset ->
            book?.cover ?: coverAsset?.takeIf { !isManualCoverAsset(it) }?.storageUri
        }
    }
    val observeManualCover = remember(bookId, appContainer) {
        observeCoverAsset.map { asset ->
            asset?.takeIf(::isManualCoverAsset)?.storageUri
        }
    }

    return remember(bookId, appContainer) {
        BookDetailViewModel(
            bookId = bookId,
            observeBook = observeBook,
            observeReadingState = observeReadingState,
            observeChapters = observeChapters,
            observeLatestChapterRef = observeLatestChapterRef,
            observeAutomaticCover = observeAutomaticCover,
            observeManualCover = observeManualCover,
            refreshCatalogAction = { targetBookId -> appContainer.refreshRemoteBook(targetBookId) },
            refreshCoverAction = { refreshBookCover(bookId = bookId, appContainer = appContainer) },
            importCoverAction = { bytes, mime ->
                importManualCover(
                    bookId = bookId,
                    bytes = bytes,
                    mime = mime,
                    appContainer = appContainer,
                )
            },
            restoreAutomaticCoverAction = {
                restoreAutomaticCover(
                    bookId = bookId,
                    appContainer = appContainer,
                )
            },
            updateReadingState = { state -> readingStateDao.upsert(state.toEntity()) },
        )
    }
}

@Composable
private fun rememberObserveBook(
    bookId: String,
    appContainer: AppContainer,
): Flow<BookRecord?> {
    val bookRecordDao = appContainer.database.bookRecordDao()
    return remember(bookId, appContainer) {
        bookRecordDao.observeById(bookId).map { entity ->
            entity?.toModel()
        }
    }
}

@Composable
private fun rememberObserveBinding(
    bookId: String,
    appContainer: AppContainer,
) = remember(bookId, appContainer) {
    appContainer.database.remoteBindingDao().observeByBookId(bookId).map { entity ->
        entity?.toModel()
    }
}

@Composable
private fun rememberObserveReadingState(
    bookId: String,
    appContainer: AppContainer,
) = remember(bookId, appContainer) {
    appContainer.database.readingStateDao().observeByBookId(bookId).map { entity ->
        entity?.toModel()
    }
}

@Composable
private fun rememberObserveContent(
    bookId: String,
    appContainer: AppContainer,
    observeBook: Flow<BookRecord?>,
    observeReadingState: Flow<com.wenwentome.reader.core.model.ReadingState?>,
) = remember(bookId, appContainer, observeBook, observeReadingState) {
    observeBook.flatMapLatest { book ->
        readerContentFlow(
            book = book,
            bookId = bookId,
            observeReadingState = observeReadingState,
            localBookContentRepository = appContainer.localBookContentRepository,
            remoteBindingDao = appContainer.database.remoteBindingDao(),
            sourceBridgeRepository = appContainer.sourceBridgeRepository,
        )
    }
}

@Composable
private fun rememberObserveChapters(
    bookId: String,
    appContainer: AppContainer,
    observeBook: Flow<BookRecord?>,
    observeBinding: Flow<com.wenwentome.reader.core.model.RemoteBinding?>,
) = remember(bookId, appContainer, observeBook, observeBinding) {
    observeBook.flatMapLatest { book ->
        when (book?.originType) {
            null -> flowOf(emptyList())
            OriginType.LOCAL -> {
                flow {
                    emit(
                        runCatching {
                            withContext(Dispatchers.IO) {
                                appContainer.localBookContentRepository.loadChapters(bookId)
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
                                        appContainer.sourceBridgeRepository.fetchToc(
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

@Composable
private fun rememberObserveLatestChapterRef(
    observeBook: Flow<BookRecord?>,
    observeBinding: Flow<com.wenwentome.reader.core.model.RemoteBinding?>,
) = remember(observeBook, observeBinding) {
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

private suspend fun importManualCover(
    bookId: String,
    bytes: ByteArray,
    mime: String,
    appContainer: AppContainer,
) {
    val bookAssetDao = appContainer.database.bookAssetDao()
    val currentCover = bookAssetDao.findByRole(bookId, AssetRole.COVER)
    if (currentCover != null) {
        appContainer.fileStore.delete(currentCover.storageUri)
    }
    val extension = extensionForMime(mime)
    val storageUri = appContainer.fileStore.persistCover(
        bookId = bookId,
        extension = extension,
        bytes = bytes,
        manualOverride = true,
    )
    bookAssetDao.upsert(
        BookAssetEntity(
            bookId = bookId,
            assetRole = AssetRole.COVER,
            storageUri = storageUri,
            mime = mime,
            size = bytes.size.toLong(),
            hash = sha256Hex(bytes),
            syncPath = "books/$bookId/${LocalBookFileStore.MANUAL_COVER_BASE_NAME}.$extension",
        )
    )
}

private suspend fun refreshBookCover(
    bookId: String,
    appContainer: AppContainer,
) {
    val book = appContainer.database.bookRecordDao().observeById(bookId).first()?.toModel() ?: return
    when (book.originType) {
        OriginType.LOCAL -> refreshLocalAutomaticCover(bookId = bookId, appContainer = appContainer)
        OriginType.WEB, OriginType.MIXED -> refreshRemoteAutomaticCover(book = book, appContainer = appContainer)
    }
}

private suspend fun restoreAutomaticCover(
    bookId: String,
    appContainer: AppContainer,
) {
    val book = appContainer.database.bookRecordDao().observeById(bookId).first()?.toModel() ?: return
    when (book.originType) {
        OriginType.LOCAL -> refreshLocalAutomaticCover(bookId = bookId, appContainer = appContainer)
        OriginType.WEB, OriginType.MIXED -> {
            refreshRemoteAutomaticCover(book = book, appContainer = appContainer)
            val currentCover = appContainer.database.bookAssetDao().findByRole(bookId, AssetRole.COVER)
            if (currentCover != null && isManualCoverAsset(currentCover)) {
                appContainer.fileStore.delete(currentCover.storageUri)
                appContainer.database.bookAssetDao().deleteByRole(bookId, AssetRole.COVER)
            }
        }
    }
}

private suspend fun refreshLocalAutomaticCover(
    bookId: String,
    appContainer: AppContainer,
) {
    val bookAssetDao = appContainer.database.bookAssetDao()
    val currentCover = bookAssetDao.findByRole(bookId, AssetRole.COVER)
    val primaryAsset = bookAssetDao.findPrimaryAsset(bookId)
    if (currentCover != null) {
        appContainer.fileStore.delete(currentCover.storageUri)
    }
    if (primaryAsset == null) {
        bookAssetDao.deleteByRole(bookId, AssetRole.COVER)
        return
    }
    if (!primaryAsset.mime.contains("epub")) {
        bookAssetDao.deleteByRole(bookId, AssetRole.COVER)
        return
    }
    val parsed = withContext(Dispatchers.IO) {
        appContainer.epubBookParser.parse(
            name = "book-$bookId.epub",
            inputStream = appContainer.fileStore.open(primaryAsset.storageUri),
        )
    }
    val coverAsset = parsed.assets.firstOrNull { it.assetRole == AssetRole.COVER }
    if (coverAsset == null) {
        bookAssetDao.deleteByRole(bookId, AssetRole.COVER)
        return
    }
    persistAutomaticLocalCover(
        bookId = bookId,
        asset = coverAsset,
        appContainer = appContainer,
    )
}

private suspend fun persistAutomaticLocalCover(
    bookId: String,
    asset: ParsedAsset,
    appContainer: AppContainer,
) {
    val storageUri = appContainer.fileStore.persistCover(
        bookId = bookId,
        extension = asset.extension,
        bytes = asset.bytes,
        manualOverride = false,
    )
    appContainer.database.bookAssetDao().upsert(
        BookAssetEntity(
            bookId = bookId,
            assetRole = AssetRole.COVER,
            storageUri = storageUri,
            mime = asset.mime,
            size = asset.bytes.size.toLong(),
            hash = sha256Hex(asset.bytes),
            syncPath = "books/$bookId/${LocalBookFileStore.AUTO_COVER_BASE_NAME}.${asset.extension}",
        )
    )
}

private suspend fun refreshRemoteAutomaticCover(
    book: BookRecord,
    appContainer: AppContainer,
) {
    val binding = appContainer.database.remoteBindingDao().observeByBookId(book.id).first()?.toModel() ?: return
    val detail = appContainer.sourceBridgeRepository.fetchBookDetail(binding.sourceId, binding.remoteBookId)
    appContainer.database.bookRecordDao().upsert(
        book.copy(
            cover = detail.coverUrl,
            updatedAt = System.currentTimeMillis(),
        ).toEntity()
    )
}

private fun isManualCoverAsset(asset: BookAssetEntity): Boolean =
    asset.syncPath.substringAfterLast('/').startsWith("${LocalBookFileStore.MANUAL_COVER_BASE_NAME}.")

private fun extensionForMime(mime: String): String =
    when (mime.lowercase()) {
        "image/png" -> "png"
        "image/webp" -> "webp"
        "image/gif" -> "gif"
        else -> "jpg"
    }

private fun sha256Hex(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { b -> "%02x".format(b.toInt() and 0xff) }

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
