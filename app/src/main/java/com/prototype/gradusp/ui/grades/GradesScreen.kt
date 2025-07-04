package com.prototype.gradusp.ui.grades

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.prototype.gradusp.data.model.CourseGrade
import com.prototype.gradusp.data.model.GradeEntry
import com.prototype.gradusp.ui.theme.GRADUSPTheme
import com.prototype.gradusp.viewmodel.GradesViewModel

@Composable
fun GradesScreen(
    viewModel: GradesViewModel = hiltViewModel()
) {
    val courses by viewModel.courses.collectAsState()
    val showAddCourseDialog by viewModel.isAddCourseDialogVisible.collectAsState()
    var newCourseName by remember { mutableStateOf("") }

    GradesScreenContent(
        courses = courses,
        showAddCourseDialog = showAddCourseDialog,
        newCourseName = newCourseName,
        onNewCourseNameChange = { newCourseName = it },
        onAddCourseDialogDismiss = {
            viewModel.onAddCourseDialogDismissed()
            newCourseName = ""
        },
        onAddCourse = {
            if (newCourseName.isNotBlank()) {
                viewModel.addCourse(newCourseName)
                newCourseName = ""
                viewModel.onAddCourseDialogDismissed()
            }
        },
        onUpdateCourseName = viewModel::updateCourseName,
        onUpdateCourseNotes = viewModel::updateCourseNotes,
        onAddGradeEntry = { courseId, name, grade, weight ->
            viewModel.addGradeEntry(courseId, name, grade, weight)
        },
        onUpdateGradeEntry = viewModel::updateGradeEntry,
        onRemoveGradeEntry = viewModel::removeGradeEntry,
        onRemoveCourse = viewModel::removeCourse
    )
}

@Composable
private fun GradesScreenContent(
    courses: List<CourseGrade>,
    showAddCourseDialog: Boolean,
    newCourseName: String,
    onNewCourseNameChange: (String) -> Unit,
    onAddCourseDialogDismiss: () -> Unit,
    onAddCourse: () -> Unit,
    onUpdateCourseName: (String, String) -> Unit,
    onUpdateCourseNotes: (String, String) -> Unit,
    onAddGradeEntry: (String, String, Double, Double) -> Unit,
    onUpdateGradeEntry: (String, GradeEntry) -> Unit,
    onRemoveGradeEntry: (String, String) -> Unit,
    onRemoveCourse: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        if (courses.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
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
                            onUpdateCourseName(course.id, newName)
                        },
                        onUpdateNotes = { notes ->
                            onUpdateCourseNotes(course.id, notes)
                        },
                        onAddGradeEntry = { name, grade, weight ->
                            onAddGradeEntry(course.id, name, grade, weight)
                        },
                        onUpdateGradeEntry = { entry ->
                            onUpdateGradeEntry(course.id, entry)
                        },
                        onRemoveGradeEntry = { entryId ->
                            onRemoveGradeEntry(course.id, entryId)
                        },
                        onRemoveCourse = {
                            onRemoveCourse(course.id)
                        }
                    )
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }

        if (showAddCourseDialog) {
            AlertDialog(
                onDismissRequest = onAddCourseDialogDismiss,
                title = { Text("Adicionar Disciplina") },
                text = {
                    OutlinedTextField(
                        value = newCourseName,
                        onValueChange = onNewCourseNameChange,
                        label = { Text("Nome da Disciplina") },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = onAddCourse,
                        enabled = newCourseName.isNotBlank()
                    ) {
                        Text("Adicionar")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = onAddCourseDialogDismiss
                    ) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}

@Preview(showBackground = true, name = "Grades Screen (Empty)")
@Composable
fun GradesScreenPreview_EmptyState() {
    GRADUSPTheme {
        GradesScreenContent(
            courses = emptyList(),
            showAddCourseDialog = false,
            newCourseName = "",
            onNewCourseNameChange = {},
            onAddCourseDialogDismiss = {},
            onAddCourse = {},
            onUpdateCourseName = { _, _ -> },
            onUpdateCourseNotes = { _, _ -> },
            onAddGradeEntry = { _, _, _, _ -> },
            onUpdateGradeEntry = { _, _ -> },
            onRemoveGradeEntry = { _, _ -> },
            onRemoveCourse = {}
        )
    }
}

@Preview(showBackground = true, name = "Grades Screen (With Courses)")
@Composable
fun GradesScreenPreview_WithCourses() {
    val sampleCourses = listOf(
        CourseGrade(
            name = "Cálculo I",
            gradeEntries = listOf(
                GradeEntry(name = "P1", grade = 8.5, weight = 1.0),
                GradeEntry(name = "P2", grade = 7.0, weight = 1.0),
            )
        ),
        CourseGrade(
            name = "Física para Computação",
            gradeEntries = listOf(
                GradeEntry(name = "Prova 1", grade = 9.0, weight = 2.0),
                GradeEntry(name = "Trabalho", grade = 10.0, weight = 1.0),
            )
        )
    )

    GRADUSPTheme {
        GradesScreenContent(
            courses = sampleCourses,
            showAddCourseDialog = false,
            newCourseName = "",
            onNewCourseNameChange = {},
            onAddCourseDialogDismiss = {},
            onAddCourse = {},
            onUpdateCourseName = { _, _ -> },
            onUpdateCourseNotes = { _, _ -> },
            onAddGradeEntry = { _, _, _, _ -> },
            onUpdateGradeEntry = { _, _ -> },
            onRemoveGradeEntry = { _, _ -> },
            onRemoveCourse = {}
        )
    }
}