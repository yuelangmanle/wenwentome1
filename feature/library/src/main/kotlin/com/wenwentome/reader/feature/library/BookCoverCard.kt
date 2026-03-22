package com.wenwentome.reader.feature.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wenwentome.reader.core.model.OriginType

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookCoverCard(
    item: LibraryBookItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBF7)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
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
                    .aspectRatio(0.66f),
            ) {
                LibraryBookCover(
                    title = item.book.title,
                    coverUri = item.effectiveCover,
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(22.dp),
                    realCoverTag = "book-cover-real-cover-${item.book.id}",
                    placeholderTag = "book-cover-placeholder-${item.book.id}",
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = placeholderLabel(item),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.72f),
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = item.book.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = item.book.author ?: "未知作者",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.86f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    CoverPill(
                        text = sourceLabel(item.book.originType),
                        containerColor = Color(0xFFFBF4EC).copy(alpha = 0.92f),
                        contentColor = Color(0xFF6D4125),
                    )
                    if (item.hasUpdates) {
                        CoverPill(
                            text = "更新",
                            containerColor = Color(0xFFB74232),
                            contentColor = Color.White,
                        )
                    }
                }

                CoverPill(
                    text = progressLabel(item),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp),
                    containerColor = Color(0xFF24150D).copy(alpha = 0.78f),
                    contentColor = Color.White,
                )
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
                text = "${sourceLabel(item.book.originType)} · ${progressLabel(item)}",
                style = MaterialTheme.typography.labelMedium,
                color = if (item.hasUpdates) Color(0xFFB74232) else Color(0xFF8D6848),
            )
        }
    }
}

@Composable
internal fun CoverPill(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color,
    contentColor: Color,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = containerColor,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

internal fun sourceLabel(originType: OriginType): String =
    when (originType) {
        OriginType.LOCAL -> "本地藏书"
        OriginType.WEB -> "连载书籍"
        OriginType.MIXED -> "混合来源"
    }

internal fun placeholderLabel(item: LibraryBookItem): String =
    if (item.effectiveCover.isNullOrBlank()) {
        "书架藏书"
    } else {
        "同步封面"
    }

internal fun progressLabel(item: LibraryBookItem): String =
    if (item.progressPercent > 0f) {
        "已读 ${item.progressLabel}"
    } else {
        "未开始"
    }
