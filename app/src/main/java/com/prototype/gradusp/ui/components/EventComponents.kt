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
import java.time.DayOfWeek
import java.time.LocalTime

@Composable
fun EventCard(title: String, startTime: java.time.LocalTime, endTime: java.time.LocalTime, color: Color) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(text = "From $startTime to $endTime")
            }
        }
    }
}

@Composable
fun EventList(events: List<Event>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        events.forEach { event ->
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = event.color.copy(alpha = 0.2f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.titleMedium
                    )

                    event.occurrences.forEach { occurrence ->
                        Text(
                            text = "${occurrence.day}: ${occurrence.startTime} - ${occurrence.endTime}"
                        )
                    }

                    if (event.recurring) {
                        Text(
                            text = "Recurring Event",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
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
        title = "Math Class",
        startTime = java.time.LocalTime.of(10, 0),
        endTime = java.time.LocalTime.of(12, 0),
        color = Color.Blue
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
                    EventOccurrence(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(10, 0))
                ),
                color = Color.Blue,
                recurring = true
            )
        )
    )
}
