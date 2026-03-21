package com.wenwentome.reader.feature.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wenwentome.reader.core.model.BookFormat
import com.wenwentome.reader.core.model.ReaderMode
import com.wenwentome.reader.core.model.ReaderTheme
import kotlinx.coroutines.launch

@Composable
fun ReaderScreen(
    state: ReaderUiState,
    onLocatorChanged: (String, Float) -> Unit,
    onReaderModeChange: (ReaderMode) -> Unit,
    onThemeChange: (ReaderTheme) -> Unit,
    onFontSizeChange: (Int) -> Unit,
    onLineHeightChange: (Float) -> Unit,
    onBrightnessChange: (Int) -> Unit,
    onChapterSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = state.readerPalette()
    val baseParagraphIndex = remember(state.book?.primaryFormat, state.locator) {
        windowBaseParagraphIndex(
            format = state.book?.primaryFormat,
            locator = state.locator,
        )
    }
    val readerPages = remember(
        state.book?.primaryFormat,
        state.chapterRef,
        state.paragraphs,
        state.presentation.fontSizeSp,
        state.readerMode,
        baseParagraphIndex,
    ) {
        state.toReaderPages(baseParagraphIndex = baseParagraphIndex)
    }
    val initialParagraphIndex = remember(state.book?.primaryFormat) {
        when (state.book?.primaryFormat) {
            BookFormat.TXT,
            BookFormat.EPUB -> 0
            BookFormat.WEB,
            null -> paragraphIndexFromLocator(
                format = state.book?.primaryFormat,
                locator = state.locator,
            )
        }
    }
    val initialPageIndex = remember(readerPages, state.book?.primaryFormat, state.locator) {
        pageIndexFromLocator(
            pages = readerPages,
            format = state.book?.primaryFormat,
            locator = state.locator,
        )
    }
    val pagerState = rememberPagerState(
        initialPage = initialPageIndex.coerceIn(0, readerPages.lastIndex),
        pageCount = { readerPages.size },
    )
    val verticalListState = rememberLazyListState(
        initialFirstVisibleItemIndex = initialParagraphIndex.coerceIn(0, state.paragraphs.lastIndex.coerceAtLeast(0)),
    )
    val pagerScope = rememberCoroutineScope()
    var showModePicker by remember { mutableIntStateOf(0) }
    var showSettings by remember { mutableIntStateOf(0) }
    var showToc by remember { mutableIntStateOf(0) }

    LaunchedEffect(state.locator, state.readerMode, readerPages.size) {
        val targetPage = pageIndexFromLocator(
            pages = readerPages,
            format = state.book?.primaryFormat,
            locator = state.locator,
        ).coerceIn(0, readerPages.lastIndex)
        if (state.readerMode != ReaderMode.VERTICAL_SCROLL && pagerState.currentPage != targetPage) {
            pagerState.scrollToPage(targetPage)
        }
    }

    LaunchedEffect(state.locator, state.readerMode, state.paragraphs.size) {
        val targetIndex = initialParagraphIndex.coerceIn(0, state.paragraphs.lastIndex.coerceAtLeast(0))
        if (state.readerMode == ReaderMode.VERTICAL_SCROLL && verticalListState.firstVisibleItemIndex != targetIndex) {
            verticalListState.scrollToItem(targetIndex)
        }
    }

    val currentPosition = when (state.readerMode) {
        ReaderMode.SIMULATED_PAGE_TURN,
        ReaderMode.HORIZONTAL_PAGING ->
            pagerViewportPosition(
                pages = readerPages,
                currentPage = pagerState.currentPage,
            )

        ReaderMode.VERTICAL_SCROLL ->
            verticalViewportPosition(
                state = state,
                paragraphIndex = verticalListState.firstVisibleItemIndex,
                baseParagraphIndex = baseParagraphIndex,
            )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
            .testTag("reader-screen"),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = state.chapterTitle ?: state.book?.title.orEmpty(),
                style = MaterialTheme.typography.titleLarge,
                color = palette.text,
                modifier = Modifier.testTag("reader-chapter-title"),
            )
            LinearProgressIndicator(
                progress = { state.progressPercent.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = state.progressLabel,
                style = MaterialTheme.typography.labelLarge,
                color = palette.text,
                modifier = Modifier.testTag("reader-progress-summary"),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TextButton(onClick = { showModePicker = 1 - showModePicker }) {
                    Text("模式")
                }
                TextButton(onClick = { showToc = 1 - showToc }) {
                    Text("目录")
                }
                TextButton(onClick = { showSettings = 1 - showSettings }) {
                    Text("设置")
                }
            }
            if (showModePicker == 1) {
                ReaderModePicker(
                    selectedMode = state.readerMode,
                    onModeSelected = { mode ->
                        onReaderModeChange(mode)
                        showModePicker = 0
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (showSettings == 1) {
                ReaderSettingsSheet(
                    presentation = state.presentation,
                    progressLabel = state.progressLabel,
                    onThemeChange = onThemeChange,
                    onFontSizeChange = onFontSizeChange,
                    onLineHeightChange = onLineHeightChange,
                    onBrightnessChange = onBrightnessChange,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (showToc == 1) {
                ReaderTocSheet(
                    chapters = state.chapters,
                    currentChapterRef = state.tocHighlightedChapterRef ?: state.chapterRef,
                    latestChapterRef = state.latestChapterRef,
                    initialScrollChapterRef = state.chapterRef ?: state.latestChapterRef,
                    progressLabel = state.progressLabel,
                    onChapterClick = { chapter ->
                        onChapterSelected(chapter.chapterRef)
                        showToc = 0
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp),
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            when (state.readerMode) {
                ReaderMode.SIMULATED_PAGE_TURN ->
                    ReaderSimulatedPager(
                        pages = readerPages,
                        pagerState = pagerState,
                        palette = palette,
                        presentation = state.presentation,
                        modifier = Modifier.fillMaxSize(),
                        onPrev = {
                            if (pagerState.currentPage > 0) {
                                pagerScope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                            }
                        },
                        onNext = {
                            if (pagerState.currentPage < readerPages.lastIndex) {
                                pagerScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                            }
                        },
                    )

                ReaderMode.HORIZONTAL_PAGING ->
                    ReaderHorizontalPager(
                        pages = readerPages,
                        pagerState = pagerState,
                        palette = palette,
                        presentation = state.presentation,
                        modifier = Modifier.fillMaxSize(),
                    )

                ReaderMode.VERTICAL_SCROLL ->
                    ReaderVerticalScrollBody(
                        state = state,
                        palette = palette,
                        listState = verticalListState,
                        modifier = Modifier.fillMaxSize(),
                    )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (state.readerMode != ReaderMode.VERTICAL_SCROLL) {
                Text(
                    text = "第 ${pagerState.currentPage + 1} / ${readerPages.size} 页",
                    color = palette.text,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.testTag("reader-page-indicator"),
                )
            } else {
                Text(
                    text = "段落 ${verticalListState.firstVisibleItemIndex + 1}",
                    color = palette.text,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Button(
                onClick = {
                    val locator = currentPosition.locator ?: state.locatorForSave() ?: return@Button
                    onLocatorChanged(locator, currentPosition.progressPercent)
                },
                enabled = currentPosition.locator != null || state.locatorForSave() != null,
            ) {
                Text("保存进度")
            }
        }
    }
}

@Composable
private fun ReaderSimulatedPager(
    pages: List<ReaderPageSlice>,
    pagerState: androidx.compose.foundation.pager.PagerState,
    palette: ReaderPalette,
    presentation: com.wenwentome.reader.core.model.ReaderPresentationPrefs,
    modifier: Modifier = Modifier,
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
            .testTag("reader-simulated-pager"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 18.dp),
            pageSpacing = 14.dp,
        ) { pageIndex ->
            ReaderPageSurface(
                paragraphs = pages[pageIndex].paragraphs,
                palette = palette,
                presentation = presentation,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(onClick = onPrev, enabled = pagerState.currentPage > 0) {
                Text("上一页")
            }
            Text(
                text = "仿真翻页",
                color = palette.text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
            )
            OutlinedButton(onClick = onNext, enabled = pagerState.currentPage < pages.lastIndex) {
                Text("下一页")
            }
        }
    }
}

@Composable
private fun ReaderHorizontalPager(
    pages: List<ReaderPageSlice>,
    pagerState: androidx.compose.foundation.pager.PagerState,
    palette: ReaderPalette,
    presentation: com.wenwentome.reader.core.model.ReaderPresentationPrefs,
    modifier: Modifier = Modifier,
) {
    HorizontalPager(
        state = pagerState,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp)
            .testTag("reader-horizontal-pager"),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        pageSpacing = 12.dp,
    ) { pageIndex ->
        ReaderPageSurface(
            paragraphs = pages[pageIndex].paragraphs,
            palette = palette,
            presentation = presentation,
            modifier = Modifier.fillMaxSize(),
            containerColor = palette.background.copy(alpha = 0.92f),
        )
    }
}

@Composable
private fun ReaderVerticalScrollBody(
    state: ReaderUiState,
    palette: ReaderPalette,
    listState: androidx.compose.foundation.lazy.LazyListState,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("reader-vertical-scroll"),
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .testTag("reader-body"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (state.paragraphs.isEmpty()) {
                item {
                    Text(
                        text = "暂无正文内容",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.text,
                    )
                }
            } else {
                items(state.paragraphs) { paragraph ->
                    SelectionContainer {
                        Text(
                            text = paragraph,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = state.presentation.fontSizeSp.sp,
                                lineHeight = (state.presentation.fontSizeSp * state.presentation.lineHeightMultiplier).sp,
                                color = palette.text,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReaderPageSurface(
    paragraphs: List<String>,
    palette: ReaderPalette,
    presentation: com.wenwentome.reader.core.model.ReaderPresentationPrefs,
    modifier: Modifier = Modifier,
    containerColor: Color = Color(0xFFFFFCF8),
) {
    Card(
        modifier = modifier
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(28.dp)),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(containerColor)
                .padding(horizontal = 22.dp, vertical = 26.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (paragraphs.isEmpty()) {
                Text(
                    text = "暂无正文内容",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.text,
                )
            } else {
                paragraphs.forEach { paragraph ->
                    SelectionContainer {
                        Text(
                            text = paragraph,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = presentation.fontSizeSp.sp,
                                lineHeight = (presentation.fontSizeSp * presentation.lineHeightMultiplier).sp,
                                color = palette.text,
                            ),
                        )
                    }
                }
            }
        }
    }
}

internal fun ReaderUiState.locatorForSave(): String? =
    locator?.takeIf { it.isNotBlank() }
        ?: when (book?.primaryFormat) {
            BookFormat.TXT -> "0"
            BookFormat.EPUB -> chapterRef?.takeIf { it.isNotBlank() }?.let { "chapter:$it#paragraph:0" }
            BookFormat.WEB -> chapterRef?.takeIf { it.isNotBlank() }
            null -> chapterRef?.takeIf { it.isNotBlank() }
        }

private data class ReaderPageSlice(
    val locator: String?,
    val progressPercent: Float,
    val paragraphs: List<String>,
)

private data class ReaderViewportPosition(
    val locator: String?,
    val progressPercent: Float,
    val progressLabel: String,
)

private fun pagerViewportPosition(
    pages: List<ReaderPageSlice>,
    currentPage: Int,
): ReaderViewportPosition {
    val page = pages.getOrElse(currentPage) { pages.first() }
    return ReaderViewportPosition(
        locator = page.locator,
        progressPercent = page.progressPercent,
        progressLabel = formatProgressLabel(page.progressPercent),
    )
}

private fun verticalViewportPosition(
    state: ReaderUiState,
    paragraphIndex: Int,
    baseParagraphIndex: Int,
): ReaderViewportPosition {
    val resolvedParagraphIndex = paragraphIndex.coerceAtLeast(0)
    val progressPercent = progressPercentForParagraphIndex(
        paragraphIndex = baseParagraphIndex + resolvedParagraphIndex,
        paragraphCount = state.paragraphs.size,
    )
    return ReaderViewportPosition(
        locator = buildLocatorForParagraph(
            format = state.book?.primaryFormat,
            chapterRef = state.chapterRef,
            paragraphIndex = baseParagraphIndex + resolvedParagraphIndex,
            fallbackLocator = state.locatorForSave(),
        ),
        progressPercent = progressPercent,
        progressLabel = formatProgressLabel(progressPercent),
    )
}

private fun ReaderUiState.toReaderPages(baseParagraphIndex: Int): List<ReaderPageSlice> {
    if (paragraphs.isEmpty()) {
        return listOf(
            ReaderPageSlice(
                locator = locatorForSave(),
                progressPercent = progressPercent,
                paragraphs = emptyList(),
            )
        )
    }

    val paragraphsPerPage = when {
        presentation.fontSizeSp >= 24 -> 2
        presentation.fontSizeSp >= 20 -> 3
        readerMode == ReaderMode.SIMULATED_PAGE_TURN -> 3
        else -> 4
    }

    val chunks = paragraphs.chunked(paragraphsPerPage)
    return chunks.mapIndexed { pageIndex, chunk ->
        val startParagraphIndex = baseParagraphIndex + (pageIndex * paragraphsPerPage)
        ReaderPageSlice(
            locator = buildLocatorForParagraph(
                format = book?.primaryFormat,
                chapterRef = chapterRef,
                paragraphIndex = startParagraphIndex,
                fallbackLocator = locatorForSave(),
            ),
            progressPercent = progressPercentForParagraphIndex(startParagraphIndex, paragraphs.size),
            paragraphs = chunk,
        )
    }
}

private fun windowBaseParagraphIndex(
    format: BookFormat?,
    locator: String?,
): Int =
    when (format) {
        BookFormat.TXT,
        BookFormat.EPUB -> paragraphIndexFromLocator(format, locator)
        BookFormat.WEB,
        null -> 0
    }

private fun buildLocatorForParagraph(
    format: BookFormat?,
    chapterRef: String?,
    paragraphIndex: Int,
    fallbackLocator: String?,
): String? =
    when (format) {
        BookFormat.TXT -> paragraphIndex.coerceAtLeast(0).toString()
        BookFormat.EPUB -> chapterRef?.takeIf { it.isNotBlank() }?.let { "chapter:$it#paragraph:${paragraphIndex.coerceAtLeast(0)}" }
        BookFormat.WEB -> chapterRef?.takeIf { it.isNotBlank() } ?: fallbackLocator
        null -> chapterRef?.takeIf { it.isNotBlank() } ?: fallbackLocator
    }

private fun paragraphIndexFromLocator(
    format: BookFormat?,
    locator: String?,
): Int {
    val value = locator?.trim().orEmpty()
    if (value.isBlank()) return 0
    return when (format) {
        BookFormat.TXT -> value.toIntOrNull()?.coerceAtLeast(0) ?: 0
        BookFormat.EPUB -> STRUCTURED_EPUB_LOCATOR.matchEntire(value)?.groupValues?.getOrNull(2)?.toIntOrNull()?.coerceAtLeast(0) ?: 0
        BookFormat.WEB, null -> 0
    }
}

private fun pageIndexFromLocator(
    pages: List<ReaderPageSlice>,
    format: BookFormat?,
    locator: String?,
): Int {
    val paragraphIndex = paragraphIndexFromLocator(format, locator)
    return pages.indexOfLast { page ->
        val pageParagraphIndex = paragraphIndexFromLocator(format, page.locator)
        pageParagraphIndex <= paragraphIndex
    }.takeIf { it >= 0 } ?: 0
}

private fun progressPercentForParagraphIndex(
    paragraphIndex: Int,
    paragraphCount: Int,
): Float {
    if (paragraphCount <= 1) return 0f
    return paragraphIndex.coerceIn(0, paragraphCount - 1) / (paragraphCount - 1).toFloat()
}

private data class ReaderPalette(
    val background: Color,
    val text: Color,
)

private fun ReaderUiState.readerPalette(): ReaderPalette =
    when (presentation.theme) {
        ReaderTheme.PAPER ->
            ReaderPalette(
                background = Color(0xFFF6F0E5),
                text = Color(0xFF2D241C),
            )

        ReaderTheme.SEPIA ->
            ReaderPalette(
                background = Color(0xFFE7D2B6),
                text = Color(0xFF3D2C1C),
            )

        ReaderTheme.NIGHT ->
            ReaderPalette(
                background = Color(0xFF121212),
                text = Color(0xFFE8E4DA),
            )
    }

private val STRUCTURED_EPUB_LOCATOR = Regex("^chapter:(.+)#paragraph:(\\d+)$")
