package com.wenwentome.reader

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.wenwentome.reader.di.AppContainer
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppSmokeFlowTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun appShell_topLevelRoutesRemainReachable() {
        val appContainer = AppContainer(ApplicationProvider.getApplicationContext())

        composeTestRule.setContent {
            ReaderApp(appContainer = appContainer)
        }

        composeTestRule.onNodeWithTag("screen").assertTextEquals("书库")
        composeTestRule.onNodeWithText("发现").performClick()
        composeTestRule.onNodeWithText("我的").performClick()
        composeTestRule.onNodeWithText("立即备份").assertTextEquals("立即备份")
    }
}
