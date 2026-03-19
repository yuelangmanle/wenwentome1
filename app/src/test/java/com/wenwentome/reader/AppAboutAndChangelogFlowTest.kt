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
class AppAboutAndChangelogFlowTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun settingsShowsAboutCardAndNavigatesToChangelog() {
        val appContainer = AppContainer(ApplicationProvider.getApplicationContext())

        composeTestRule.setContent {
            ReaderApp(appContainer = appContainer)
        }

        composeTestRule.onNodeWithTag("nav-settings").performClick()
        composeTestRule.onNodeWithText("关于与项目").assertTextEquals("关于与项目")
        composeTestRule.onNodeWithText("作者：月亮满了").assertTextEquals("作者：月亮满了")
        composeTestRule.onNodeWithText("版本 1.0").assertTextEquals("版本 1.0")
        composeTestRule.onNodeWithText("查看完整更新日志").performClick()
        composeTestRule.onNodeWithText("更新日志").assertTextEquals("更新日志")
        composeTestRule.onNodeWithText("v1.0").assertTextEquals("v1.0")
    }
}
