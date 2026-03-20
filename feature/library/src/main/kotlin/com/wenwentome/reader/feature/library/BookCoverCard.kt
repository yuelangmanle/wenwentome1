package com.wenwentome.reader.feature.library

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URI

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookCoverCard(
    item: LibraryBookItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val coverBitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(initialValue = null, item.effectiveCover) {
        value = loadLocalCoverBitmap(context, item.effectiveCover)
    }

    Card(
        modifier = modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(26.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.68f),
            ) {
                if (coverBitmap != null) {
                    Image(
                        bitmap = requireNotNull(coverBitmap),
                        contentDescription = item.book.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color(0xFFD39B68), Color(0xFF724425)),
                                ),
                                shape = RoundedCornerShape(22.dp),
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
                                    text = if (item.effectiveCover.isNullOrBlank()) "书架藏书" else "已同步封面",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = item.book.title,
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                if (item.hasUpdates) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp),
                        shape = RoundedCornerShape(999.dp),
                        color = Color(0xFFB74232),
                    ) {
                        Text(
                            text = "更新",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            Text(
                text = item.book.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.book.author ?: "未知作者",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (item.progressPercent > 0f) "已读 ${item.progressLabel}" else "未开始",
                style = MaterialTheme.typography.labelMedium,
                color = if (item.hasUpdates) Color(0xFFB74232) else Color(0xFF8D6848),
            )
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
