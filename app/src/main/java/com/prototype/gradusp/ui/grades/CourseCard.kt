package com.prototype.gradusp.ui.grades

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.prototype.gradusp.data.model.CourseGrade
import com.prototype.gradusp.data.model.GradeEntry
import com.prototype.gradusp.ui.theme.GRADUSPTheme
import java.text.DecimalFormat

@Composable
fun CourseCard(
    course: CourseGrade,
    onUpdateName: (String) -> Unit,
    onUpdateNotes: (String) -> Unit,
    onAddGradeEntry: (String, Double, Double) -> Unit,
    onUpdateGradeEntry: (GradeEntry) -> Unit,
    onRemoveGradeEntry: (String) -> Unit,
    onRemoveCourse: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .animateContentSize( // Smoothly animates size changes
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            CourseCardHeader(
                courseName = course.name,
                finalGrade = course.calculateFinalGrade(),
                isExpanded = isExpanded,
                onNameChange = onUpdateName,
                onExpandClick = { isExpanded = !isExpanded },
                onRemoveCourse = onRemoveCourse
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Progress bar and grade summary
            GradeProgress(
                currentGrade = course.calculateFinalGrade(),
                targetGrade = course.targetGrade,
                maxGrade = course.maxGrade
            )

            // Expandable section for grade details
            AnimatedVisibility(visible = isExpanded) {
                GradeDetails(
                    gradeEntries = course.gradeEntries,
                    onAddGradeEntry = onAddGradeEntry,
                    onUpdateGradeEntry = onUpdateGradeEntry,
                    onRemoveGradeEntry = onRemoveGradeEntry
                )
            }
        }
    }
}

@Composable
private fun CourseCardHeader(
    courseName: String,
    finalGrade: Double,
    isExpanded: Boolean,
    onNameChange: (String) -> Unit,
    onExpandClick: () -> Unit,
    onRemoveCourse: () -> Unit
) {
    val gradeFormat = remember { DecimalFormat("0.00") }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Editable course name
        OutlinedTextField(
            value = courseName,
            onValueChange = onNameChange,
            label = { Text("Nome da Disciplina") },
            modifier = Modifier.weight(1f),
            textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            singleLine = true
        )
        Spacer(modifier = Modifier.width(8.dp))
        // Grade display is now in the header
        Text(
            text = gradeFormat.format(finalGrade),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        IconButton(onClick = onExpandClick) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Recolher" else "Expandir"
            )
        }
    }
}

@Composable
private fun GradeProgress(currentGrade: Double, targetGrade: Double, maxGrade: Double) {
    val progress = (currentGrade / maxGrade).toFloat().coerceIn(0f, 1f)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(MaterialTheme.shapes.medium)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Nota Atual: ${"%.2f".format(currentGrade)} / Meta: ${"%.1f".format(targetGrade)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


@Composable
private fun GradeDetails(
    gradeEntries: List<GradeEntry>,
    onAddGradeEntry: (String, Double, Double) -> Unit,
    onUpdateGradeEntry: (GradeEntry) -> Unit,
    onRemoveGradeEntry: (String) -> Unit
) {
    var showAddForm by remember { mutableStateOf(false) }

    Column {
        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        // Header for the grade entries list
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
            Text("Atividade", modifier = Modifier.weight(2f), style = MaterialTheme.typography.labelLarge)
            Text("Nota", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
            Text("Peso", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.width(48.dp)) // For alignment with delete icon
        }
        Spacer(modifier = Modifier.height(8.dp))

        // List of existing grade entries
        gradeEntries.forEach { entry ->
            GradeEntryRow(
                entry = entry,
                onUpdate = onUpdateGradeEntry,
                onRemove = { onRemoveGradeEntry(entry.id) }
            )
        }

        // The form to add a new grade entry
        AnimatedVisibility(visible = showAddForm) {
            NewGradeEntryForm(
                onAdd = { name, grade, weight ->
                    onAddGradeEntry(name, grade, weight)
                    showAddForm = false
                },
                onCancel = { showAddForm = false }
            )
        }

        // Show "Add" button if form is hidden
        if (!showAddForm) {
            Button(
                onClick = { showAddForm = true },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar Atividade")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Adicionar Atividade")
            }
        }
    }
}

@Composable
private fun GradeEntryRow(
    entry: GradeEntry,
    onUpdate: (GradeEntry) -> Unit,
    onRemove: () -> Unit
) {
    var name by remember(entry.name) { mutableStateOf(entry.name) }
    var grade by remember(entry.grade) { mutableStateOf(if (entry.grade == 0.0) "" else entry.grade.toString()) }
    var weight by remember(entry.weight) { mutableStateOf(entry.weight.toString()) }

    fun update() {
        onUpdate(entry.copy(
            name = name,
            grade = grade.toDoubleOrNull() ?: 0.0,
            weight = weight.toDoubleOrNull() ?: 1.0
        ))
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(value = name, onValueChange = { name = it; update() }, modifier = Modifier.weight(2f))
        TextField(value = grade, onValueChange = { grade = it; update() }, modifier = Modifier.weight(1f).padding(start = 4.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center))
        TextField(value = weight, onValueChange = { weight = it; update() }, modifier = Modifier.weight(1f).padding(start = 4.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center))
        IconButton(onClick = onRemove) {
            Icon(Icons.Default.Delete, contentDescription = "Remover", tint = MaterialTheme.colorScheme.error)
        }
    }
}


@Composable
private fun NewGradeEntryForm(
    onAdd: (String, Double, Double) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var grade by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("1.0") }
    val isFormValid = name.isNotBlank() && grade.isNotBlank() && weight.isNotBlank()

    Column(modifier = Modifier.padding(top = 16.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Nome da Atividade (Ex: P1)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = grade,
                onValueChange = { grade = it },
                label = { Text("Nota") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = weight,
                onValueChange = { weight = it },
                label = { Text("Peso") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onCancel) {
                Text("Cancelar")
            }
            Button(
                onClick = {
                    onAdd(name, grade.toDoubleOrNull() ?: 0.0, weight.toDoubleOrNull() ?: 1.0)
                },
                enabled = isFormValid
            ) {
                Text("Salvar")
            }
        }
    }
}


@Preview(showBackground = true, name = "Course Card Preview")
@Composable
fun CourseCardPreview() {
    val sampleCourseWithGrades = CourseGrade(
        id = "1",
        name = "Engenharia de Software",
        gradeEntries = listOf(
            GradeEntry(id = "g1", name = "Prova 1", grade = 8.5, weight = 1.0),
            GradeEntry(id = "g2", name = "Trabalho Prático", grade = 10.0, weight = 1.5),
        ),
        targetGrade = 7.0
    )

    val sampleCourseEmpty = CourseGrade(
        id = "2",
        name = "Introdução à Computação",
        gradeEntries = emptyList(),
        targetGrade = 5.0
    )

    GRADUSPTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Preview for a card with existing grade entries
            CourseCard(
                course = sampleCourseWithGrades,
                onUpdateName = {},
                onUpdateNotes = {},
                onAddGradeEntry = { _, _, _ -> },
                onUpdateGradeEntry = {},
                onRemoveGradeEntry = {},
                onRemoveCourse = {}
            )
            // Preview for a card with no grade entries
            CourseCard(
                course = sampleCourseEmpty,
                onUpdateName = {},
                onUpdateNotes = {},
                onAddGradeEntry = { _, _, _ -> },
                onUpdateGradeEntry = {},
                onRemoveGradeEntry = {},
                onRemoveCourse = {}
            )
        }
    }
}