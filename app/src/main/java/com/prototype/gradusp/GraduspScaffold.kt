package com.prototype.gradusp

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.prototype.gradusp.navigation.NavGraph
import com.prototype.gradusp.ui.components.base.BottomNavigationBar
import com.prototype.gradusp.ui.components.dialogs.WelcomeDialog
import com.prototype.gradusp.utils.FirstLaunchManager
import com.prototype.gradusp.viewmodel.MainViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun GraduspScaffold(
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val uiState by mainViewModel.uiState.collectAsState()

    Scaffold(
        bottomBar = { BottomNavigationBar(navController) }
    ) { padding ->
        NavGraph(navController, padding)
    }

    // Show welcome dialog if it's the first launch
    if (uiState.showWelcomeDialog) {
        WelcomeDialog(
            onDismiss = {
                mainViewModel.onWelcomeDialogDismissed()
            }
        )
    }
}