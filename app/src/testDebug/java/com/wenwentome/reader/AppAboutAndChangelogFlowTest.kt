package com.wenwentome.reader

import androidx.compose.runtime.SideEffect
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import com.wenwentome.reader.di.AppContainer
import org.junit.Assert.assertEquals
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
        var navController: NavHostController? = null

        composeTestRule.setContent {
            val controller = rememberNavController()
            SideEffect { navController = controller }
            ReaderApp(appContainer = appContainer, navController = controller)
        }

        composeTestRule.onNodeWithTag("nav-settings").performClick()
        composeTestRule.onNodeWithText("关于与项目").assertTextEquals("关于与项目")
        composeTestRule.onNodeWithText("作者：月亮满了").assertTextEquals("作者：月亮满了")
        composeTestRule.onNodeWithText("版本 1.0").assertTextEquals("版本 1.0")
        composeTestRule.onNodeWithTag("open-changelog-button").performScrollTo().performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            navController?.currentDestination?.route == "settings/changelog"
        }
        composeTestRule.runOnIdle {
            assertEquals("settings/changelog", navController?.currentDestination?.route)
        }
        composeTestRule.onNodeWithTag("nav-settings").assertIsSelected()
    }
}
