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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.prototype.gradusp.data.model.Event
import com.prototype.gradusp.data.model.EventImportance
import com.prototype.gradusp.data.model.eventColors
import com.prototype.gradusp.ui.components.ColorPickerDialog
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun EventDetailsDialog(
    event: Event,
    onDismiss: () -> Unit,
    onSave: (Event) -> Unit
) {
    var title by remember { mutableStateOf(event.title) }
    var teacher by remember { mutableStateOf(event.teacher ?: "") }
    var location by remember { mutableStateOf(event.location ?: "") }
    var notes by remember { mutableStateOf(event.notes ?: "") }
    var selectedColor by remember { mutableStateOf(event.color) }
    var showColorPicker by remember { mutableStateOf(false) }
    var importance by remember { mutableStateOf(event.importance) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                HeaderSection(selectedColor = selectedColor)

                Spacer(modifier = Modifier.height(16.dp))

                TitleSection(title = title, onTitleChange = { title = it })

                Spacer(modifier = Modifier.height(16.dp))

                OccurrencesSection(event = event)

//                Spacer(modifier = Modifier.height(16.dp))
//                ImportanceSelector(
//                    importance = importance,
//                    onImportanceSelected = { importance = it }
//                )

                Spacer(modifier = Modifier.height(16.dp))

                TeacherField(teacher = teacher, onTeacherChange = { teacher = it })

                Spacer(modifier = Modifier.height(12.dp))

                LocationField(location = location, onLocationChange = { location = it })

                Spacer(modifier = Modifier.height(12.dp))

                NotesField(notes = notes, onNotesChange = { notes = it })

                Spacer(modifier = Modifier.height(16.dp))

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

                Spacer(modifier = Modifier.height(24.dp))

                HorizontalDivider()

                Spacer(modifier = Modifier.height(16.dp))

                ButtonsSection(
                    onCancel = onDismiss,
                    onSave = {
                        val updatedEvent = event.copy(
                            title = title,
                            color = selectedColor,
                            teacher = teacher.takeIf { it.isNotBlank() },
                            location = location.takeIf { it.isNotBlank() },
                            notes = notes.takeIf { it.isNotBlank() },
                            importance = importance
                        )
                        onSave(updatedEvent)
                        onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
fun HeaderSection(selectedColor: Color) {
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
        Spacer(modifier = Modifier.padding(8.dp))
        Text(
            text = "Editar Evento",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun TitleSection(title: String, onTitleChange: (String) -> Unit) {
    OutlinedTextField(
        value = title,
        onValueChange = onTitleChange,
        label = { Text("Nome do evento") },
        modifier = Modifier.fillMaxWidth(),
        textStyle = MaterialTheme.typography.bodyLarge
    )
}

@Composable
fun OccurrencesSection(event: Event) {
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Horários"
                )
                Spacer(modifier = Modifier.padding(4.dp))
                Text(
                    text = "Horários",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            event.occurrences.forEach { occurrence ->
                val dayName = occurrence.day.getDisplayName(TextStyle.FULL, Locale("pt", "BR"))
                Text(
                    text = "$dayName: ${occurrence.startTime.format(timeFormatter)} - ${occurrence.endTime.format(timeFormatter)}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 28.dp, bottom = 4.dp)
                )
            }
        }
    }
}

@Composable
fun ImportanceSelector(
    importance: EventImportance,
    onImportanceSelected: (EventImportance) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = when (importance) {
                EventImportance.LOW -> Icons.Outlined.Star
                EventImportance.NORMAL -> Icons.Rounded.Star
                EventImportance.HIGH -> Icons.Default.Star
            },
            contentDescription = "Importância",
            tint = when (importance) {
                EventImportance.LOW -> MaterialTheme.colorScheme.onSurfaceVariant
                EventImportance.NORMAL -> Color(0xFFFFA000)
                EventImportance.HIGH -> Color(0xFFFFD700)
            }
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "Importância:",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(end = 8.dp)
        )
        Box(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(4.dp)
                    )
                    .clickable { expanded = true }
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (importance) {
                        EventImportance.LOW -> "Baixa"
                        EventImportance.NORMAL -> "Normal"
                        EventImportance.HIGH -> "Alta"
                    }
                )
                Icon(
                    imageVector = if (expanded)
                        Icons.Default.KeyboardArrowUp
                    else
                        Icons.Default.KeyboardArrowDown,
                    contentDescription = "Expandir menu"
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                DropdownMenuItem(
                    text = { Text("Baixa") },
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.Star,
                            contentDescription = "Baixa importância",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    onClick = {
                        onImportanceSelected(EventImportance.LOW)
                        expanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Normal") },
                    leadingIcon = {
                        Icon(
                            Icons.Rounded.Star,
                            contentDescription = "Importância normal",
                            tint = Color(0xFFFFA000)
                        )
                    },
                    onClick = {
                        onImportanceSelected(EventImportance.NORMAL)
                        expanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Alta") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "Alta importância",
                            tint = Color(0xFFFFD700)
                        )
                    },
                    onClick = {
                        onImportanceSelected(EventImportance.HIGH)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun TeacherField(teacher: String, onTeacherChange: (String) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = "Professor"
        )
        Spacer(modifier = Modifier.padding(4.dp))
        OutlinedTextField(
            value = teacher,
            onValueChange = onTeacherChange,
            label = { Text("Professor") },
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun LocationField(location: String, onLocationChange: (String) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = "Local"
        )
        Spacer(modifier = Modifier.padding(4.dp))
        OutlinedTextField(
            value = location,
            onValueChange = onLocationChange,
            label = { Text("Local/Sala") },
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun NotesField(notes: String, onNotesChange: (String) -> Unit) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = "Anotações",
            modifier = Modifier.padding(top = 8.dp)
        )
        Spacer(modifier = Modifier.padding(4.dp))
        OutlinedTextField(
            value = notes,
            onValueChange = onNotesChange,
            label = { Text("Anotações") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            textStyle = MaterialTheme.typography.bodyLarge
        )
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
        Spacer(modifier = Modifier.height(8.dp))
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
                        .clickable { onColorSelected(color) }
                )
            }
            // More colors button
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
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun ButtonsSection(
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        TextButton(onClick = onCancel) {
            Text("Cancelar")
        }
        Spacer(modifier = Modifier.padding(8.dp))
        Button(onClick = onSave) {
            Text("Salvar")
        }
    }
}
