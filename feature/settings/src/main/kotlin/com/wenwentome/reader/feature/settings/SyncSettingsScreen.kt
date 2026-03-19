package com.wenwentome.reader.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SyncSettingsScreen(
    state: SyncSettingsUiState,
    onStateChange: (SyncSettingsUiState) -> Unit,
    onSaveConfig: (SyncSettingsUiState) -> Unit,
    onPush: (SyncSettingsUiState) -> Unit,
    onPull: (SyncSettingsUiState) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = state.owner,
            onValueChange = { onStateChange(state.updateOwner(it)) },
            label = { Text("Owner") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.repo,
            onValueChange = { onStateChange(state.updateRepo(it)) },
            label = { Text("Repo") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.branch,
            onValueChange = { onStateChange(state.updateBranch(it)) },
            label = { Text("Branch") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.token,
            onValueChange = { onStateChange(state.updateToken(it)) },
            label = { Text("Token") },
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = { onSaveConfig(state) }) {
            Text("保存配置")
        }
        Button(onClick = { onPush(state) }) {
            Text("立即备份")
        }
        OutlinedButton(onClick = { onPull(state) }) {
            Text("恢复到本机")
        }
    }
}
