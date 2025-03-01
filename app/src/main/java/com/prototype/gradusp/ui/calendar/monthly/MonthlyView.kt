package com.prototype.gradusp.ui.calendar.monthly

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.prototype.gradusp.ui.components.dialogs.DayDetailsDialog
import com.prototype.gradusp.utils.DateTimeUtils
import com.prototype.gradusp.utils.EventProcessingUtil
import com.prototype.gradusp.viewmodel.CalendarViewModel
import java.time.LocalDate

@Composable
fun MonthlyView(
    viewModel: CalendarViewModel
) {
    // Observe view model states
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val events by viewModel.events.collectAsState()
    var selectedDay by remember { mutableStateOf<LocalDate?>(null) }

    // Memoized grid cells computation
    val daysInGrid by remember(selectedMonth) {
        derivedStateOf {
            DateTimeUtils.getDatesForMonthGrid(selectedMonth)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(6.dp)
    ) {
        MonthNavigationHeader(
            currentMonth = selectedMonth,
            onPreviousMonth = { viewModel.navigateToPreviousMonth() },
            onNextMonth = { viewModel.navigateToNextMonth() },
            onTodayClick = { viewModel.navigateToToday() }
        )

        WeekdaysHeader()

        Spacer(modifier = Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            contentPadding = PaddingValues(4.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(daysInGrid) { date ->
                if (date != null) {
                    // Filter events for this day (by matching the day-of-week)
                    val dayEvents = remember(events, date) {
                        EventProcessingUtil.getEventsForDate(date, events)
                    }

                    DayCell(
                        date = date,
                        dayEvents = dayEvents,
                        onClick = { selectedDay = date }
                    )
                } else {
                    // Empty cell for padding.
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .fillMaxWidth()
                            .height(100.dp)
                    )
                }
            }
        }
    }

    // When a day is selected, show the DayDetailsDialog.
    selectedDay?.let { date ->
        val dayEvents = remember(events, date) {
            EventProcessingUtil.getEventsForDate(date, events)
        }

        DayDetailsDialog(
            date = date,
            events = dayEvents,
            onDismiss = { selectedDay = null },
            onEventClick = { event ->
                // Navigate to event details or edit screen
                viewModel.updateEvent(event)
                selectedDay = null
            }
        )
    }
}