package com.wenwentome.reader

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertExists
import androidx.test.core.app.ApplicationProvider
import com.wenwentome.reader.di.AppContainer
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppNavigationSmokeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun bottomBar_showsThreeTopLevelDestinations() {
        val appContainer = AppContainer(ApplicationProvider.getApplicationContext())
        composeTestRule.setContent { ReaderApp(appContainer = appContainer) }
        composeTestRule.onNodeWithText("书库").assertExists()
        composeTestRule.onNodeWithText("发现").assertExists()
        composeTestRule.onNodeWithText("我的").assertExists()
    }
}
