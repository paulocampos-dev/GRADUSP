package com.prototype.gradusp.ui.calendar.daily

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.prototype.gradusp.data.model.Event
import com.prototype.gradusp.data.model.EventOccurrence
import com.prototype.gradusp.ui.components.dialogs.EventDetailsDialog
import com.prototype.gradusp.utils.EventProcessingUtil
import com.prototype.gradusp.viewmodel.CalendarViewModel

// Data class to represent a time block with events for the day view
data class TimeBlock(
    val startHour: Int,
    val endHour: Int,
    val events: List<Pair<Event, EventOccurrence>>
)

@Composable
fun DailyView(
    viewModel: CalendarViewModel = hiltViewModel()
) {
    // Observe selected date and events
    val selectedDate by viewModel.selectedDate.collectAsState()
    val allEvents by viewModel.events.collectAsState()

    var selectedEvent by remember { mutableStateOf<Event?>(null) }

    // Time ranges for display
    val startHour = 7 // 7 AM
    val endHour = 22 // 10 PM

    // Memoize event processing to avoid recomputing on every recomposition
    val dayEvents by remember(allEvents, selectedDate) {
        derivedStateOf {
            EventProcessingUtil.processEventsIntoTimeBlocks(
                events = allEvents,
                dayOfWeek = selectedDate.dayOfWeek,
                startHour = startHour,
                endHour = endHour
            )
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
            onPreviousDay = { viewModel.navigateToPreviousDay() },
            onNextDay = { viewModel.navigateToNextDay() },
            onTodayClick = { viewModel.navigateToToday() }
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
                viewModel.updateEvent(updatedEvent)
                selectedEvent = null
            }
        )
    }
}