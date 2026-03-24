package com.wenwentome.reader.feature.apihub

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
                    providerStatusLabel = "已配置 3 个可用接口",
                    bindingStatusLabel = "已绑定 6 项能力",
                    usageStatusLabel = "今天已调用 12 次",
                    enabledProviderCount = 3,
                    boundCapabilityCount = 6,
                    todayCallCount = 12,
                    latestError = "无",
                ),
                onOpenProviders = {},
                onOpenBindings = {},
                onOpenBudgets = {},
                onOpenPrices = {},
                onOpenUsageLogs = {},
            )
        }

        composeTestRule.onNodeWithText("配置 AI").assertExistsCompat()
        composeTestRule.onNodeWithText("已配置 3 个可用接口").assertExistsCompat()
        composeTestRule.onNodeWithText("已绑定 6 项能力").assertExistsCompat()
        composeTestRule.onNodeWithText("今天已调用 12 次").assertExistsCompat()
        composeTestRule.onNodeWithText("无").assertExistsCompat()
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

private fun androidx.compose.ui.test.SemanticsNodeInteraction.assertExistsCompat() {
    fetchSemanticsNode()
}
