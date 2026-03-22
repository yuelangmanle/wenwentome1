package com.wenwentome.reader.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun AboutProjectCard(
    info: AboutProjectInfo,
    onOpenProject: () -> Unit,
    onOpenChangelog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "关于与项目",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(text = info.appName)
            Text(
                text = "作者：${info.authorName}",
                modifier = Modifier.testTag("project-author-text"),
            )
            Text(
                text = "版本 ${info.versionName}",
                modifier = Modifier.testTag("project-version-text"),
            )
            OutlinedButton(onClick = onOpenProject) {
                Text("打开 GitHub 项目")
            }
            Button(
                onClick = onOpenChangelog,
                modifier = Modifier.testTag("open-changelog-button"),
            ) {
                Text("查看完整更新日志")
            }
        }
    }
}
