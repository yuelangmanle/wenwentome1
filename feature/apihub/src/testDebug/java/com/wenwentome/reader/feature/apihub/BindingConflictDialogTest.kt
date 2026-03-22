package com.wenwentome.reader.feature.apihub

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.wenwentome.reader.core.model.ApiCapabilityBinding
import com.wenwentome.reader.data.apihub.sync.PendingSyncConflict
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BindingConflictDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun choosingResolution_invokesCallbacks() {
        var keptLocal = false
        var usedRemote = false

        composeTestRule.setContent {
            BindingConflictDialog(
                conflict =
                    PendingSyncConflict(
                        capabilityId = "reader.summary",
                        local =
                            ApiCapabilityBinding(
                                capabilityId = "reader.summary",
                                primaryProviderId = "provider-a",
                                primaryModelId = "model-a",
                                updatedAt = 1_000L,
                            ),
                        remote =
                            ApiCapabilityBinding(
                                capabilityId = "reader.summary",
                                primaryProviderId = "provider-b",
                                primaryModelId = "model-b",
                                updatedAt = 1_500L,
                            ),
                    ),
                onDismissRequest = {},
                onKeepLocal = { keptLocal = true },
                onUseRemote = { usedRemote = true },
            )
        }

        composeTestRule.onNodeWithText("绑定冲突").assertTextEquals("绑定冲突")
        composeTestRule.onNodeWithText("保留本地").performClick()
        composeTestRule.onNodeWithText("采用新方案").performClick()

        assertTrue(keptLocal)
        assertTrue(usedRemote)
    }
}
