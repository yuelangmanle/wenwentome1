package com.wenwentome.reader.feature.apihub

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Assert.assertTrue
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
                onOpenPrices = {},
                onOpenUsageLogs = {},
            )
        }

        composeTestRule.onNodeWithText("12").assertTextEquals("12")
        composeTestRule.onNodeWithText("35%").assertTextEquals("35%")
        composeTestRule.onNodeWithText("无").assertTextEquals("无")
    }

    @Test
    fun actionButtons_invokeCallbacks() {
        var openedProviders = false
        var openedBindings = false
        var openedBudgets = false
        var openedPrices = false
        var openedUsageLogs = false

        composeTestRule.setContent {
            ApiHubOverviewScreen(
                state = ApiHubUiState(),
                onOpenProviders = { openedProviders = true },
                onOpenBindings = { openedBindings = true },
                onOpenBudgets = { openedBudgets = true },
                onOpenPrices = { openedPrices = true },
                onOpenUsageLogs = { openedUsageLogs = true },
            )
        }

        composeTestRule.onNodeWithTag("api-hub-open-providers").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("api-hub-open-bindings").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("api-hub-open-budgets").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("api-hub-open-prices").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("api-hub-open-usage-logs").performScrollTo().performClick()

        assertTrue(openedProviders)
        assertTrue(openedBindings)
        assertTrue(openedBudgets)
        assertTrue(openedPrices)
        assertTrue(openedUsageLogs)
    }
}
