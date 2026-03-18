package com.wenwentome.reader

import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.wenwentome.reader.di.AppContainer
import com.wenwentome.reader.navigation.AppNavHost
import com.wenwentome.reader.navigation.TopLevelDestination

@Composable
fun ReaderApp(
    appContainer: AppContainer,
    navController: NavHostController = rememberNavController(),
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    Scaffold(
        bottomBar = {
            NavigationBar {
                TopLevelDestination.entries.forEach { destination ->
                    val selected =
                        currentDestination
                            ?.hierarchy
                            ?.any { it.route == destination.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        modifier = Modifier.testTag("nav-${destination.route}"),
                        icon = {},
                        label = { Text(destination.label) },
                    )
                }
            }
        },
    ) { paddingValues ->
        AppNavHost(
            navController = navController,
            paddingValues = paddingValues,
            appContainer = appContainer,
        )
    }
}
