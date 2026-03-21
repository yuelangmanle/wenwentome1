package com.wenwentome.reader.feature.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun BookActionsMenu(
    visible: Boolean,
    onDismiss: () -> Unit,
    onOpenDetail: () -> Unit,
    onRefreshCatalog: (() -> Unit)?,
    onRefreshCover: (() -> Unit)?,
    onImportPhoto: (() -> Unit)?,
    onRestoreAutomaticCover: (() -> Unit)?,
) {
    if (!visible) return

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = {
            Text(
                text = "书籍操作",
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                ActionButton("打开详情") {
                    onDismiss()
                    onOpenDetail()
                }
                onRefreshCatalog?.let { refresh ->
                    ActionButton("刷新目录") {
                        onDismiss()
                        refresh()
                    }
                }
                onRefreshCover?.let { refresh ->
                    ActionButton("刷新封面") {
                        onDismiss()
                        refresh()
                    }
                }
                onImportPhoto?.let { import ->
                    ActionButton("导入照片") {
                        onDismiss()
                        import()
                    }
                }
                onRestoreAutomaticCover?.let { restore ->
                    ActionButton("恢复自动封面") {
                        onDismiss()
                        restore()
                    }
                }
            }
        },
    )
}

@Composable
private fun ActionButton(
    label: String,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
    ) {
        Text(text = label)
    }
}
