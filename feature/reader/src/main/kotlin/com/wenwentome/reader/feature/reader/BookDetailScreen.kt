package com.wenwentome.reader.feature.reader

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wenwentome.reader.core.model.ReaderChapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URI

@Composable
fun BookDetailScreen(
    state: BookDetailUiState,
    onReadClick: () -> Unit,
    onToggleCatalog: () -> Unit,
    onChapterClick: (String) -> Unit,
    onRefreshCatalogClick: () -> Unit,
    onJumpToLatestClick: () -> Unit,
    onRefreshCoverClick: () -> Unit,
    onImportPhotoClick: () -> Unit,
    onRestoreAutomaticCoverClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showCatalog by rememberSaveable { mutableStateOf(false) }
    val book = state.book

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF8F1E8),
                        Color(0xFFFFFCF8),
                    )
                )
            )
            .testTag("book-detail"),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFFBF6),
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    BookCoverArt(
                        title = book?.title.orEmpty(),
                        coverUri = state.effectiveCover,
                        modifier = Modifier.width(124.dp),
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = book?.title ?: "书籍加载中",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = book?.author ?: "未知作者",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        state.lastReadLabel?.let { label ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFF1E3D1),
                            ) {
                                Text(
                                    text = label,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color(0xFF6A4B2F),
                                )
                            }
                        }
                        book?.summary?.takeIf { it.isNotBlank() }?.let { summary ->
                            Text(
                                text = summary,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "阅读进度",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    LinearProgressIndicator(
                        progress = { state.progressPercent.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = state.progressLabel,
                        modifier = Modifier.testTag("detail-progress-label"),
                        style = MaterialTheme.typography.titleLarge,
                        color = Color(0xFF8B5E34),
                    )
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        onClick = onReadClick,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("detail-read-button"),
                    ) {
                        Text(state.readActionLabel)
                    }
                    OutlinedButton(
                        onClick = {
                            showCatalog = !showCatalog
                            onToggleCatalog()
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("查看目录")
                    }
                }
                if (state.showRefreshCatalogAction || state.showJumpToLatestAction) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (state.showRefreshCatalogAction) {
                            OutlinedButton(
                                onClick = onRefreshCatalogClick,
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("刷新目录")
                            }
                        }
                        if (state.showJumpToLatestAction) {
                            OutlinedButton(
                                onClick = onJumpToLatestClick,
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("跳转最新章")
                            }
                        }
                    }
                }
            }
        }

        item {
            BookCoverActionSheet(
                onRefreshCover = onRefreshCoverClick,
                onImportPhoto = onImportPhotoClick,
                onRestoreAutomaticCover = if (state.canRestoreAutomaticCover) {
                    onRestoreAutomaticCoverClick
                } else {
                    null
                },
            )
        }

        if (showCatalog && state.showTocAction) {
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = "目录",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        state.latestChapterTitle?.let { latest ->
                            Text(
                                text = "最新更新 $latest",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF8B5E34),
                            )
                        }
                        CatalogList(
                            chapters = state.chapters,
                            latestChapterRef = state.latestChapterRef,
                            onChapterClick = onChapterClick,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CatalogList(
    chapters: List<ReaderChapter>,
    latestChapterRef: String?,
    onChapterClick: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 280.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        userScrollEnabled = true,
    ) {
        items(chapters, key = { it.chapterRef }) { chapter ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onChapterClick(chapter.chapterRef) }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = chapter.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (chapter.chapterRef == latestChapterRef || chapter.isLatest) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Color(0xFFE9D2B7),
                    ) {
                        Text(
                            text = "最新",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF80542D),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BookCoverArt(
    title: String,
    coverUri: String?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val coverBitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(initialValue = null, coverUri) {
        value = loadLocalCoverBitmap(context, coverUri)
    }

    Card(
        modifier = modifier.aspectRatio(0.68f),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    ) {
        if (coverBitmap != null) {
            Image(
                bitmap = requireNotNull(coverBitmap),
                contentDescription = title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF9E6A43),
                                Color(0xFF54331F),
                            )
                        )
                    )
                    .padding(14.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Color.White.copy(alpha = 0.16f),
                    ) {
                        Text(
                            text = if (coverUri.isNullOrBlank()) "本地书籍" else "封面待同步",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = title.ifBlank { "未命名书籍" },
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

private suspend fun loadLocalCoverBitmap(
    context: Context,
    coverUri: String?,
): androidx.compose.ui.graphics.ImageBitmap? =
    withContext(Dispatchers.IO) {
        if (coverUri.isNullOrBlank()) return@withContext null
        val stream = when {
            coverUri.startsWith("content://") ->
                context.contentResolver.openInputStream(android.net.Uri.parse(coverUri))

            coverUri.startsWith("file:") ->
                runCatching { File(URI(coverUri)).inputStream() }.getOrNull()

            else -> null
        } ?: return@withContext null

        stream.use { input ->
            BitmapFactory.decodeStream(input)?.asImageBitmap()
        }
    }
