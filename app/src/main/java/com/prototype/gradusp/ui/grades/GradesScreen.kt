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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.navigation.NavController
import com.prototype.gradusp.ui.components.base.GraduspTitleBar
import com.prototype.gradusp.ui.components.CourseCard
import com.prototype.gradusp.viewmodel.GradesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradesScreen(
    navController: NavController,
    viewModel: GradesViewModel = hiltViewModel()
) {
    val courses by viewModel.courses.collectAsState()
    var showAddCourseDialog by remember { mutableStateOf(false) }
    var newCourseName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            GraduspTitleBar(screenTitle = "Notas")
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddCourseDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = "Adicionar") },
                text = { Text("Adicionar Disciplina") }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (courses.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.weight(1f))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
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
                                text = "Adicione suas matérias e atividades para calcular suas notas finais",
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = { showAddCourseDialog = true }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null
                                )
                                Text(
                                    text = "Adicionar Disciplina",
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))
                }
            } else {
                // List of courses
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Calculadora de Notas",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }

                    items(courses) { course ->
                        CourseCard(
                            course = course,
                            onUpdateName = { newName ->
                                viewModel.updateCourseName(course.id, newName)
                            },
                            onAddGradeEntry = { name, grade, multiplier ->
                                viewModel.addGradeEntry(course.id, name, grade, multiplier)
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

                    item {
                        Spacer(modifier = Modifier.height(80.dp)) // Space for FAB
                    }
                }
            }
        }

        // Add Course Dialog
        if (showAddCourseDialog) {
            AlertDialog(
                onDismissRequest = {
                    showAddCourseDialog = false
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
                                showAddCourseDialog = false
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
                            showAddCourseDialog = false
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