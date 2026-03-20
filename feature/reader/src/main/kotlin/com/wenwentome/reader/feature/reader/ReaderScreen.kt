package com.wenwentome.reader.feature.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wenwentome.reader.core.model.BookFormat
import com.wenwentome.reader.core.model.ReaderMode
import com.wenwentome.reader.core.model.ReaderTheme

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
    val saveLocator = state.locatorForSave()
    val palette = state.readerPalette()
    var showModePicker by rememberSaveable { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showToc by rememberSaveable { mutableStateOf(false) }

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
                TextButton(onClick = { showModePicker = !showModePicker }) {
                    Text("模式")
                }
                TextButton(onClick = { showToc = !showToc }) {
                    Text("目录")
                }
                TextButton(onClick = { showSettings = !showSettings }) {
                    Text("设置")
                }
            }
            if (showModePicker) {
                ReaderModePicker(
                    selectedMode = state.readerMode,
                    onModeSelected = { mode ->
                        onReaderModeChange(mode)
                        showModePicker = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (showSettings) {
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
            if (showToc) {
                ReaderTocSheet(
                    chapters = state.chapters,
                    currentChapterRef = state.chapterRef,
                    latestChapterRef = state.latestChapterRef,
                    initialScrollChapterRef = state.chapterRef ?: state.latestChapterRef,
                    progressLabel = state.progressLabel,
                    onChapterClick = { chapter ->
                        onChapterSelected(chapter.chapterRef)
                        showToc = false
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
                    ReaderParagraphBody(
                        state = state,
                        palette = palette,
                        modifier = Modifier.background(palette.background),
                    )

                ReaderMode.HORIZONTAL_PAGING ->
                    ReaderParagraphBody(
                        state = state,
                        palette = palette,
                        modifier = Modifier.background(palette.background),
                    )

                ReaderMode.VERTICAL_SCROLL ->
                    ReaderParagraphBody(
                        state = state,
                        palette = palette,
                        modifier = Modifier.background(palette.background),
                    )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            Button(
                onClick = {
                    saveLocator?.let { locator ->
                        onLocatorChanged(locator, state.progressPercent)
                    }
                },
                enabled = saveLocator != null,
            ) {
                Text("保存进度")
            }
        }
    }
}

@Composable
private fun ReaderParagraphBody(
    state: ReaderUiState,
    palette: ReaderPalette,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
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

internal fun ReaderUiState.locatorForSave(): String? =
    locator?.takeIf { it.isNotBlank() }
        ?: when (book?.primaryFormat) {
            BookFormat.TXT -> "0"
            BookFormat.EPUB -> chapterRef?.takeIf { it.isNotBlank() }?.let { "chapter:$it#paragraph:0" }
            BookFormat.WEB -> chapterRef?.takeIf { it.isNotBlank() }
            null -> chapterRef?.takeIf { it.isNotBlank() }
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
