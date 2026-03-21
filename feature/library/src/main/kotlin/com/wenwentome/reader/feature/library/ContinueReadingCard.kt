package com.wenwentome.reader.feature.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun ContinueReadingCard(
    item: LibraryBookItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBF6)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(width = 118.dp, height = 174.dp)) {
                LibraryBookCover(
                    title = item.book.title,
                    coverUri = item.effectiveCover,
                    modifier = Modifier.matchParentSize(),
                    shape = RoundedCornerShape(24.dp),
                    realCoverTag = "continue-reading-real-cover",
                    placeholderTag = "continue-reading-placeholder-cover",
                ) {
                    Column(
                        modifier = Modifier.matchParentSize(),
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        CoverPill(
                            text = "继续阅读",
                            containerColor = Color.White.copy(alpha = 0.18f),
                            contentColor = Color.White,
                        )
                        Text(
                            text = item.book.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.SemiBold,
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

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "继续阅读",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF9A6336),
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = item.book.title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.book.author ?: "未知作者",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LinearProgressIndicator(
                    progress = { item.progressPercent.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "阅读进度 ${item.progressLabel}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(2.dp))
                FilledTonalButton(onClick = onClick) {
                    Text(text = "继续阅读")
                }
            }
        }
    }
}
