package com.wenwentome.reader.feature.apihub

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.wenwentome.reader.data.apihub.sync.PendingSyncConflict

@Composable
fun BindingConflictDialog(
    conflict: PendingSyncConflict,
    onDismissRequest: () -> Unit,
    onKeepLocal: () -> Unit,
    onUseRemote: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("绑定冲突") },
        text = {
            Text(
                "本地：${conflict.local.primaryProviderId}/${conflict.local.primaryModelId}\n" +
                    "新方案：${conflict.remote.primaryProviderId}/${conflict.remote.primaryModelId}",
            )
        },
        dismissButton = {
            TextButton(onClick = onKeepLocal) {
                Text("保留本地")
            }
        },
        confirmButton = {
            TextButton(onClick = onUseRemote) {
                Text("采用新方案")
            }
        },
    )
}
