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
import androidx.compose.foundation.lazy.items
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
import com.prototype.gradusp.data.model.EventOccurrence
import com.prototype.gradusp.ui.components.EventCard
import com.prototype.gradusp.ui.components.dialogs.EventDetailsDialog
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

// Data class to represent a time block with events for the day view
data class TimeBlock(
    val startHour: Int,
    val endHour: Int,
    val events: List<Pair<Event, EventOccurrence>>
)

@Composable
fun DailyView(events: List<Event>, onUpdateEvent: (Event) -> Unit) {
    var selectedEvent by remember { mutableStateOf<Event?>(null) }

    // Use the current date as initial state but allow navigation
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val currentDayOfWeek = selectedDate.dayOfWeek

    // Time ranges for display
    val startHour = 7 // 7 AM
    val endHour = 22 // 10 PM

    // Filter events for the current day and organize them by time blocks
    val dayEvents = remember(events, currentDayOfWeek) {
        // Get all events for this day with their occurrences
        val filteredEvents = events.flatMap { event ->
            event.occurrences
                .filter { it.day == currentDayOfWeek }
                .map { occurrence -> Pair(event, occurrence) }
        }

        // Group events into coherent time blocks
        if (filteredEvents.isEmpty()) {
            emptyList()
        } else {
            processEventsIntoTimeBlocks(filteredEvents, startHour, endHour)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        // Header with day navigation and today button
        DailyViewHeader(
            selectedDate = selectedDate,
            onPreviousDay = { selectedDate = selectedDate.minusDays(1) },
            onNextDay = { selectedDate = selectedDate.plusDays(1) },
            onTodayClick = { selectedDate = LocalDate.now() }
        )

        // Check if there are any events for this day
        val hasEvents = dayEvents.any { it.events.isNotEmpty() }

        if (hasEvents) {
            // Display time blocks
            LazyColumn {
                items(dayEvents) { timeBlock ->
                    TimeBlockDisplay(
                        timeBlock = timeBlock,
                        onClick = { event -> selectedEvent = event }
                    )
                }
            }
        } else {
            // Show no events message
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Não há eventos para esse dia!",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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

/**
 * Process events into time blocks, ensuring that each event appears only once
 * within its appropriate time range
 */
private fun processEventsIntoTimeBlocks(
    eventsWithOccurrences: List<Pair<Event, EventOccurrence>>,
    startHour: Int,
    endHour: Int
): List<TimeBlock> {
    val timeBlocks = mutableListOf<TimeBlock>()

    if (eventsWithOccurrences.isEmpty()) {
        // If no events, return an empty list - we'll handle this case separately in the UI
        return emptyList()
    }

    // Find the actual time range needed for events
    val firstEventHour = eventsWithOccurrences.minOfOrNull { (_, occurrence) ->
        occurrence.startTime.hour
    } ?: startHour

    val lastEventHour = eventsWithOccurrences.maxOfOrNull { (_, occurrence) ->
        occurrence.endTime.hour
    } ?: endHour

    // Use either the specified range or the actual event range, whichever is more inclusive
//    val effectiveStartHour = firstEventHour.coerceAtMost(startHour)
    val effectiveStartHour = maxOf(startHour, firstEventHour)
    val effectiveEndHour = lastEventHour.coerceAtLeast(endHour).coerceAtMost(23)

    // Create a time block for each hour in our display range
    for (hour in effectiveStartHour..effectiveEndHour) {
        // Get events that are active during this hour
        val eventsAtThisHour = eventsWithOccurrences.filter { (_, occurrence) ->
            val eventStart = occurrence.startTime.hour
            val eventEnd = occurrence.endTime.hour

            // Event is active if it starts at or before this hour and ends after this hour
            (eventStart <= hour && eventEnd > hour) ||
                    // Or if it starts during this hour
                    (eventStart == hour)
        }

        if (eventsAtThisHour.isNotEmpty()) {
            // Only create a new block if we don't have one already for these events
            val existingBlock = timeBlocks.find { block ->
                block.events.all { it in eventsAtThisHour } &&
                        eventsAtThisHour.all { it in block.events } &&
                        block.endHour == hour
            }

            if (existingBlock != null) {
                // Extend existing block if it contains exactly the same events
                val index = timeBlocks.indexOf(existingBlock)
                timeBlocks[index] = existingBlock.copy(endHour = hour + 1)
            } else {
                // Create a new block
                timeBlocks.add(TimeBlock(hour, hour + 1, eventsAtThisHour))
            }
        } else {
            // For empty hours, only create a block if it's between events
            // This avoids creating empty blocks at the beginning and end of the day
            if (hour in effectiveStartHour..effectiveEndHour &&
                (timeBlocks.isNotEmpty() || eventsWithOccurrences.any { (_, o) -> o.startTime.hour > hour })) {
                timeBlocks.add(TimeBlock(hour, hour + 1, emptyList()))
            }
        }
    }

    // Merge adjacent blocks with the same events
    return mergeAdjacentBlocks(timeBlocks)
}

/**
 * Merge adjacent time blocks that contain the same events
 */
private fun mergeAdjacentBlocks(blocks: List<TimeBlock>): List<TimeBlock> {
    val result = mutableListOf<TimeBlock>()
    var currentBlock: TimeBlock? = null

    for (block in blocks) {
        if (currentBlock == null) {
            currentBlock = block
        } else if (currentBlock.endHour == block.startHour &&
            currentBlock.events.size == block.events.size &&
            currentBlock.events.containsAll(block.events)) {
            // Merge blocks if they are adjacent and have the same events
            currentBlock = currentBlock.copy(endHour = block.endHour)
        } else {
            result.add(currentBlock)
            currentBlock = block
        }
    }

    // Add the last block if there is one
    currentBlock?.let { result.add(it) }

    return result
}

@Composable
fun TimeBlockDisplay(
    timeBlock: TimeBlock,
    onClick: (Event) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Hour indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Format time range for the block
            val startTime = String.format("%02d:00", timeBlock.startHour) +
                    if (timeBlock.startHour < 12) " AM" else " PM"

            Text(
                text = startTime,
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

        // Events in this time block
        if (timeBlock.events.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp)
            ) {
                timeBlock.events.forEach { (event, occurrence) ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        // Create a formatted time string for this occurrence
                        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
                        val timeText = "${occurrence.startTime.format(timeFormatter)} - ${occurrence.endTime.format(timeFormatter)}"

                        // Display event card with the specific occurrence
                        EventCard(
                            event = event.copy(
                                occurrences = listOf(occurrence)
                            ),
                            onClick = { onClick(event) }
                        )
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