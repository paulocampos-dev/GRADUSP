package com.prototype.gradusp.ui.components

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prototype.gradusp.data.model.EventOccurrence
import com.prototype.gradusp.ui.components.dialogs.TimePickerDialog
import com.prototype.gradusp.utils.DateTimeUtils
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun ScheduleSelector(
    occurrences: List<EventOccurrence>,
    onOccurrencesChanged: (List<EventOccurrence>) -> Unit
) {
    var currentOccurrences by remember { mutableStateOf(occurrences) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Schedule",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // List existing occurrences with edit/delete options
        currentOccurrences.forEachIndexed { index, occurrence ->
            CompactOccurrenceItem(
                occurrence = occurrence,
                onEdit = { updatedOccurrence ->
                    val newList = currentOccurrences.toMutableList()
                    newList[index] = updatedOccurrence
                    currentOccurrences = newList
                    onOccurrencesChanged(newList)
                },
                onDelete = {
                    val newList = currentOccurrences.toMutableList()
                    newList.removeAt(index)
                    currentOccurrences = newList
                    onOccurrencesChanged(newList)
                }
            )

            Spacer(modifier = Modifier.height(8.dp))
        }

        // Add new occurrence button
        Button(
            onClick = {
                val newOccurrence = EventOccurrence(
                    day = DayOfWeek.MONDAY,
                    startTime = LocalTime.of(8, 0),
                    endTime = LocalTime.of(10, 0)
                )
                val newList = currentOccurrences + newOccurrence
                currentOccurrences = newList
                onOccurrencesChanged(newList)
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Add Time Slot")
        }
    }
}

@Composable
fun CompactOccurrenceItem(
    occurrence: EventOccurrence,
    onEdit: (EventOccurrence) -> Unit,
    onDelete: () -> Unit
) {
    var showDayPicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    var selectedDay by remember { mutableStateOf(occurrence.day) }
    var selectedStartTime by remember { mutableStateOf(occurrence.startTime) }
    var selectedEndTime by remember { mutableStateOf(occurrence.endTime) }

    // Update parent when any value changes
    LaunchedEffect(selectedDay, selectedStartTime, selectedEndTime) {
        if (selectedDay != occurrence.day ||
            selectedStartTime != occurrence.startTime ||
            selectedEndTime != occurrence.endTime) {
            onEdit(EventOccurrence(selectedDay, selectedStartTime, selectedEndTime))
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Day selector on top
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Day:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(end = 8.dp)
                )

                Button(
                    onClick = { showDayPicker = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    // Using short day name for better readability
                    Text(DateTimeUtils.getShortDayName(selectedDay))
                }

                // Delete button
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove time slot",
                        tint = MaterialTheme.colorScheme.error
                    )
                }

                // Day picker dropdown
                DropdownMenu(
                    expanded = showDayPicker,
                    onDismissRequest = { showDayPicker = false }
                ) {
                    DayOfWeek.values().forEach { day ->
                        DropdownMenuItem(
                            text = { Text(DateTimeUtils.getDayName(day)) },
                            onClick = {
                                selectedDay = day
                                showDayPicker = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Time selectors in a row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Time:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(end = 8.dp)
                )

                // Start time selector
                Button(
                    onClick = { showStartTimePicker = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(selectedStartTime.format(DateTimeFormatter.ofPattern("HH:mm")))
                }

                Text(
                    text = " - ",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                // End time selector
                Button(
                    onClick = { showEndTimePicker = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(selectedEndTime.format(DateTimeFormatter.ofPattern("HH:mm")))
                }

                // Time picker dropdown for start time
                DropdownMenu(
                    expanded = showStartTimePicker,
                    onDismissRequest = { showStartTimePicker = false }
                ) {
                    for (hour in 7..21) {
                        DropdownMenuItem(
                            text = { Text("$hour:00") },
                            onClick = {
                                selectedStartTime = LocalTime.of(hour, 0)
                                showStartTimePicker = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("$hour:30") },
                            onClick = {
                                selectedStartTime = LocalTime.of(hour, 30)
                                showStartTimePicker = false
                            }
                        )
                    }
                }

                // Time picker dropdown for end time
                DropdownMenu(
                    expanded = showEndTimePicker,
                    onDismissRequest = { showEndTimePicker = false }
                ) {
                    for (hour in 8..22) {
                        DropdownMenuItem(
                            text = { Text("$hour:00") },
                            onClick = {
                                selectedEndTime = LocalTime.of(hour, 0)
                                showEndTimePicker = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("$hour:30") },
                            onClick = {
                                selectedEndTime = LocalTime.of(hour, 30)
                                showEndTimePicker = false
                            }
                        )
                    }
                }
            }
        }
    }
}
