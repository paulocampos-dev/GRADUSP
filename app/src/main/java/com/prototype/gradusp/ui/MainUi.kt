package com.prototype.gradusp.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.prototype.gradusp.navigation.NavGraph
import com.prototype.gradusp.navigation.Screen
import com.prototype.gradusp.ui.components.base.BottomNavigationBar
import com.prototype.gradusp.ui.components.base.GraduspTitleBar
import com.prototype.gradusp.ui.components.dialogs.WelcomeDialog
import com.prototype.gradusp.viewmodel.MainViewModel

@Composable
fun MainUi(
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val uiState by mainViewModel.uiState.collectAsState()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val currentScreenTitle = when (currentRoute) {
        Screen.Calendar.route -> "Calendário"
        Screen.Grades.route -> "Notas"
        Screen.Config.route -> "Configurações"
        else -> "GRADUSP"
    }

    Scaffold(
        topBar = { GraduspTitleBar(screenTitle = currentScreenTitle) },
        bottomBar = { BottomNavigationBar(navController) },
        floatingActionButton = {
            if (currentRoute == Screen.Grades.route) {
                FloatingActionButton(
                    onClick = {
                        navController.currentBackStackEntry?.savedStateHandle?.set("show_add_course_dialog", true)
                    }
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Adicionar Disciplina")
                }
            }
        }
    ) { padding ->
        NavGraph(navController, padding)
    }

    if (uiState.showWelcomeDialog) {
        WelcomeDialog(
            onDismiss = {
                mainViewModel.onWelcomeDialogDismissed()
            }
        )
    }
}