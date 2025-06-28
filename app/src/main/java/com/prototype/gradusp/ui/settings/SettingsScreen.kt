package com.prototype.gradusp.ui.settings

import android.text.Highlights
import androidx.compose.foundation.border
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.rememberUpdatedState
import com.prototype.gradusp.data.AnimationSpeed
import com.prototype.gradusp.ui.components.VersionDisplay
import com.prototype.gradusp.ui.components.config.SchoolSelectionModal
import com.prototype.gradusp.ui.theme.GRADUSPTheme
import com.prototype.gradusp.viewmodel.SettingsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    highlightUspData: Boolean = false,
    highlightSchoolSelection: Boolean = false
) {
    val animationSpeed by viewModel.animationSpeed.collectAsState(initial = AnimationSpeed.MÉDIA)
    val invertSwipe by viewModel.invertSwipeDirection.collectAsState(initial = false)
    val selectedSchools by viewModel.selectedSchools.collectAsState(initial = emptySet())
    val uiState by viewModel.uiState.collectAsState(initial = SettingsUiState())

    SettingsScreenContent(
        uiState = uiState,
        animationSpeed = animationSpeed,
        invertSwipe = invertSwipe,
        selectedSchools = selectedSchools,
        onAnimationSpeedSelected = viewModel::updateAnimationSpeed,
        onInvertSwipeChange = viewModel::updateInvertSwipeDirection,
        onSelectedSchoolsChange = viewModel::updateSelectedSchools,
        onTriggerUspDataUpdate = viewModel::triggerUspDataUpdate,
        highlightUspData = highlightUspData,
        highlightSchoolSelection = highlightSchoolSelection
    )
}

@Composable
private fun SettingsScreenContent(
    uiState: SettingsUiState,
    animationSpeed: AnimationSpeed,
    invertSwipe: Boolean,
    selectedSchools: Set<String>,
    onAnimationSpeedSelected: (AnimationSpeed) -> Unit,
    onInvertSwipeChange: (Boolean) -> Unit,
    onSelectedSchoolsChange: (Set<String>) -> Unit,
    onTriggerUspDataUpdate: () -> Unit,
    highlightUspData: Boolean,
    highlightSchoolSelection: Boolean
) {
    var showSchoolSelectionModal by remember { mutableStateOf(false) }

    if (showSchoolSelectionModal) {
        SchoolSelectionModal(
            campusMap = uiState.campusMap,
            selectedSchools = selectedSchools,
            onSelectionChanged = onSelectedSchoolsChange,
            onDismiss = { showSchoolSelectionModal = false }
        )
    }

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
            onSpeedSelected = onAnimationSpeedSelected
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
                onCheckedChange = onInvertSwipeChange
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
            val highlightSchoolSelection by rememberUpdatedState(highlightUspData)
            val schoolSelectionModifier = if (highlightSchoolSelection) {
                Modifier
                    .fillMaxWidth()
                    .border(2.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.medium)
                    .padding(2.dp)
            } else {
                Modifier.fillMaxWidth()
            }

            Button(
                onClick = { showSchoolSelectionModal = true },
                modifier = schoolSelectionModifier
            ) {
                Text("Selecionar Unidades")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (selectedSchools.isEmpty()) {
                    "Nenhuma unidade selecionada"
                } else {
                    "Unidades selecionadas: ${selectedSchools.joinToString()}"
                },
                style = MaterialTheme.typography.bodyMedium
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
                    onClick = onTriggerUspDataUpdate,
                    modifier = if (highlightSchoolSelection) {
                        Modifier
                            .fillMaxWidth()
                            .border(2.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.medium)
                            .padding(2.dp)
                    } else {
                        Modifier.fillMaxWidth()
                    },
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
                        color = if (result.startsWith("Erro")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
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
                Text(
                    "Apoie o Desenvolvedor",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("GRADUSP é um aplicativo gratuito e de código aberto. Se ele te ajuda, considere apoiar o desenvolvimento assistindo a um anúncio rápido.")
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

@Preview(showBackground = true, name = "Settings Screen")
@Composable
fun SettingsScreenPreview() {
    val sampleCampusMap = mapOf(
        "São Carlos" to listOf("Instituto de Ciências Matemáticas e de Computação (ICMC)", "Escola de Engenharia de São Carlos (EESC)"),
        "São Paulo" to listOf("Escola Politécnica (Poli)", "Faculdade de Economia, Administração e Contabilidade (FEA)")
    )

    GRADUSPTheme {
        SettingsScreenContent(
            uiState = SettingsUiState(
                campusMap = sampleCampusMap,
                isLoadingCampusData = false,
                lastUpdateTime = System.currentTimeMillis() - 1000 * 60 * 60 * 3, // 3 hours ago
                isUpdateInProgress = false,
                updateProgress = 0f,
                updateResult = null
            ),
            animationSpeed = AnimationSpeed.MÉDIA,
            invertSwipe = false,
            selectedSchools = setOf("Instituto de Ciências Matemáticas e de Computação (ICMC)"),
            onAnimationSpeedSelected = {},
            onInvertSwipeChange = {},
            onSelectedSchoolsChange = {},
            onTriggerUspDataUpdate = {},
            highlightUspData = true,
            highlightSchoolSelection = true
        )
    }
}

@Preview(showBackground = true, name = "Settings Screen (Updating)")
@Composable
fun SettingsScreenUpdatingPreview() {
    GRADUSPTheme {
        SettingsScreenContent(
            uiState = SettingsUiState(
                campusMap = emptyMap(),
                isLoadingCampusData = false,
                lastUpdateTime = System.currentTimeMillis() - 1000 * 60 * 60 * 24, // 1 day ago
                isUpdateInProgress = true,
                updateProgress = 0.65f,
                updateResult = null
            ),
            animationSpeed = AnimationSpeed.RÁPIDO,
            invertSwipe = true,
            selectedSchools = emptySet(),
            onAnimationSpeedSelected = {},
            onInvertSwipeChange = {},
            onSelectedSchoolsChange = {},
            onTriggerUspDataUpdate = {},
            highlightUspData = false,
    	    highlightSchoolSelection = false
        )
    }
}