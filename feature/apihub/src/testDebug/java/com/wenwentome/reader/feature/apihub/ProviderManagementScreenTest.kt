package com.wenwentome.reader.feature.apihub

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ProviderManagementScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun providerCard_validateButtonCallsBack() {
        var validatedProviderId: String? = null

        composeTestRule.setContent {
            ProviderManagementScreen(
                state =
                    ApiHubUiState(
                        providerCards =
                            listOf(
                                ProviderCardUiState(
                                    providerId = "openai-main",
                                    displayName = "OpenAI 主线",
                                    templateLabel = "OpenAI Compatible",
                                    modelSourceLabel = "远端发现",
                                    validationLabel = "未校验",
                                    enabled = true,
                                    availableModelCount = 8,
                                ),
                            ),
                    ),
                onValidateProvider = { validatedProviderId = it },
                onToggleProviderEnabled = { _, _ -> },
                onAddProvider = {},
                onSelectProvider = {},
            )
        }

        composeTestRule.onNodeWithText("OpenAI 主线").assertTextContains("OpenAI 主线")
        composeTestRule.onNodeWithText("OpenAI Compatible").assertTextContains("OpenAI Compatible")
        composeTestRule.onNodeWithText("远端发现").assertTextContains("远端发现")
        composeTestRule.onNodeWithText("校验").performClick()

        assertEquals("openai-main", validatedProviderId)
    }
}
