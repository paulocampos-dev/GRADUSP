package com.prototype.gradusp.ui.grades

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.prototype.gradusp.viewmodel.GradesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradesScreen(
    viewModel: GradesViewModel = hiltViewModel()
) {
    val courses by viewModel.courses.collectAsState()
    val showAddCourseDialog by viewModel.isAddCourseDialogVisible.collectAsState()
    var newCourseName by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        if (courses.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Calculadora de Notas",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Acompanhe suas notas e calcule a média final das suas disciplinas. Adicione seu primeiro curso para começar!",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item { Spacer(modifier = Modifier.height(16.dp)) }

                items(courses) { course ->
                    CourseCard(
                        course = course,
                        onUpdateName = { newName ->
                            viewModel.updateCourseName(course.id, newName)
                        },
                        onUpdateNotes = { notes ->
                            viewModel.updateCourseNotes(course.id, notes)
                        },
                        onAddGradeEntry = { name, grade, weight ->
                            viewModel.addGradeEntry(course.id, name, grade, weight)
                        },
                        onUpdateGradeEntry = { entry ->
                            viewModel.updateGradeEntry(course.id, entry)
                        },
                        onRemoveGradeEntry = { entryId ->
                            viewModel.removeGradeEntry(course.id, entryId)
                        },
                        onRemoveCourse = {
                            viewModel.removeCourse(course.id)
                        }
                    )
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }

        if (showAddCourseDialog) {
            AlertDialog(
                onDismissRequest = {
                    viewModel.onAddCourseDialogDismissed()
                    newCourseName = ""
                },
                title = { Text("Adicionar Disciplina") },
                text = {
                    OutlinedTextField(
                        value = newCourseName,
                        onValueChange = { newCourseName = it },
                        label = { Text("Nome da Disciplina") },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newCourseName.isNotBlank()) {
                                viewModel.addCourse(newCourseName)
                                newCourseName = ""
                                viewModel.onAddCourseDialogDismissed()
                            }
                        },
                        enabled = newCourseName.isNotBlank()
                    ) {
                        Text("Adicionar")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            viewModel.onAddCourseDialogDismissed()
                            newCourseName = ""
                        }
                    ) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}