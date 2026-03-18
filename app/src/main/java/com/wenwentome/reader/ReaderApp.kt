package com.wenwentome.reader

import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.wenwentome.reader.di.AppContainer
import com.wenwentome.reader.navigation.AppNavHost
import com.wenwentome.reader.navigation.TopLevelDestination

@Composable
fun ReaderApp(appContainer: AppContainer) {
    val navController = rememberNavController()
    Scaffold(
        bottomBar = {
            NavigationBar {
                TopLevelDestination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = false,
                        onClick = { navController.navigate(destination.name) },
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
