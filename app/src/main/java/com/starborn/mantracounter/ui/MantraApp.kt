package com.starborn.mantracounter.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.starborn.mantracounter.ui.screens.ArchiveScreen
import com.starborn.mantracounter.ui.screens.CounterScreen
import com.starborn.mantracounter.ui.screens.JapaListScreen
import com.starborn.mantracounter.ui.screens.HistoryScreen
import com.starborn.mantracounter.ui.screens.SettingsScreen
import com.starborn.mantracounter.ui.screens.StatsScreen

private object Routes {
    const val LIST = "list"
    const val ARCHIVE = "archive"
    const val SETTINGS = "settings"
    const val HISTORY = "history"
    const val STATS = "stats"
    const val COUNTER = "counter/{japaId}"
    fun counter(id: Long) = "counter/$id"
}

@Composable
fun MantraApp() {
    val navController = rememberNavController()
    // One ViewModel for the whole graph so the search query and DB flows are shared.
    val viewModel: JapaViewModel = viewModel()

    NavHost(navController = navController, startDestination = Routes.LIST) {
        composable(Routes.LIST) {
            JapaListScreen(
                viewModel = viewModel,
                onOpenJapa = { navController.navigate(Routes.counter(it)) },
                onOpenArchive = { navController.navigate(Routes.ARCHIVE) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenStats = { navController.navigate(Routes.STATS) },
            )
        }
        composable(Routes.ARCHIVE) {
            ArchiveScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onOpenJapa = { navController.navigate(Routes.counter(it)) },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onOpenHistory = { navController.navigate(Routes.HISTORY) },
            )
        }
        composable(Routes.STATS) {
            StatsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.HISTORY) {
            HistoryScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.COUNTER,
            arguments = listOf(navArgument("japaId") { type = NavType.LongType }),
        ) { entry ->
            CounterScreen(
                japaId = entry.arguments?.getLong("japaId") ?: 0L,
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
