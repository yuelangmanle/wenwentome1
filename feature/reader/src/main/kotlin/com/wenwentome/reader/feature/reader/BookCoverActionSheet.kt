package com.wenwentome.reader.feature.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun BookCoverActionSheet(
    onRefreshCover: () -> Unit,
    onImportPhoto: () -> Unit,
    onRestoreAutomaticCover: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "封面管理",
                style = MaterialTheme.typography.titleMedium,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onRefreshCover,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("刷新封面")
                }
                OutlinedButton(
                    onClick = onImportPhoto,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("导入照片")
                }
            }
            onRestoreAutomaticCover?.let { restore ->
                OutlinedButton(
                    onClick = restore,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("恢复自动封面")
                }
            }
        }
    }
}
