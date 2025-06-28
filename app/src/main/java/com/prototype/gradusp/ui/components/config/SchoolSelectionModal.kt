package com.prototype.gradusp.ui.components.config

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.prototype.gradusp.ui.theme.GRADUSPTheme

@Composable
fun SchoolSelectionModal(
    campusMap: Map<String, List<String>>,
    selectedSchools: Set<String>,
    onSelectionChanged: (Set<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredCampusMap = if (searchQuery.isBlank()) {
        campusMap
    } else {
        campusMap.mapValues { (_, schools) ->
            schools.filter { it.contains(searchQuery, ignoreCase = true) }
        }.filter { it.value.isNotEmpty() }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f) // Occupy 90% of the width
                .fillMaxHeight(0.8f) // Occupy 80% of the height
//                .padding(4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Selecione suas unidades", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Pesquisar") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn(modifier = Modifier.weight(1f)) {
                    filteredCampusMap.forEach { (campus, schools) ->
                        item {
                            Text(
                                text = campus,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(schools) { school ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = selectedSchools.contains(school),
                                    onCheckedChange = {
                                        val newSelection = selectedSchools.toMutableSet()
                                        if (it) {
                                            newSelection.add(school)
                                        } else {
                                            newSelection.remove(school)
                                        }
                                        onSelectionChanged(newSelection)
                                    }
                                )
                                Text(text = school, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(androidx.compose.ui.Alignment.End)
                ) {
                    Text("Fechar")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewSchoolSelectionModal() {
    val sampleCampusMap = mapOf(
        "São Carlos" to listOf("Instituto de Ciências Matemáticas e de Computação (ICMC)", "Escola de Engenharia de São Carlos (EESC)"),
        "São Paulo" to listOf("Escola Politécnica (Poli)", "Faculdade de Economia, Administração e Contabilidade (FEA)")
    )
    GRADUSPTheme { // Assuming GRADUSPTheme is your custom theme
        SchoolSelectionModal(
            campusMap = sampleCampusMap,
            selectedSchools = setOf("Instituto de Ciências Matemáticas e de Computação (ICMC)"),
            onSelectionChanged = {},
            onDismiss = {}
        )
    }
}