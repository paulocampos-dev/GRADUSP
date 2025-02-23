package com.prototype.gradusp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.prototype.gradusp.data.model.Event
import com.prototype.gradusp.data.model.EventOccurrence
import com.prototype.gradusp.ui.components.dialogs.EventDetailsDialog
import java.time.DayOfWeek
import java.time.LocalTime

@Composable
fun EventCard(event: Event, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = event.color.copy(alpha = 0.2f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(text = event.title, style = MaterialTheme.typography.titleMedium)
                event.occurrences.forEach { occurrence ->
                    Text(text = "${occurrence.day}: ${occurrence.startTime} - ${occurrence.endTime}")
                }
            }
        }
    }
}

@Composable
fun EventList(events: List<Event>, onUpdateEvent: (Event) -> Unit) {
    var selectedEvent by remember { mutableStateOf<Event?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        events.forEach { event ->
            EventCard(
                event = event,
                onClick = { selectedEvent = event }
            )
        }
    }

    selectedEvent?.let { event ->
        EventDetailsDialog(
            event = event,
            onDismiss = { selectedEvent = null },
            onSave = { updatedEvent ->
                // Make sure we update the entire event in the parent
                onUpdateEvent(updatedEvent)
                selectedEvent = null
            }
        )
    }
}


@Composable
fun EditableEvent(event: Event, onColorChange: (Color) -> Unit) {
    var showColorPicker by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { showColorPicker = true },
        colors = CardDefaults.cardColors(containerColor = event.color.copy(alpha = 0.2f))
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            Column {
                Text(text = event.title)
                Text(text = "Recurring: ${event.recurring}")
            }
        }
    }

    if (showColorPicker) {
        ColorPickerDialog(
            currentColor = event.color,
            onColorSelected = { onColorChange(it) },
            onDismiss = { showColorPicker = false }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewEventCard() {
    EventCard(
        event = Event(
            title = "Math Class",
            occurrences = listOf(
                EventOccurrence(DayOfWeek.MONDAY, java.time.LocalTime.of(8, 0), java.time.LocalTime.of(10, 0))
            ),
            color = Color.Blue,
            recurring = true
        ),
        onClick = {}
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewEventList() {
    EventList(
        events = listOf(
            Event(
                title = "Math Class",
                occurrences = listOf(
                    EventOccurrence(DayOfWeek.MONDAY, java.time.LocalTime.of(8, 0), java.time.LocalTime.of(10, 0))
                ),
                color = Color.LightGray,
                recurring = true
            ),
            Event(
                title = "Physics Class",
                occurrences = listOf(
                    EventOccurrence(DayOfWeek.WEDNESDAY, java.time.LocalTime.of(14, 0), java.time.LocalTime.of(16, 0))
                ),
                color = Color.Red,
                recurring = true
            )
        ),
        onUpdateEvent = {}
    )
}

