package com.wenwentome.reader.feature.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wenwentome.reader.core.model.ReadingBookmark

@Composable
fun ReaderBookmarkDrawer(
    bookmarks: List<ReadingBookmark>,
    currentLocator: String?,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.testTag("reader-bookmark-drawer"),
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("书签", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "长按正文可补充摘录与批注",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (bookmarks.isEmpty()) {
                Text("暂无书签", style = MaterialTheme.typography.bodyMedium)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(bookmarks, key = { it.locator }) { bookmark ->
                        TextButton(
                            onClick = {},
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (bookmark.locator == currentLocator) {
                                        Modifier.testTag("bookmark-current-item")
                                    } else {
                                        Modifier
                                    }
                                ),
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = bookmark.label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (bookmark.locator == currentLocator) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = bookmark.chapterRef ?: "当前章节",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "摘录预览：${bookmark.label}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
