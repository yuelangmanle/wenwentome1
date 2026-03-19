package com.wenwentome.reader.feature.settings

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ChangelogScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun rendersStaticTitleAndVersionEntry() {
        composeTestRule.setContent {
            ChangelogScreen(
                state = ChangelogUiState(
                    entries = listOf(
                        ChangelogEntry(
                            version = "1.0",
                            releaseDate = "2026-03-19",
                            title = "首发版本",
                            highlights = listOf("统一书库"),
                            details = listOf("支持 TXT / EPUB"),
                        ),
                    ),
                ),
            )
        }

        composeTestRule.onNodeWithTag("screen").assertTextEquals("更新日志")
        composeTestRule.onNodeWithText("v1.0").assertTextEquals("v1.0")
    }
}
