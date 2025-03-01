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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.prototype.gradusp.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch


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

    // For displaying parser results
    var parserResults by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

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

            Button(
                onClick = {
                    coroutineScope.launch {
                        val parser = UspParser(context)
                        try {
                            // Get campus and units
                            val campusUnits = parser.fetchCampusUnits()

                            // Display a sample of campus and units
                            val sampleCampus = campusUnits.entries.firstOrNull()
                            val campusInfo = sampleCampus?.let {
                                "Campus: ${it.key}\nUnidades: ${it.value.take(3).joinToString(", ")}..."
                            } ?: "Nenhum campus encontrado"

                            // Get sample course if we have units
                            val sampleUnit = sampleCampus?.value?.firstOrNull()
                            val unitCode = parser.getUnitCode(sampleUnit ?: "")

                            val courseInfo = if (!unitCode.isNullOrEmpty()) {
                                val courses = parser.fetchCoursesForUnit(unitCode)
                                val course = courses.firstOrNull()
                                course?.let {
                                    "\n\nCurso: ${it.name}\nCódigo: ${it.code}\nPeríodo: ${it.period}"
                                } ?: "\n\nNenhum curso encontrado"
                            } else {
                                "\n\nNenhuma unidade disponível"
                            }

                            // Get sample lecture if we have courses
                            val lectureInfo = if (!unitCode.isNullOrEmpty()) {
                                val sample = parser.getSampleLecture(unitCode)
                                sample?.let {
                                    "\n\nDisciplina: ${it.name}\nCódigo: ${it.code}\nCréditos: ${it.lectureCredits}"
                                } ?: "\n\nNenhuma disciplina encontrada"
                            } else {
                                "\n\nNenhuma disciplina disponível"
                            }

                            parserResults = campusInfo + courseInfo + lectureInfo
                        } catch (e: Exception) {
                            parserResults = "Erro ao executar o parser: ${e.message}"
                            Log.e("ConfigScreen", "Parser error", e)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Testar Parser USP")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Display parser results
            parserResults?.let {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Resultados do Parser:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
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