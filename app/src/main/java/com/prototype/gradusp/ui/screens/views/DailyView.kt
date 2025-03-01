package com.prototype.gradusp.ui.screens.views

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prototype.gradusp.data.model.Event
import com.prototype.gradusp.ui.components.EventCard
import com.prototype.gradusp.ui.components.dialogs.EventDetailsDialog
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun DailyView(events: List<Event>, onUpdateEvent: (Event) -> Unit) {
    var selectedEvent by remember { mutableStateOf<Event?>(null) }

    // Use the current date as initial state but allow navigation
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val currentDayOfWeek = selectedDate.dayOfWeek

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header with day navigation and today button
        DailyViewHeader(
            selectedDate = selectedDate,
            onPreviousDay = { selectedDate = selectedDate.minusDays(1) },
            onNextDay = { selectedDate = selectedDate.plusDays(1) },
            onTodayClick = { selectedDate = LocalDate.now() }
        )

        // Time slots
        val startHour = 7 // 7 AM
        val endHour = 22 // 10 PM

        LazyColumn {
            items(endHour - startHour + 1) { hourOffset ->
                val hour = startHour + hourOffset
                val timeSlotEvents = events.filter { event ->
                    event.occurrences.any {
                        it.day == currentDayOfWeek &&
                                (it.startTime.hour == hour ||
                                        (it.startTime.hour < hour && it.endTime.hour > hour))
                    }
                }

                TimeSlot(
                    hour = hour,
                    events = timeSlotEvents,
                    onClick = { selectedEvent = it }
                )
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

@Composable
fun DailyViewHeader(
    selectedDate: LocalDate,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onTodayClick: () -> Unit
) {
    val isToday = selectedDate.equals(LocalDate.now())

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPreviousDay) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowLeft,
                    contentDescription = "Dia anterior"
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = selectedDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("pt", "BR")),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onNextDay) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Próximo dia"
                )
            }
        }

        // Today button
        AnimatedVisibility(visible = !isToday) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                OutlinedButton(
                    onClick = onTodayClick,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Hoje",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Hoje")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun TimeSlot(hour: Int, events: List<Event>, onClick: (Event) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Hour indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hour text with AM/PM indicator
            Text(
                text = String.format("%02d:00", hour) + if (hour < 12) " AM" else " PM",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )

            HorizontalDivider(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            )
        }

        // Events at this hour
        if (events.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp) // Indentation to align with hour markers
            ) {
                events.forEach { event ->
                    val occurrence = event.occurrences.firstOrNull { it.startTime.hour <= hour && it.endTime.hour > hour }
                    occurrence?.let {
                        // Show event with time information
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            EventCard(
                                event = event.copy(
                                    occurrences = listOf(occurrence)
                                ),
                                onClick = { onClick(event) }
                            )
                        }
                    }
                }
            }
        } else {
            // Empty time slot indicator
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .padding(start = 24.dp)
            ) {
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                )
            }
        }
    }
}
