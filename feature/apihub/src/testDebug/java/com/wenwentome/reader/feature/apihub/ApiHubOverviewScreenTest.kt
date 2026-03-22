package com.wenwentome.reader.feature.apihub

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ApiHubOverviewScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun rendersOverviewPlaceholderMetrics() {
        composeTestRule.setContent {
            ApiHubOverviewScreen(
                state = ApiHubUiState(
                    enabledProviderCount = 3,
                    boundCapabilityCount = 6,
                    todayCallCount = 12,
                    budgetUsageRatio = 0.35f,
                    latestError = "无",
                ),
                onOpenProviders = {},
                onOpenBindings = {},
                onOpenBudgets = {},
            )
        }

        composeTestRule.onNodeWithTag("api-hub-overview-screen").assertExists()
        composeTestRule.onNodeWithText("今日调用").assertExists()
        composeTestRule.onNodeWithText("12").assertExists()
        composeTestRule.onNodeWithText("35%").assertExists()
        composeTestRule.onNodeWithText("无").assertExists()
    }
}
