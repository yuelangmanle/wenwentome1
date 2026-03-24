package com.wenwentome.reader.feature.settings

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
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

        composeTestRule.assertTagExists("settings-cloud-sync-entry")
        composeTestRule.assertTextDoesNotExist("立即备份")
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

        composeTestRule.assertTextExists("立即备份")
        composeTestRule.assertTextExists("GitHub 用户名")
    }
}

private fun sampleProjectInfo() =
    AboutProjectInfo(
        appName = "WenwenToMe",
        authorName = "月亮慢了",
        versionName = "1.5.0",
        projectUrl = "https://github.com/example/wenwentome",
    )

private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.assertTagExists(tag: String) {
    assertTrue(onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty())
}

private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.assertTextExists(text: String) {
    assertTrue(onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty())
}

private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.assertTextDoesNotExist(text: String) {
    assertTrue(onAllNodesWithText(text).fetchSemanticsNodes().isEmpty())
}
