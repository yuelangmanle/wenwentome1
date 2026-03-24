package com.wenwentome.reader.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    state: SyncSettingsUiState,
    projectInfo: AboutProjectInfo,
    onStateChange: (SyncSettingsUiState) -> Unit,
    onSaveConfig: (SyncSettingsUiState) -> Unit,
    onPush: (SyncSettingsUiState) -> Unit,
    onPull: (SyncSettingsUiState) -> Unit,
    onOpenApiHub: () -> Unit,
    onOpenProject: () -> Unit,
    onOpenChangelog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var syncExpanded by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "我的",
            modifier = Modifier.testTag("screen"),
            style = MaterialTheme.typography.headlineSmall,
        )
        ElevatedCard(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .testTag("settings-cloud-sync-entry"),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "云同步",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = buildCloudSyncSummary(state),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = { syncExpanded = !syncExpanded }) {
                    Text(if (syncExpanded) "收起配置" else "打开配置")
                }
            }
        }
        if (syncExpanded) {
            SyncSettingsScreen(
                state = state,
                onStateChange = onStateChange,
                onSaveConfig = onSaveConfig,
                onPush = onPush,
                onPull = onPull,
            )
        }
        Button(
            onClick = onOpenApiHub,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .testTag("open-api-hub-button"),
        ) {
            Text("API 中心")
        }
        AboutProjectCard(
            info = projectInfo,
            onOpenProject = onOpenProject,
            onOpenChangelog = onOpenChangelog,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}

private fun buildCloudSyncSummary(state: SyncSettingsUiState): String {
    val owner = state.owner.trim()
    val repo = state.repo.trim()
    return if (owner.isNotBlank() && repo.isNotBlank()) {
        "已连接到 $owner/$repo，点击可查看或修改同步配置。"
    } else {
        "把书库和配置备份到 GitHub。点击后再填写用户名、仓库、Token 和同步密码。"
    }
}
