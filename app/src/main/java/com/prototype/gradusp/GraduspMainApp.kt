package com.prototype.gradusp

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.prototype.gradusp.navigation.NavGraph
import com.prototype.gradusp.ui.components.BottomNavigationBar

@Composable
fun GraduspApp() {
    val navController = rememberNavController()
    Scaffold(
        bottomBar = { BottomNavigationBar(navController) }
    ) { padding ->
        NavGraph(navController, padding)
    }
}
