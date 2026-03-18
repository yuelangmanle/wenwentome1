package com.wenwentome.reader.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.wenwentome.reader.di.AppContainer
import com.wenwentome.reader.feature.library.LibraryScreen
import com.wenwentome.reader.feature.library.LibraryViewModel
import com.wenwentome.reader.feature.library.ObserveBookshelfUseCase

@Composable
fun AppNavHost(
    navController: NavHostController,
    paddingValues: PaddingValues,
    appContainer: AppContainer,
) {
    NavHost(
        navController = navController,
        startDestination = TopLevelDestination.BOOKSHELF.route,
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
    ) {
        composable(TopLevelDestination.BOOKSHELF.route) {
            val viewModel: LibraryViewModel = remember(appContainer) {
                LibraryViewModel(
                    observeBookshelf = ObserveBookshelfUseCase.from(appContainer.database.bookRecordDao()),
                    importLocalBook = { uri -> appContainer.importLocalBook(uri) },
                )
            }
            LibraryScreen(viewModel = viewModel)
        }
        composable(TopLevelDestination.DISCOVER.route) {
            androidx.compose.material3.Text(text = TopLevelDestination.DISCOVER.label)
        }
        composable(TopLevelDestination.SETTINGS.route) {
            androidx.compose.material3.Text(text = TopLevelDestination.SETTINGS.label)
        }
    }
}
