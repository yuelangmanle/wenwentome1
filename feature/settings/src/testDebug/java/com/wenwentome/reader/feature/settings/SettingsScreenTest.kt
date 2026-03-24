package com.wenwentome.reader.feature.settings

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SettingsScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun cloudSyncForm_isCollapsedByDefault() {
        composeTestRule.setContent {
            SettingsScreen(
                state = SyncSettingsUiState(),
                projectInfo = sampleProjectInfo(),
                onStateChange = {},
                onSaveConfig = {},
                onPush = {},
                onPull = {},
                onOpenApiHub = {},
                onOpenProject = {},
                onOpenChangelog = {},
            )
        }

        composeTestRule.onNodeWithTag("settings-cloud-sync-entry").assertExists()
        composeTestRule.onNodeWithText("立即备份").assertDoesNotExist()
    }

    @Test
    fun cloudSyncForm_expandsAfterClick() {
        composeTestRule.setContent {
            SettingsScreen(
                state = SyncSettingsUiState(),
                projectInfo = sampleProjectInfo(),
                onStateChange = {},
                onSaveConfig = {},
                onPush = {},
                onPull = {},
                onOpenApiHub = {},
                onOpenProject = {},
                onOpenChangelog = {},
            )
        }

        composeTestRule.onNodeWithText("打开配置").performClick()

        composeTestRule.onNodeWithText("立即备份").assertExists()
        composeTestRule.onNodeWithText("GitHub 用户名").assertExists()
    }
}

private fun sampleProjectInfo() =
    AboutProjectInfo(
        appName = "WenwenToMe",
        authorName = "月亮慢了",
        versionName = "1.5.0",
        projectUrl = "https://github.com/example/wenwentome",
    )
