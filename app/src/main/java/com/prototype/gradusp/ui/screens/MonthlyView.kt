package com.prototype.gradusp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Button
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prototype.gradusp.data.model.Event
import com.prototype.gradusp.data.model.EventImportance
import com.prototype.gradusp.ui.components.dialogs.DayDetailsDialog
import com.prototype.gradusp.ui.components.dialogs.EventDetailsDialog
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.ceil

@Composable
fun MonthlyView(events: List<Event>, onUpdateEvent: (Event) -> Unit) {
    var selectedEvent by remember { mutableStateOf<Event?>(null) }
    var selectedDayEvents by remember { mutableStateOf<Pair<LocalDate, List<Event>>?>(null) }
    val currentMonth = remember { YearMonth.now() }
    var selectedMonth by remember { mutableStateOf(currentMonth) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(6.dp)
    ) {
        // Month navigation header with today button
        MonthNavigationHeader(
            currentMonth = selectedMonth,
            onPreviousMonth = { selectedMonth = selectedMonth.minusMonths(1) },
            onNextMonth = { selectedMonth = selectedMonth.plusMonths(1) },
            onTodayClick = {
                selectedMonth = YearMonth.now()
                selectedDate = LocalDate.now()
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Days of week header
        WeekdaysHeader()

        // Enhanced calendar grid - now fills most of the screen
        EnhancedCalendarGrid(
            yearMonth = selectedMonth,
            events = events,
            selectedDate = selectedDate,
            onDateClick = { date, dayEvents ->
                selectedDate = date
                if (dayEvents.isNotEmpty()) {
                    selectedDayEvents = Pair(date, dayEvents)
                }
            },
            onEventClick = { selectedEvent = it }
        )
    }

    // Day details modal dialog
    selectedDayEvents?.let { (date, dayEvents) ->
        DayDetailsDialog(
            date = date,
            events = dayEvents,
            onDismiss = { selectedDayEvents = null },
            onEventClick = { selectedEvent = it }
        )
    }

    // Event details dialog
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
fun MonthNavigationHeader(
    currentMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onTodayClick: () -> Unit = {}
) {
    val isCurrentMonth = currentMonth.equals(YearMonth.now())

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPreviousMonth) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowLeft,
                    contentDescription = "Mês anterior"
                )
            }

            Text(
                text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale("pt", "BR"))),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            IconButton(onClick = onNextMonth) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "Próximo mês"
                )
            }
        }

        // Today button - only show if not on current month
        androidx.compose.animation.AnimatedVisibility(visible = !isCurrentMonth) {
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
                        contentDescription = "Mês atual",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Hoje")
                }
            }
        }
    }
}

@Composable
fun WeekdaysHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp) // Added vertical padding
    ) {
        val daysOfWeek = DayOfWeek.values()

        for (dayOfWeek in daysOfWeek) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("pt", "BR")),
                    style = MaterialTheme.typography.bodyLarge, // Increased text size
                    fontWeight = FontWeight.SemiBold // Made font weight slightly heavier
                )
            }
        }
    }
}

