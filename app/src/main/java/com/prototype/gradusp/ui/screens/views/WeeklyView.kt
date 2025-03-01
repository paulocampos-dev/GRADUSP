package com.prototype.gradusp.ui.screens.views

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.prototype.gradusp.data.model.Event
import com.prototype.gradusp.ui.components.EventCard
import com.prototype.gradusp.ui.components.dialogs.EventDetailsDialog
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun WeeklyView(events: List<Event>, onUpdateEvent: (Event) -> Unit) {
    val daysOfWeek = DayOfWeek.values().toList()
    var selectedEvent by remember { mutableStateOf<Event?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        items(daysOfWeek) { day ->
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = day.getDisplayName(TextStyle.FULL, Locale("pt", "BR")),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp, start = 4.dp)
                )

                val dayEvents = events.filter { event ->
                    event.occurrences.any { it.day == day }
                }

                if (dayEvents.isNotEmpty()) {
                    dayEvents.forEach { event ->
                        val relevantOccurrences = event.occurrences.filter { it.day == day }
                        if (relevantOccurrences.isNotEmpty()) {
                            EventCard(
                                event = event.copy(occurrences = relevantOccurrences),
                                onClick = { selectedEvent = event }
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Sem eventos",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 12.dp)
                    )
                }

                if (day != daysOfWeek.last()) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
        }
    }

    selectedEvent?.let { event ->
        EventDetailsDialog(
            event = event,
            onDismiss = { selectedEvent = null },
            onSave = { updatedEvent ->
                onUpdateEvent(updatedEvent)
                selectedEvent = null
            }
        )
    }
}
