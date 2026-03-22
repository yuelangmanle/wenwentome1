package com.wenwentome.reader

import androidx.compose.runtime.SideEffect
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertExists
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
class AppNavigationSmokeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun bottomBar_topLevelNavigationBehavesLikeTabs() {
        val appContainer = AppContainer(ApplicationProvider.getApplicationContext())
        var navController: NavHostController? = null

        composeTestRule.setContent {
            // 把 NavController 暴露给测试，验证重复点击当前 tab 不会堆叠相同目的地。
            val controller = rememberNavController()
            SideEffect { navController = controller }
            ReaderApp(appContainer = appContainer, navController = controller)
        }

        // 初始状态：书库选中 + 屏幕内容为书库
        composeTestRule.onNodeWithTag("nav-bookshelf").assertIsSelected()
        composeTestRule.onNodeWithTag("nav-discover").assertIsNotSelected()
        composeTestRule.onNodeWithTag("nav-settings").assertIsNotSelected()
        composeTestRule.onNodeWithTag("screen").assertTextEquals("书库")

        // 切换到发现
        composeTestRule.onNodeWithTag("nav-discover").performClick()
        composeTestRule.onNodeWithTag("nav-discover").assertIsSelected()
        composeTestRule.onNodeWithTag("screen").assertTextEquals("发现")

        // 重复点击当前 tab 后，向上导航应该直接回到书库，而不是停留在第二个 discover。
        composeTestRule.onNodeWithTag("nav-discover").performClick()
        composeTestRule.runOnIdle {
            assertEquals(true, navController!!.navigateUp())
        }
        composeTestRule.onNodeWithTag("nav-bookshelf").assertIsSelected()
        composeTestRule.onNodeWithTag("screen").assertTextEquals("书库")
    }

    @Test
    fun settings_canOpenApiHubOverview() {
        val appContainer = AppContainer(ApplicationProvider.getApplicationContext())

        composeTestRule.setContent {
            val controller = rememberNavController()
            ReaderApp(appContainer = appContainer, navController = controller)
        }

        composeTestRule.onNodeWithTag("nav-settings").performClick()
        composeTestRule.onNodeWithText("API 中心").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("api-hub-overview-screen").assertExists()
        composeTestRule.onNodeWithText("今日调用").assertExists()
        composeTestRule.onNodeWithTag("nav-settings").assertIsSelected()
    }
}
