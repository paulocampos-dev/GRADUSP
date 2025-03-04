package com.prototype.gradusp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.prototype.gradusp.data.model.CourseGrade
import com.prototype.gradusp.data.model.GradeEntry
import java.text.DecimalFormat

@Composable
fun CourseCard(
    course: CourseGrade,
    onUpdateName: (String) -> Unit,
    onUpdateNotes: (String) -> Unit,
    onAddGradeEntry: (String, Float, Float) -> Unit,
    onUpdateGradeEntry: (GradeEntry) -> Unit,
    onRemoveGradeEntry: (String) -> Unit,
    onRemoveCourse: () -> Unit
) {
    var expanded by remember { mutableStateOf(true) }
    var courseName by remember(course.name) { mutableStateOf(course.name) }
    var courseNotes by remember(course.notes) { mutableStateOf(course.notes) }
    var showNotesSection by remember { mutableStateOf(false) }

    // For adding new grade entries
    var newEntryName by remember { mutableStateOf("") }
    var newEntryGrade by remember { mutableStateOf("") }
    var newEntryMultiplier by remember { mutableStateOf("1.0") }
    var showAddGradeForm by remember { mutableStateOf(false) }

    // For formatting the final grade
    val decimalFormat = remember { DecimalFormat("#.##") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Course header with expand/collapse and delete options
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Editable course name
                OutlinedTextField(
                    value = courseName,
                    onValueChange = {
                        courseName = it
                        onUpdateName(it)
                    },
                    label = { Text("Nome da disciplina") },
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Expand/collapse button
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) "Recolher" else "Expandir"
                    )
                }

                // Delete course button
                IconButton(onClick = onRemoveCourse) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remover curso",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Final grade display
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Nota Final:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = course.calculateFinalGrade().toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.weight(1f))

                // Notes button
                TextButton(
                    onClick = { showNotesSection = !showNotesSection },
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Anotações",
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text(
                        text = if (showNotesSection) "Ocultar Anotações" else "Mostrar Anotações"
                    )
                }
            }

            // Notes section
            AnimatedVisibility(
                visible = showNotesSection,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "Anotações sobre Cálculo da Nota",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = courseNotes,
                        onValueChange = {
                            courseNotes = it
                            onUpdateNotes(it)
                        },
                        label = { Text("Descreva como a nota da disciplina é calculada") },
                        placeholder = { Text("Ex: Média = (Prova1 + Prova2 + Prova3)/3, precisa de 5.0 para passar") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        minLines = 3,
                        maxLines = 5
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                }
            }

            // Expanded content - grade entries
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // Header for grade entries
                    if (course.gradeEntries.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Atividade",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(2.5f)
                            )

                            Text(
                                text = "Nota",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )

                            Text(
                                text = "Peso",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )

                            // Space for delete button
                            Spacer(modifier = Modifier.weight(0.5f))
                        }

                        // Grade entries list
                        course.gradeEntries.forEach { entry ->
                            GradeEntryRow(
                                entry = entry,
                                onUpdate = onUpdateGradeEntry,
                                onRemove = { onRemoveGradeEntry(entry.id) }
                            )
                        }
                    }

                    // Add new grade entry form
                    AnimatedVisibility(
                        visible = showAddGradeForm,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            if (course.gradeEntries.isNotEmpty()) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            }

                            Text(
                                text = "Nova Atividade",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = newEntryName,
                                onValueChange = { newEntryName = it },
                                label = { Text("Nome da atividade") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = newEntryGrade,
                                    onValueChange = {
                                        if (it.isEmpty() || it.toFloatOrNull() != null) {
                                            newEntryGrade = it
                                        }
                                    },
                                    label = { Text("Nota") },
                                    modifier = Modifier.weight(1f)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                OutlinedTextField(
                                    value = newEntryMultiplier,
                                    onValueChange = {
                                        if (it.isEmpty() || it.toFloatOrNull() != null) {
                                            newEntryMultiplier = it
                                        }
                                    },
                                    label = { Text("Peso") },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = { showAddGradeForm = false }
                                ) {
                                    Text("Cancelar")
                                }

                                Spacer(modifier = Modifier.weight(1f))

                                Button(
                                    onClick = {
                                        val grade = newEntryGrade.toFloatOrNull() ?: 0f
                                        val multiplier = newEntryMultiplier.toFloatOrNull() ?: 1f

                                        onAddGradeEntry(newEntryName, grade, multiplier)

                                        // Reset form fields
                                        newEntryName = ""
                                        newEntryGrade = ""
                                        newEntryMultiplier = "1.0"
                                        showAddGradeForm = false
                                    },
                                    enabled = newEntryName.isNotBlank() &&
                                            newEntryGrade.isNotBlank() &&
                                            newEntryMultiplier.isNotBlank()
                                ) {
                                    Text("Adicionar")
                                }
                            }
                        }
                    }

                    // Add grade entry button
                    if (!showAddGradeForm) {
                        Button(
                            onClick = { showAddGradeForm = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text("Adicionar Atividade")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GradeEntryRow(
    entry: GradeEntry,
    onUpdate: (GradeEntry) -> Unit,
    onRemove: () -> Unit
) {
    var name by remember(entry.name) { mutableStateOf(entry.name) }
    var grade by remember(entry.grade) { mutableStateOf(entry.grade.toString()) }
    var multiplier by remember(entry.multiplier) { mutableStateOf(entry.multiplier.toString()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                onUpdate(entry.copy(name = it))
            },
            modifier = Modifier.weight(2.5f),
            label = { Text("Atividade") }
        )

        Spacer(modifier = Modifier.width(4.dp))

        OutlinedTextField(
            value = grade,
            onValueChange = {
                if (it.isEmpty() || it.toFloatOrNull() != null) {
                    grade = it
                    onUpdate(entry.copy(grade = it.toFloatOrNull() ?: 0f))
                }
            },
            modifier = Modifier.weight(1f),
            label = { Text("Nota") }
        )

        Spacer(modifier = Modifier.width(4.dp))

        OutlinedTextField(
            value = multiplier,
            onValueChange = {
                if (it.isEmpty() || it.toFloatOrNull() != null) {
                    multiplier = it
                    onUpdate(entry.copy(multiplier = it.toFloatOrNull() ?: 1f))
                }
            },
            modifier = Modifier.weight(1f),
            label = { Text("Peso") }
        )

        Spacer(modifier = Modifier.width(4.dp))

        IconButton(
            onClick = onRemove,
            modifier = Modifier.weight(0.5f)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Remover atividade",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}