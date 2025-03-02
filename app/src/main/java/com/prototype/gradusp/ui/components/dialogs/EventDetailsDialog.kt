package com.prototype.gradusp.ui.components.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.prototype.gradusp.data.model.Event
import com.prototype.gradusp.data.model.eventColors
import com.prototype.gradusp.ui.components.base.DialogActions
import com.prototype.gradusp.ui.components.base.GraduspDialog
import com.prototype.gradusp.ui.components.base.LabeledTextField
import com.prototype.gradusp.ui.components.colorpicker.ColorPickerDialog
import com.prototype.gradusp.ui.theme.GraduspShapes
import com.prototype.gradusp.ui.theme.GraduspSpacing
import com.prototype.gradusp.utils.DateTimeUtils

@Composable
fun EventDetailsDialog(
    event: Event,
    onDismiss: () -> Unit,
    onSave: (Event) -> Unit,
    onDelete: (Event) -> Unit
) {
    var title by remember { mutableStateOf(event.title) }
    var teacher by remember { mutableStateOf(event.teacher ?: "") }
    var location by remember { mutableStateOf(event.location ?: "") }
    var notes by remember { mutableStateOf(event.notes ?: "") }
    var selectedColor by remember { mutableStateOf(event.color) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    GraduspDialog(
        title = "Editar Evento",
        onDismiss = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        actions = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Delete button
                IconButton(
                    onClick = { showDeleteConfirmation = true },
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Excluir Evento",
                        tint = MaterialTheme.colorScheme.error
                    )
                }

                // Save/Cancel buttons
                Row {
                    DialogActions(
                        onCancel = onDismiss,
                        onConfirm = {
                            val updatedEvent = event.copy(
                                title = title,
                                color = selectedColor,
                                teacher = teacher.takeIf { it.isNotBlank() },
                                location = location.takeIf { it.isNotBlank() },
                                notes = notes.takeIf { it.isNotBlank() }
                            )
                            onSave(updatedEvent)
                        },
                        confirmText = "Salvar"
                    )
                }
            }
        }
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState())
        ) {
            // Header with color indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(selectedColor, CircleShape)
                        .border(1.dp, Color.Black.copy(alpha = 0.2f), CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(GraduspSpacing.md))

            // Title field
            LabeledTextField(
                value = title,
                onValueChange = { title = it },
                label = "Nome do evento",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(GraduspSpacing.md))

            // Occurrences section
            OccurrencesSection(event = event)

            Spacer(modifier = Modifier.height(GraduspSpacing.md))

            // Teacher field
            LabeledTextField(
                value = teacher,
                onValueChange = { teacher = it },
                label = "Professor",
                icon = Icons.Default.Person
            )

            Spacer(modifier = Modifier.height(GraduspSpacing.sm))

            // Location field
            LabeledTextField(
                value = location,
                onValueChange = { location = it },
                label = "Local/Sala",
                icon = Icons.Default.LocationOn
            )

            Spacer(modifier = Modifier.height(GraduspSpacing.sm))

            // Notes field
            LabeledTextField(
                value = notes,
                onValueChange = { notes = it },
                label = "Anotações",
                icon = Icons.Default.Edit,
                singleLine = false,
                minLines = 3
            )

            Spacer(modifier = Modifier.height(GraduspSpacing.md))

            // Color picker section
            ColorPickerSection(
                selectedColor = selectedColor,
                onColorSelected = { selectedColor = it },
                onShowColorPicker = { showColorPicker = true }
            )

            if (showColorPicker) {
                ColorPickerDialog(
                    currentColor = selectedColor,
                    onColorSelected = {
                        selectedColor = it
                        showColorPicker = false
                    },
                    onDismiss = { showColorPicker = false }
                )
            }

            Spacer(modifier = Modifier.height(GraduspSpacing.sm))
        }
    }

    // Delete confirmation dialog
    if (showDeleteConfirmation) {
        DeleteConfirmationDialog(
            eventTitle = event.title,
            onDismiss = { showDeleteConfirmation = false },
            onConfirmDelete = {
                onDelete(event)
                showDeleteConfirmation = false
            }
        )
    }
}

@Composable
fun OccurrencesSection(event: Event) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = GraduspShapes.medium
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Horários"
                )
                Spacer(modifier = Modifier.width(GraduspSpacing.sm))
                Text(
                    text = "Horários",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(GraduspSpacing.sm))

            event.occurrences.forEach { occurrence ->
                val dayName = DateTimeUtils.getDayName(occurrence.day)
                val timeRange = DateTimeUtils.formatTimeRange(occurrence.startTime, occurrence.endTime)

                Text(
                    text = "$dayName: $timeRange",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 28.dp, bottom = 4.dp)
                )
            }
        }
    }
}

@Composable
fun ColorPickerSection(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit,
    onShowColorPicker: () -> Unit
) {
    Column {
        Text(
            text = "Cor do evento",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(GraduspSpacing.sm))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            eventColors.forEach { color ->
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (selectedColor == color) 3.dp else 1.dp,
                            color = if (selectedColor == color)
                                MaterialTheme.colorScheme.primary
                            else
                                Color.Black.copy(alpha = 0.2f),
                            shape = CircleShape
                        )
                        .padding(4.dp)
                        .clip(CircleShape)
                        .clickable { onColorSelected(color) }
                )
            }

            // Custom color button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, Color.Black.copy(alpha = 0.2f), CircleShape)
                    .clickable { onShowColorPicker() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun DeleteConfirmationDialog(
    eventTitle: String,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    GraduspDialog(
        title = "Excluir evento",
        onDismiss = onDismiss,
        actions = {
            DialogActions(
                onCancel = onDismiss,
                onConfirm = onConfirmDelete,
                cancelText = "Cancelar",
                confirmText = "Excluir"
            )
        }
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(
                text = "Você tem certeza que deseja excluir o evento \"$eventTitle\"?",
                style = MaterialTheme.typography.bodyLarge,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Esta ação não pode ser desfeita.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

private val Color.Companion.clickable: Any
    get() = Any()