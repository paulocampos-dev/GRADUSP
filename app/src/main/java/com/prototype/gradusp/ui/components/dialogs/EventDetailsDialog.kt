package com.prototype.gradusp.ui.components.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.prototype.gradusp.data.model.Event
import com.prototype.gradusp.data.model.eventColors
import com.prototype.gradusp.ui.components.ScheduleSelector
import com.prototype.gradusp.ui.components.base.DialogActions
import com.prototype.gradusp.ui.components.base.GraduspDialog
import com.prototype.gradusp.ui.components.base.LabeledTextField
import com.prototype.gradusp.ui.components.colorpicker.ColorPickerDialog
import com.prototype.gradusp.ui.theme.GraduspShapes
import com.prototype.gradusp.ui.theme.GraduspSpacing
import com.prototype.gradusp.utils.DateTimeUtils

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
    var customSelectedColor by remember {
        mutableStateOf(
            if (event.color !in eventColors) event.color else Color.LightGray
        )
    }
    var occurrences by remember { mutableStateOf(event.occurrences) }

    var showColorPicker by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Dialog title
                Text(
                    text = "Editar",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

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
                    label = "Nome",
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(GraduspSpacing.md))

                // Schedule selector (replaces OccurrencesSection)
                ScheduleSelector(
                    occurrences = occurrences,
                    onOccurrencesChanged = { occurrences = it }
                )

                Spacer(modifier = Modifier.height(GraduspSpacing.md))

                // Teacher field
                LabeledTextField(
                    value = teacher,
                    onValueChange = { teacher = it },
                    label = "Professor(a)",
                    icon = Icons.Default.Person
                )

                Spacer(modifier = Modifier.height(GraduspSpacing.sm))

                // Location field
                LabeledTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = "Localização/Sala",
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
                Column {
                    Text(
                        text = "Cor",
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
                                        width = if (selectedColor == color) 4.dp else 1.dp,
                                        color = if (selectedColor == color)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            Color.Black.copy(alpha = 0.2f),
                                        shape = CircleShape
                                    )
                                    .padding(4.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        selectedColor = color
                                        customSelectedColor = Color.LightGray
                                    }
                            )
                        }

                        // Custom color display
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(customSelectedColor)
                                .border(
                                    width = if (selectedColor == customSelectedColor) 4.dp else 1.dp,
                                    color = if (selectedColor == customSelectedColor)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        Color.Black.copy(alpha = 0.2f),
                                    shape = CircleShape
                                )
                                .padding(4.dp)
                                .clip(CircleShape)
                                .clickable { selectedColor = customSelectedColor }
                        )

                        // Custom color button
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, Color.Black.copy(alpha = 0.2f), CircleShape)
                                .clickable { showColorPicker = true },
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

                Spacer(modifier = Modifier.height(24.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Delete button
                    TextButton(
                        onClick = { showDeleteConfirmation = true },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete")
                    }

                    // Save/Cancel buttons
                    Row {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel")
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                val updatedEvent = event.copy(
                                    title = title,
                                    color = selectedColor,
                                    teacher = teacher.takeIf { it.isNotBlank() },
                                    location = location.takeIf { it.isNotBlank() },
                                    notes = notes.takeIf { it.isNotBlank() },
                                    occurrences = occurrences
                                )
                                onSave(updatedEvent)
                            }
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }

        // Show color picker dialog when needed
        if (showColorPicker) {
            ColorPickerDialog(
                currentColor = selectedColor,
                onColorSelected = {
                    selectedColor = it
                    customSelectedColor = it
                    showColorPicker = false
                },
                onDismiss = { showColorPicker = false }
            )
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
