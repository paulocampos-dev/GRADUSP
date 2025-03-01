package com.prototype.gradusp.ui.screens.views

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.prototype.gradusp.ui.components.dialogs.DayDetailsDialog
import com.prototype.gradusp.ui.components.dialogs.EventDetailsDialog
import com.prototype.gradusp.data.model.Event
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun MonthlyView(events: List<Event>, onUpdateEvent: (Event) -> Unit) {
    var selectedEvent by remember { mutableStateOf<Event?>(null) }
    var selectedDay by remember { mutableStateOf<LocalDate?>(null) }
    val currentMonth = remember { YearMonth.now() }
    var selectedMonth by remember { mutableStateOf(currentMonth) }
    val coroutineScope = rememberCoroutineScope()

    // Create a list of cells for the grid:
    val daysInGrid = remember(selectedMonth) {
        val firstDayOfMonth = selectedMonth.atDay(1)
        val totalDays = selectedMonth.lengthOfMonth()
        // Calculate offset – using Sunday as first day (Sunday = 0)
        val firstDayOffset = firstDayOfMonth.dayOfWeek.value % 7
        val totalCells = firstDayOffset + totalDays
        val extra = if (totalCells % 7 == 0) 0 else 7 - (totalCells % 7)
        val cells = mutableListOf<LocalDate?>()
        repeat(firstDayOffset) { cells.add(null) }
        for (day in 1..totalDays) {
            cells.add(selectedMonth.atDay(day))
        }
        repeat(extra) { cells.add(null) }
        cells
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        MonthNavigationHeader(
            currentMonth = selectedMonth,
            onPreviousMonth = { selectedMonth = selectedMonth.minusMonths(1) },
            onNextMonth = { selectedMonth = selectedMonth.plusMonths(1) },
            onTodayClick = { selectedMonth = YearMonth.now() }
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
                    // Get events for the given date by matching the day-of-week
                    val dayEvents = events.filter { event ->
                        event.occurrences.any { it.day == date.dayOfWeek }
                    }
                    DayCell(
                        date = date,
                        dayEvents = dayEvents,
                        isToday = date == LocalDate.now(),
                        isSelected = date == selectedDay,
                        onClick = { selectedDay = date },
                        onEventClick = { selectedEvent = it }
                    )
                } else {
                    // Empty cell
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Transparent)
                    )
                }
            }
        }
    }

    // Show dialog with the day’s events (if any) when a day is selected
    selectedDay?.let { date ->
        val dayEvents = events.filter { event ->
            event.occurrences.any { it.day == date.dayOfWeek }
        }
        if (dayEvents.isNotEmpty()) {
            DayDetailsDialog(
                date = date,
                events = dayEvents,
                onDismiss = { selectedDay = null },
                onEventClick = { selectedEvent = it }
            )
        } else {
            // Clear selection if no events exist
            selectedDay = null
        }
    }

    // Show event details dialog when an event is selected
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
fun DayCell(
    date: LocalDate,
    dayEvents: List<Event>,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onEventClick: (Event) -> Unit
) {
    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        isToday -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
        else -> Color.Transparent
    }
    Box(
        modifier = Modifier
            .padding(4.dp)
            .clickable { onClick() }
            .background(backgroundColor, shape = RoundedCornerShape(8.dp))
            .border(
                width = if (isSelected || isToday) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            )
            .fillMaxWidth()
            .aspectRatio(1f),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal
            )
            if (dayEvents.isNotEmpty()) {
                EventIndicators(dayEvents = dayEvents, onEventClick = onEventClick)
            }
        }
    }
}

@Composable
fun EventIndicators(dayEvents: List<Event>, onEventClick: (Event) -> Unit) {
    // Show up to three event indicators (dots) and a "+X" if there are more
    val displayEvents = dayEvents.take(3)
    val remaining = dayEvents.size - displayEvents.size
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        displayEvents.forEach { event ->
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color = event.color)
                    .clickable { onEventClick(event) }
            )
            Spacer(modifier = Modifier.padding(1.dp))
        }
        if (remaining > 0) {
            Text(
                text = "+$remaining",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(start = 2.dp)
            )
        }
    }
}

@Composable
fun MonthNavigationHeader(
    currentMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onTodayClick: () -> Unit = {}
) {
    val isCurrentMonth = currentMonth == YearMonth.now()
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPreviousMonth) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.KeyboardArrowLeft,
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
                    imageVector = androidx.compose.material.icons.Icons.Default.KeyboardArrowRight,
                    contentDescription = "Próximo mês"
                )
            }
        }
        if (!isCurrentMonth) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                OutlinedButton(onClick = onTodayClick) {
                    Text("Hoje")
                }
            }
        }
    }
}

@Composable
fun WeekdaysHeader() {
    val daysOfWeek = listOf("Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        daysOfWeek.forEach { day ->
            Text(
                text = day,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
        }
    }
}
