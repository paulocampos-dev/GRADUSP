package com.prototype.gradusp.ui.settings

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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.prototype.gradusp.data.AnimationSpeed
import com.prototype.gradusp.ui.components.VersionDisplay
import com.prototype.gradusp.ui.components.config.SchoolSelectionCard
import com.prototype.gradusp.viewmodel.SettingsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val animationSpeed by viewModel.animationSpeed.collectAsState(initial = AnimationSpeed.MÉDIA)
    val invertSwipe by viewModel.invertSwipeDirection.collectAsState(initial = false)
    val selectedSchools by viewModel.selectedSchools.collectAsState(initial = emptySet())
    val uiState by viewModel.uiState.collectAsState(initial = SettingsUiState())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Configurações de Animação",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        AnimationSpeedDropdown(
            selectedSpeed = animationSpeed,
            onSpeedSelected = viewModel::updateAnimationSpeed
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
                checked = invertSwipe,
                onCheckedChange = viewModel::updateInvertSwipeDirection
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

        if (uiState.isLoadingCampusData) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Carregando unidades...")
            }
        } else if (uiState.campusMap.isNotEmpty()) {
            SchoolSelectionCard(
                campusMap = uiState.campusMap,
                selectedSchools = selectedSchools,
                onSelectionChanged = viewModel::updateSelectedSchools,
                isLoading = uiState.isUpdateInProgress
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

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

                if (uiState.lastUpdateTime > 0) {
                    val date = Date(uiState.lastUpdateTime)
                    val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))
                    Text(
                        text = "Última atualização: ${formatter.format(date)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (uiState.isUpdateInProgress) {
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
                        Text(
                            text = "${(uiState.updateProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { uiState.updateProgress },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Button(
                    onClick = viewModel::triggerUspDataUpdate,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isUpdateInProgress
                ) {
                    if (uiState.isUpdateInProgress) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(if (uiState.isUpdateInProgress) "Atualizando..." else "Atualizar Dados")
                }

                uiState.updateResult?.let { result ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = result,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (result.startsWith("Falha")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.padding(16.dp))

        Text(
            text = "Apoio",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Apoie o Desenvolvedor", /* ... */)
                Spacer(modifier = Modifier.height(16.dp))
                Text("GRADUSP é um aplicativo gratuito...", /* ... */)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { /* TODO */ }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Assistir um anúncio para apoiar")
                }
            }
        }

        VersionDisplay()
    }
}

@Composable
fun AnimationSpeedDropdown(
    selectedSpeed: AnimationSpeed,
    onSpeedSelected: (AnimationSpeed) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Velocidade: ${selectedSpeed.name}",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge
            )
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
            AnimationSpeed.entries.forEach { speed ->
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