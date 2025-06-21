package com.prototype.gradusp.ui.calendar.monthly

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.prototype.gradusp.data.model.Event
import com.prototype.gradusp.data.model.sampleEvents
import com.prototype.gradusp.ui.theme.GRADUSPTheme
import com.prototype.gradusp.utils.AccessibilityUtils
import com.prototype.gradusp.utils.DateTimeUtils
import java.time.LocalDate

@Composable
fun DayCell(
    date: LocalDate,
    dayEvents: List<Event>,
    onClick: () -> Unit
) {
    val isToday = DateTimeUtils.isToday(date)
    // Mimic Google Calendar's subtle border and background for today.
    val cellBackground = if (isToday) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent

    // Create accessibility description
    val formattedDate = DateTimeUtils.formatFullDate(date)
    val eventCount = dayEvents.size
    val accessibilityDescription = AccessibilityUtils.getDateAccessibilityDescription(
        dayName = DateTimeUtils.getDayName(date.dayOfWeek),
        date = formattedDate,
        eventCount = eventCount
    )

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
            .padding(4.dp)
            .semantics { contentDescription = accessibilityDescription },
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

@Preview(showBackground = true, name = "Day Cell - Regular")
@Composable
fun DayCellPreview() {
    GRADUSPTheme {
        DayCell(date = LocalDate.of(2023, 10, 26), dayEvents = emptyList(), onClick = {})
    }
}

@Preview(showBackground = true, name = "Day Cell - Today")
@Composable
fun DayCellTodayPreview() {
    GRADUSPTheme {
        DayCell(date = LocalDate.now(), dayEvents = sampleEvents.take(1), onClick = {})
    }
}

@Preview(showBackground = true, name = "Day Cell - With Events")
@Composable
fun DayCellWithEventsPreview() {
    GRADUSPTheme {
        DayCell(date = LocalDate.of(2023, 10, 27), dayEvents = sampleEvents, onClick = {})
    }
}