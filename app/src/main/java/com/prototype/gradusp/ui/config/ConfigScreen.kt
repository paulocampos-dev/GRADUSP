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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.prototype.gradusp.data.AnimationSpeed
import com.prototype.gradusp.data.UserPreferencesRepository
import com.prototype.gradusp.data.repository.UspDataRepository
import com.prototype.gradusp.ui.components.base.GraduspTitleBar
import com.prototype.gradusp.ui.components.config.SchoolSelectionCard
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
    val animationSpeedFlow = settingsViewModel.animationSpeed.collectAsState(initial = AnimationSpeed.MÉDIA)
    val invertSwipeDirection = settingsViewModel.invertSwipeDirection.collectAsState(initial = false)
    val selectedSchools = settingsViewModel.selectedSchools.collectAsState(initial = emptySet())
    val coroutineScope = rememberCoroutineScope()

    // USP Data Repository setup
    val context = LocalContext.current
    val userPreferencesRepository = remember { UserPreferencesRepository(context) }
    val uspDataRepository = remember { UspDataRepository(context, userPreferencesRepository) }

    var lastUpdateTime by remember { mutableStateOf(0L) }
    var isUpdateInProgress by remember { mutableStateOf(false) }
    var updateResult by remember { mutableStateOf<String?>(null) }
    var updateProgress by remember { mutableStateOf(0f) }  // New state for progress tracking

    // State for campus/schools data
    var campusMap by remember { mutableStateOf<Map<String, List<String>>>(emptyMap()) }
    var isLoadingCampusData by remember { mutableStateOf(false) }

    // Collect flows
    LaunchedEffect(uspDataRepository) {
        uspDataRepository.lastUpdateTime.collect { lastUpdateTime = it }
    }

    LaunchedEffect(uspDataRepository) {
        uspDataRepository.isUpdateInProgress.collect { isUpdateInProgress = it }
    }

    LaunchedEffect(uspDataRepository) {
        uspDataRepository.updateProgress.collect { updateProgress = it }
    }

    // Load campus data
    LaunchedEffect(Unit) {
        isLoadingCampusData = true
        try {
            campusMap = uspDataRepository.getCampusUnits()
        } catch (e: Exception) {
            Log.e("ConfigScreen", "Error loading campus data", e)
        } finally {
            isLoadingCampusData = false
        }
    }

    Scaffold(
        topBar = {
            GraduspTitleBar(screenTitle = "Configurações")
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

            Text(
                text = "Dados da USP",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // School selection card
            if (isLoadingCampusData) {
                // Show loading indicator while campus data is being loaded
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Carregando unidades...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else if (campusMap.isNotEmpty()) {
                SchoolSelectionCard(
                    campusMap = campusMap,
                    selectedSchools = selectedSchools.value,
                    onSelectionChanged = { newSelection ->
                        settingsViewModel.updateSelectedSchools(newSelection)
                    },
                    isLoading = isUpdateInProgress
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            // USP Data update card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Atualização de Dados",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
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

                    // Progress indicator for updates
                    if (isUpdateInProgress) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Atualizando dados da USP...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f)
                                )

                                // Show percentage
                                Text(
                                    text = "${(updateProgress * 100).toInt()}%",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Determinate progress indicator
                            LinearProgressIndicator(
                                progress = { updateProgress },
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )

                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    // Status message based on selected schools
                    if (!isUpdateInProgress) {
                        val selectedCount = selectedSchools.value.size

                        if (selectedCount > 0) {
                            Text(
                                text = "Serão sincronizadas $selectedCount ${if (selectedCount == 1) "unidade" else "unidades"}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = "Nenhuma unidade específica selecionada. Amostra de dados será atualizada.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Update button
                    Button(
                        onClick = {
                            updateResult = null
                            updateProgress = 0f
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
                            text = if (isUpdateInProgress) "Atualizando..." else
                                if (selectedSchools.value.isEmpty()) "Atualizar Amostra de Dados"
                                else "Atualizar Dados Selecionados"
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
                        text = if (selectedSchools.value.isEmpty())
                            "Selecione unidades específicas para atualizar apenas os dados relevantes e reduzir o tempo de sincronização."
                        else
                            "Atualizar os dados da USP permite adicionar matérias mais rapidamente, sem precisar buscar online toda vez.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
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