@Composable
fun EnhancedCalendarGrid(
    yearMonth: YearMonth,
    events: List<Event>,
    selectedDate: LocalDate?,
    onDateClick: (LocalDate, List<Event>) -> Unit,
    onEventClick: (Event) -> Unit
) {
    val firstDayOfMonth = yearMonth.atDay(1)
    val lastDayOfMonth = yearMonth.atEndOfMonth()

    // Adjust to make Sunday as first day of week (1-7 where 1 is Monday in ISO)
    val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7

    val totalDays = lastDayOfMonth.dayOfMonth
    val totalCells = firstDayOfWeek + totalDays
    val totalRows = ceil(totalCells / 7.0).toInt()
    val today = LocalDate.now()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 450.dp) // Increased minimum height
    ) {
        for (row in 0 until totalRows) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                for (column in 0 until 7) {
                    val cellIndex = row * 7 + column
                    val dayOfMonth = cellIndex - firstDayOfWeek + 1

                    if (dayOfMonth in 1..totalDays) {
                        val date = yearMonth.atDay(dayOfMonth)
                        val dayOfWeek = date.dayOfWeek

                        // Get events for this specific day
                        val dayEvents = events.filter { event ->
                            event.occurrences.any { it.day == dayOfWeek }
                        }

                        val hasEvents = dayEvents.isNotEmpty()
                        val isSelected = date.equals(selectedDate)
                        val isToday = date.equals(today)

                        EnhancedCalendarDay(
                            day = dayOfMonth,
                            date = date,
                            isToday = isToday,
                            isSelected = isSelected,
                            hasEvents = hasEvents,
                            events = dayEvents,
                            onClick = { onDateClick(date, dayEvents) },
                            onEventClick = onEventClick
                        )
                    } else {
                        // Empty cell
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(0.7f)
                                .padding(3.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RowScope.EnhancedCalendarDay(
    day: Int,
    date: LocalDate,
    isToday: Boolean,
    isSelected: Boolean,
    hasEvents: Boolean,
    events: List<Event> = emptyList(),
    onClick: () -> Unit,
    onEventClick: (Event) -> Unit
) {
    // Priority of visual emphasis: Selected > Today > Normal
    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        isToday -> MaterialTheme.colorScheme.primaryContainer
        else -> Color.Transparent
    }

    val borderColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    val borderWidth = when {
        isSelected -> 2.dp
        isToday -> 1.5.dp
        else -> 0.5.dp
    }

    Box(
        modifier = Modifier
            .weight(1f)
            .aspectRatio(0.7f) // Reduced aspect ratio to make boxes taller
            .padding(3.dp) // Slightly increased padding for better spacing
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(8.dp) // Slightly increased corner radius
            )
            .border(
                width = borderWidth,
                color = borderColor,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp) // Increased padding
        ) {
            // Day number
            Text(
                text = day.toString(),
                style = MaterialTheme.typography.bodyLarge, // Larger text size
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else if (isToday)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Enhanced event indicators - show more details
            if (events.isNotEmpty()) {
                // Group by importance first, then by color
                val highPriorityEvents = events.filter { it.importance == EventImportance.HIGH }
                val normalPriorityEvents = events.filter { it.importance == EventImportance.NORMAL }
                val lowPriorityEvents = events.filter { it.importance == EventImportance.LOW }

                // Sort by priority and display up to 4 events with mini titles
                val displayEvents = (highPriorityEvents + normalPriorityEvents + lowPriorityEvents).take(4)
                val remainingCount = events.size - displayEvents.size

                // Using a fixed height box with scroll to prevent clipping
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f) // Takes remaining space
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Now we're showing up to 4 events instead of 3
                        displayEvents.forEach { event ->
                            EnhancedEventIndicator(
                                event = event,
                                onClick = { onEventClick(event) }
                            )
                        }

                        // If there are more events, show a count
                        if (remainingCount > 0) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 3.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "+$remainingCount mais",
                                    style = MaterialTheme.typography.labelMedium, // Slightly larger font style
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f), // More visible color
                                    fontWeight = FontWeight.Medium // Added medium weight for better visibility
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EnhancedEventIndicator(
    event: Event,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when (event.importance) {
        EventImportance.HIGH -> event.color.copy(alpha = 0.9f)
        EventImportance.NORMAL -> event.color.copy(alpha = 0.7f)
        EventImportance.LOW -> event.color.copy(alpha = 0.5f)
    }

    val textColor = if (backgroundColor.luminance() > 0.5f) Color.Black else Color.White

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp, horizontal = 3.dp) // Increased padding
            .height(18.dp) // Increased height for better visibility
            .background(backgroundColor, RoundedCornerShape(3.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = event.title,
            fontSize = 9.sp, // Slightly larger font size
            fontWeight = if (event.importance == EventImportance.HIGH) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = textColor,
            modifier = Modifier.align(Alignment.CenterStart)
        )
    }
}