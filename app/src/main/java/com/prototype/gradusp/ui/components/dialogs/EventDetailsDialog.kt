package com.prototype.gradusp.ui.components.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.prototype.gradusp.data.model.Event
import com.prototype.gradusp.data.model.EventOccurrence
import com.prototype.gradusp.ui.components.ColorPickerDialog
import java.time.DayOfWeek
import java.time.LocalTime

@Composable
fun EventDetailsDialog(
    event: Event,
    onDismiss: () -> Unit,
    onSave: (Event) -> Unit
) {
    var title by remember { mutableStateOf(event.title) }
    var teacher by remember { mutableStateOf("") }
    var officeHours by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var showColorPicker by remember { mutableStateOf(false) }
    var selectedColor by remember { mutableStateOf(event.color) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Evento") },
        text = {
            Column {
                Text("Nome:", fontWeight = FontWeight.Bold)
                BasicTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { }),
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                )

                Text("Professor:", fontWeight = FontWeight.Bold)
                BasicTextField(
                    value = teacher,
                    onValueChange = { teacher = it },
                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                )

                Text("Sala e Horário de atendimento", fontWeight = FontWeight.Bold)
                BasicTextField(
                    value = officeHours,
                    onValueChange = { officeHours = it },
                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                )

                Text("Anotações", fontWeight = FontWeight.Bold)
                BasicTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                )

                Button(onClick = { showColorPicker = true }) {
                    Text("Mudar a Cor")
                }

                if (showColorPicker) {
                    ColorPickerDialog(
                        currentColor = selectedColor,
                        onColorSelected = { selectedColor = it },
                        onDismiss = { showColorPicker = false }
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val updatedEvent = event.copy(
                    title = title,
                    color = selectedColor,
                    occurrences = event.occurrences,  // Preserve all occurrences
                    recurring = event.recurring       // Preserve recurring status
                )
                onSave(updatedEvent)
                onDismiss()
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewEventDetailsDialog() {
    EventDetailsDialog(
        event = Event(
            title = "Math Class",
            occurrences = listOf(
                EventOccurrence(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(10, 0))
            ),
            color = Color.Blue,
            recurring = true
        ),
        onDismiss = {},
        onSave = {}
    )
}
