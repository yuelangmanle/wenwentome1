package com.wenwentome.reader.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
        SyncSettingsScreen(
            state = state,
            onStateChange = onStateChange,
            onSaveConfig = onSaveConfig,
            onPush = onPush,
            onPull = onPull,
        )
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
