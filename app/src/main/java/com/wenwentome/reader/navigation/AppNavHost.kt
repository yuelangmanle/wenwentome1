package com.wenwentome.reader.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.wenwentome.reader.di.AppContainer

@Composable
fun AppNavHost(
    navController: NavHostController,
    paddingValues: PaddingValues,
    appContainer: AppContainer,
) {
    NavHost(
        navController = navController,
        startDestination = TopLevelDestination.BOOKSHELF.name,
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
    ) {
        composable(TopLevelDestination.BOOKSHELF.name) {
            Text(text = TopLevelDestination.BOOKSHELF.label)
        }
        composable(TopLevelDestination.DISCOVER.name) {
            Text(text = TopLevelDestination.DISCOVER.label)
        }
        composable(TopLevelDestination.SETTINGS.name) {
            Text(text = TopLevelDestination.SETTINGS.label)
        }
    }
}
