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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.prototype.gradusp.data.model.Event
import com.prototype.gradusp.ui.components.dialogs.DayDetailsDialog
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun MonthlyView(events: List<Event>, onUpdateEvent: (Event) -> Unit) {
    // State to hold the currently selected day.
    var selectedDay by remember { mutableStateOf<LocalDate?>(null) }
    val currentMonth = remember { YearMonth.now() }
    var selectedMonth by remember { mutableStateOf(currentMonth) }

    // Build a list of cells for the grid so that Sunday is the first column.
    val daysInGrid = remember(selectedMonth) {
        val firstDayOfMonth = selectedMonth.atDay(1)
        val totalDays = selectedMonth.lengthOfMonth()
        // Using ISO (Monday=1...Sunday=7), convert so Sunday becomes index 0.
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
            .padding(6.dp)
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
                    // Filter events for this day (by matching the day-of-week).
                    val dayEvents = events.filter { event ->
                        event.occurrences.any { it.day == date.dayOfWeek }
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
        val dayEvents = events.filter { event ->
            event.occurrences.any { it.day == date.dayOfWeek }
        }
        DayDetailsDialog(
            date = date,
            events = dayEvents,
            onDismiss = { selectedDay = null },
            onEventClick = { updatedEvent ->
                onUpdateEvent(updatedEvent)
                selectedDay = null
            }
        )
    }
}

@Composable
fun DayCell(
    date: LocalDate,
    dayEvents: List<Event>,
    onClick: () -> Unit
) {
    val isToday = date == LocalDate.now()
    // Mimic Google Calendar’s subtle border and background for today.
    val cellBackground = if (isToday) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent
    Box(
        modifier = Modifier
            .padding(4.dp)
            .fillMaxWidth()
            .height(100.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(cellBackground)
            .border(
                width = if (isToday) 2.dp else 0.5.dp,
                color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                shape = RoundedCornerShape(4.dp)
            )
            .clickable { onClick() }
            .padding(4.dp),
        contentAlignment = Alignment.TopStart
    ) {
        Column(
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.fillMaxSize()
        ) {
            // Display the day number in the top-left corner.
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
            )
            Spacer(modifier = Modifier.height(4.dp))
            // Display up to 2 event bars.
            val maxDisplay = 2
            val displayEvents = dayEvents.take(maxDisplay)
            displayEvents.forEach { event ->
                EventBar(event = event)
                Spacer(modifier = Modifier.height(2.dp))
            }
            if (dayEvents.size > maxDisplay) {
                val remaining = dayEvents.size - maxDisplay
                Text(
                    text = "+$remaining more",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Start,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun EventBar(event: Event) {
    // Create a small bar to mimic the event indicator.
    val backgroundColor = event.color.copy(alpha = 0.9f)
    val textColor = if (backgroundColor.luminance() > 0.5f) Color.Black else Color.White
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(16.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(backgroundColor)
            .padding(horizontal = 2.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = event.title,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
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
                    contentDescription = "Previous month"
                )
            }
            Text(
                text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale("pt", "BR"))),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onNextMonth) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next month"
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
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
        }
    }
}
