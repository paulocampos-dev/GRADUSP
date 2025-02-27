package com.prototype.gradusp.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.prototype.gradusp.data.AnimationSpeed
import com.prototype.gradusp.viewmodel.SettingsViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(
    navController: NavController
) {
    // Read the current values from SettingsManager - it will update when values change
    val settingsViewModel : SettingsViewModel = hiltViewModel()
    val animationSpeedFlow = settingsViewModel.animationSpeed.collectAsState(initial = AnimationSpeed.MEDIUM)
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configurações") }
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = "Configurações de Animação",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            AnimationSpeedSelection(
                selectedSpeed = animationSpeedFlow.value,
                onSpeedSelected = { speed ->
                    settingsViewModel.updateAnimationSpeed(speed)
                }
            )

        }
    }
}


@Composable
fun AnimationSpeedSelection(selectedSpeed: AnimationSpeed, onSpeedSelected: (AnimationSpeed) -> Unit) {
    Column {
        AnimationSpeed.values().forEach { speed ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = (speed == selectedSpeed),
                    onClick = { onSpeedSelected(speed) }
                )
                Text(
                    text = speed.name,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewConfigScreen() {
    val navController = rememberNavController()
    ConfigScreen(navController)
}
