package com.prototype.gradusp.ui.config

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.prototype.gradusp.data.AnimationSpeed
import com.prototype.gradusp.data.parser.UspParser
import com.prototype.gradusp.data.repository.UspDataRepository
import com.prototype.gradusp.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(
    navController: NavController
) {
    // Read the current values from SettingsManager - it will update when values change
    val settingsViewModel : SettingsViewModel = hiltViewModel()
    val animationSpeedFlow = settingsViewModel.animationSpeed.collectAsState(initial = AnimationSpeed.MEDIUM)
    val invertSwipeDirection = settingsViewModel.invertSwipeDirection.collectAsState(initial = false)
    val coroutineScope = rememberCoroutineScope()

    // USP Data Repository setup
    val context = LocalContext.current
    val uspDataRepository = remember { UspDataRepository(context) }
    var lastUpdateTime by remember { mutableStateOf(0L) }
    var isUpdateInProgress by remember { mutableStateOf(false) }
    var updateResult by remember { mutableStateOf<String?>(null) }

    // Collect flows
    LaunchedEffect(uspDataRepository) {
        uspDataRepository.lastUpdateTime.collect { lastUpdateTime = it }
    }

    LaunchedEffect(uspDataRepository) {
        uspDataRepository.isUpdateInProgress.collect { isUpdateInProgress = it }
    }

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
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Configurações de Animação",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            AnimationSpeedDropdown(
                selectedSpeed = animationSpeedFlow.value,
                onSpeedSelected = { speed ->
                    settingsViewModel.updateAnimationSpeed(speed)
                }
            )

            Spacer(modifier = Modifier.padding(16.dp))

            HorizontalDivider()

            Spacer(modifier = Modifier.padding(16.dp))

            Text(
                text = "Configurações de Swipe",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Inverter direção de deslize",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = invertSwipeDirection.value,
                    onCheckedChange = { inverted ->
                        settingsViewModel.updateInvertSwipeDirection(inverted)
                    }
                )
            }

            Spacer(modifier = Modifier.padding(16.dp))

            HorizontalDivider()

            Spacer(modifier = Modifier.padding(16.dp))

            // Add parser test section
            Text(
                text = "Teste do Parser USP",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            HorizontalDivider()

            Spacer(modifier = Modifier.padding(16.dp))

            // USP Data section
            Text(
                text = "Dados da USP",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Last update time
            if (lastUpdateTime > 0) {
                val lastUpdateDate = Date(lastUpdateTime)
                val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))

                Text(
                    text = "Última atualização: ${formatter.format(lastUpdateDate)}",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(8.dp))
            }

            // Update button
            Button(
                onClick = {
                    updateResult = null
                    coroutineScope.launch {
                        val success = uspDataRepository.updateUspData()
                        updateResult = if (success) {
                            "Dados atualizados com sucesso!"
                        } else {
                            "Falha ao atualizar dados. Tente novamente."
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isUpdateInProgress
            ) {
                if (isUpdateInProgress) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Text(
                    text = if (isUpdateInProgress) "Atualizando..." else "Atualizar Dados da USP"
                )
            }

            // Update result message
            updateResult?.let {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (it.startsWith("Falha"))
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary
                )
            }

            // Info text
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Atualizar os dados da USP permite adicionar matérias mais rapidamente, sem precisar buscar online toda vez.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

        }
    }
}



@Composable
fun AnimationSpeedDropdown(
    selectedSpeed: AnimationSpeed,
    onSpeedSelected: (AnimationSpeed) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Velocidade: ${selectedSpeed.name}",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Dropdown Arrow"
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            AnimationSpeed.values().forEach { speed ->
                DropdownMenuItem(
                    text = { Text(speed.name) },
                    onClick = {
                        onSpeedSelected(speed)
                        expanded = false
                    }
                )
            }
        }
    }
}