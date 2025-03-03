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
import com.prototype.gradusp.viewmodel.SettingsViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

@Composable
fun GraduspApp(
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val context = LocalContext.current

    // State for welcome dialog
    var showWelcomeDialog by remember { mutableStateOf(false) }

    // Check if this is the first launch
    LaunchedEffect(key1 = "checkFirstLaunch") {
        val isFirstLaunch = FirstLaunchManager.isFirstLaunch(context).first()
        showWelcomeDialog = isFirstLaunch
    }

    Scaffold(
        bottomBar = { BottomNavigationBar(navController) }
    ) { padding ->
        NavGraph(navController, padding)
    }

    // Show welcome dialog if it's the first launch
    if (showWelcomeDialog) {
        WelcomeDialog(
            onDismiss = {
                showWelcomeDialog = false
                // Mark first launch as completed
                kotlinx.coroutines.MainScope().launch {
                    FirstLaunchManager.setFirstLaunchCompleted(context)
                }
            }
        )
    }
}