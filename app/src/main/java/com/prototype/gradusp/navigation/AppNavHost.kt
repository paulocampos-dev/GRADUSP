package com.prototype.gradusp.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.prototype.gradusp.ui.calendar.CalendarScreen
import com.prototype.gradusp.ui.settings.SettingsScreen
import com.prototype.gradusp.ui.grades.GradesScreen

sealed class Screen(val route: String) {
    data object Calendar : Screen("calendar_screen")
    data object Grades : Screen("grades_screen")
    data object Config : Screen("config_screen")
}

@Composable
fun NavGraph(
    navHostController: NavHostController,
    padding: PaddingValues,
    onNavigateToSettings: () -> Unit
) {
    NavHost(
        navController = navHostController,
        startDestination = Screen.Calendar.route,
        modifier = Modifier.padding(padding)
    ) {
        composable(route = Screen.Calendar.route) {
            CalendarScreen(onNavigateToSettings = onNavigateToSettings)
        }
        composable(route = "${Screen.Config.route}?highlight={highlight}",
            arguments = listOf(navArgument("highlight") { defaultValue = false; type = NavType.BoolType })) {
            val highlight = it.arguments?.getBoolean("highlight") ?: false
            SettingsScreen(highlightUspData = highlight)
        }
        composable(route = Screen.Grades.route) {
            GradesScreen()
        }
    }
}