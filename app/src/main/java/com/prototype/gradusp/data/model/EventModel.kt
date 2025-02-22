package com.prototype.gradusp.data.model

import androidx.compose.ui.graphics.Color
import java.time.DayOfWeek
import java.time.LocalTime

val eventColors = listOf(Color.Blue, Color.Red, Color.Green, Color.Magenta, Color.Cyan)

data class Event(
    val title: String,
    val occurrences: List<EventOccurrence>,
    val color: Color,
    val recurring: Boolean = false,
)

data class EventOccurrence(
    val day: DayOfWeek,
    val startTime: LocalTime,
    val endTime: LocalTime,
)


fun generateRecurringEvents(baseEvent: Event, repeatWeeks: Int): List<Event> {
    return (0 until repeatWeeks).flatMap { weekOffset ->
        baseEvent.occurrences.map { occurrence ->
            EventOccurrence(
                day = occurrence.day,
                startTime = occurrence.startTime,
                endTime = occurrence.endTime
            )
        }
    }.let { occurrences ->
        listOf(baseEvent.copy(occurrences = occurrences))
    }
}


// Sample Data (Mocked from the Parser)
val sampleEvents = listOf(
    Event(
        title = "Math Class",
        occurrences = listOf(
            EventOccurrence(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(10, 0)),
            EventOccurrence(DayOfWeek.WEDNESDAY, LocalTime.of(13, 0), LocalTime.of(15, 0))
        ),
        color = Color.Blue,
        recurring = true
    ),
    Event(
        title = "Physics Class",
        occurrences = listOf(
            EventOccurrence(DayOfWeek.TUESDAY, LocalTime.of(10, 0), LocalTime.of(12, 0))
        ),
        color = Color.Red,
        recurring = true
    )
)

val recurringEvents = sampleEvents.filter { it.recurring }.flatMap { generateRecurringEvents(it, 4) }
val allEvents = sampleEvents + recurringEvents
