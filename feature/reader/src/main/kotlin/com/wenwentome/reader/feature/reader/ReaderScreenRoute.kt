package com.wenwentome.reader.feature.reader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.wenwentome.reader.core.model.ReaderTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest

@Composable
fun ReaderScreenRoute(
    uiStateFlow: StateFlow<ReaderUiState>,
    onPersistLocator: (locator: String, chapterRef: String?, progressPercent: Float) -> Unit,
    onReaderModeChange: (com.wenwentome.reader.core.model.ReaderMode) -> Unit,
    onThemeChange: (ReaderTheme) -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onAutoFitFontSizeChange: (Boolean) -> Unit = {},
    onLineHeightChange: (Float) -> Unit,
    onLetterSpacingChange: (Float) -> Unit = {},
    onParagraphSpacingChange: (Float) -> Unit = {},
    onSidePaddingChange: (Int) -> Unit = {},
    onBackgroundPaletteChange: (String) -> Unit = {},
    onImportFontClick: () -> Unit = {},
    onBrightnessChange: (Int) -> Unit,
    onChapterSelected: (String) -> Unit,
    onSummarizeChapter: () -> Unit,
    onExplainParagraph: () -> Unit,
    onTranslateParagraph: () -> Unit,
    onSpeakChapter: () -> Unit,
    onOpenBookDetail: (String) -> Unit = {},
    onOpenDownloadSheet: (String) -> Unit = {},
    onOpenSourceSwitcher: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val state by uiStateFlow.collectAsState()

    ReaderScreen(
        state = state,
        onLocatorChanged = { locator, progressPercent ->
            onPersistLocator(locator, state.chapterRef, progressPercent)
        },
        onReaderModeChange = onReaderModeChange,
        onThemeChange = onThemeChange,
        onFontSizeChange = onFontSizeChange,
        onAutoFitFontSizeChange = onAutoFitFontSizeChange,
        onLineHeightChange = onLineHeightChange,
        onLetterSpacingChange = onLetterSpacingChange,
        onParagraphSpacingChange = onParagraphSpacingChange,
        onSidePaddingChange = onSidePaddingChange,
        onBackgroundPaletteChange = onBackgroundPaletteChange,
        onImportFontClick = onImportFontClick,
        onBrightnessChange = onBrightnessChange,
        onChapterSelected = onChapterSelected,
        onSummarizeChapter = onSummarizeChapter,
        onExplainParagraph = onExplainParagraph,
        onTranslateParagraph = onTranslateParagraph,
        onSpeakChapter = onSpeakChapter,
        onBookDetailClick = {
            state.book?.id?.let(onOpenBookDetail)
        },
        onDownloadClick = {
            state.book?.id?.let(onOpenDownloadSheet)
        },
        onSwitchSourceClick = {
            state.book?.id?.let(onOpenSourceSwitcher)
        },
        modifier = modifier,
    )
}

@Composable
fun ReaderScreenRoute(
    viewModel: ReaderViewModel,
    onOpenBookDetail: (String) -> Unit = {},
    onOpenDownloadSheet: (String) -> Unit = {},
    onOpenSourceSwitcher: (String) -> Unit = {},
    onImportFontClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsState()

    ReaderScreen(
        state = state,
        onLocatorChanged = { locator, progressPercent ->
            viewModel.updateLocator(
                locator = locator,
                chapterRef = state.chapterRef,
                progressPercent = progressPercent,
            )
        },
        onReaderModeChange = viewModel::setReaderMode,
        onThemeChange = { theme ->
            viewModel.updatePresentation(state.presentation.copy(theme = theme))
        },
        onFontSizeChange = viewModel::setFontSize,
        onAutoFitFontSizeChange = viewModel::setAutoFitFontSize,
        onLineHeightChange = viewModel::setLineHeightMultiplier,
        onLetterSpacingChange = viewModel::setLetterSpacing,
        onParagraphSpacingChange = viewModel::setParagraphSpacing,
        onSidePaddingChange = viewModel::setSidePaddingDp,
        onBackgroundPaletteChange = viewModel::setBackgroundPalette,
        onImportFontClick = onImportFontClick,
        onBrightnessChange = { brightness ->
            viewModel.updatePresentation(
                state.presentation.copy(brightnessPercent = brightness.coerceIn(0, 100))
            )
        },
        onChapterSelected = viewModel::jumpToChapter,
        onSummarizeChapter = viewModel::generateChapterSummary,
        onExplainParagraph = viewModel::explainCurrentParagraph,
        onTranslateParagraph = viewModel::translateCurrentParagraph,
        onSpeakChapter = viewModel::speakCurrentChapter,
        onBookDetailClick = {
            state.book?.id?.let(onOpenBookDetail)
        },
        onDownloadClick = {
            state.book?.id?.let(onOpenDownloadSheet)
        },
        onSwitchSourceClick = {
            state.book?.id?.let(onOpenSourceSwitcher)
        },
        modifier = modifier,
    )
}

@Composable
fun BookDetailRoute(
    uiStateFlow: StateFlow<BookDetailUiState>,
    events: Flow<BookDetailEvent>,
    onReadClick: () -> Unit,
    onChapterClick: (String) -> Unit,
    onRefreshCatalogClick: () -> Unit,
    onJumpToLatestClick: () -> Unit,
    onRefreshCoverClick: () -> Unit,
    onImportPhotoClick: () -> Unit,
    onRestoreAutomaticCoverClick: () -> Unit,
    onEnhanceMetadataClick: () -> Unit,
    onApplyMetadataClick: () -> Unit,
    onOpenReader: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by uiStateFlow.collectAsState()

    LaunchedEffect(events) {
        events.collectLatest { event ->
            when (event) {
                is BookDetailEvent.OpenReader -> onOpenReader(event.bookId)
            }
        }
    }

    BookDetailScreen(
        state = state,
        onReadClick = onReadClick,
        onToggleCatalog = {},
        onChapterClick = onChapterClick,
        onRefreshCatalogClick = onRefreshCatalogClick,
        onJumpToLatestClick = onJumpToLatestClick,
        onRefreshCoverClick = onRefreshCoverClick,
        onImportPhotoClick = onImportPhotoClick,
        onRestoreAutomaticCoverClick = onRestoreAutomaticCoverClick,
        onEnhanceMetadataClick = onEnhanceMetadataClick,
        onApplyMetadataClick = onApplyMetadataClick,
        modifier = modifier,
    )
}

@Composable
fun BookDetailRoute(
    viewModel: BookDetailViewModel,
    onImportPhotoClick: () -> Unit,
    onOpenReader: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BookDetailRoute(
        uiStateFlow = viewModel.uiState,
        events = viewModel.events,
        onReadClick = viewModel::openReader,
        onChapterClick = viewModel::openChapter,
        onRefreshCatalogClick = viewModel::refreshCatalog,
        onJumpToLatestClick = viewModel::jumpToLatest,
        onRefreshCoverClick = viewModel::refreshCover,
        onImportPhotoClick = onImportPhotoClick,
        onRestoreAutomaticCoverClick = viewModel::restoreAutomaticCover,
        onEnhanceMetadataClick = viewModel::enhanceMetadata,
        onApplyMetadataClick = viewModel::applyMetadataSuggestion,
        onOpenReader = onOpenReader,
        modifier = modifier,
    )
}
