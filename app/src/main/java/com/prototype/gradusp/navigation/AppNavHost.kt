package com.prototype.gradusp.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.prototype.gradusp.ui.screens.CalendarScreen

sealed class Screen(val route: String) {
    data object Calendar : Screen("calendar_screen")
    data object Notes : Screen("notes_screen")
    data object Config : Screen("config_screen")
}

@Composable
fun NavGraph(
    navHostController: NavHostController,
    padding: PaddingValues
) {
    NavHost(
        navController = navHostController,
        startDestination = Screen.Calendar.route,
        modifier = Modifier.padding(padding)
    ) {
        composable(route = Screen.Calendar.route) {
            CalendarScreen(navHostController)
        }
//        composable(route = Screen.Notes.route) {
//            NotesScreen(navController)
//        }
//        composable(route = Screen.Config.route) {
//            ConfigScreen(navController)
//        }
    }
}