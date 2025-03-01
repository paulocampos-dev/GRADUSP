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
    val teacher: String? = null,
    val location: String? = null,
    val notes: String? = null,
    val importance: EventImportance = EventImportance.NORMAL
)

data class EventOccurrence(
    val day: DayOfWeek,
    val startTime: LocalTime,
    val endTime: LocalTime,
)

enum class EventImportance {
    LOW,
    NORMAL,
    HIGH
}


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
        title = "Cálculo I",
        occurrences = listOf(
            EventOccurrence(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(10, 0)),
            EventOccurrence(DayOfWeek.WEDNESDAY, LocalTime.of(13, 0), LocalTime.of(15, 0))
        ),
        color = eventColors[0],
        recurring = true,
        teacher = "Dr. Marcelo Santos",
        location = "Sala 103B - Bloco 4",
        importance = EventImportance.HIGH
    ),
    Event(
        title = "Física I",
        occurrences = listOf(
            EventOccurrence(DayOfWeek.TUESDAY, LocalTime.of(10, 0), LocalTime.of(12, 0))
        ),
        color = eventColors[1],
        recurring = true,
        teacher = "Profa. Ana Lima",
        location = "Laboratório de Física, Bloco 3"
    ),
    Event(
        title = "Programação",
        occurrences = listOf(
            EventOccurrence(DayOfWeek.THURSDAY, LocalTime.of(8, 0), LocalTime.of(12, 0))
        ),
        color = eventColors[2],
        recurring = true,
        teacher = "Prof. Carlos Oliveira",
        location = "Laboratório de Informática"
    ),
    Event(
        title = "Monitoria de Cálculo",
        occurrences = listOf(
            EventOccurrence(DayOfWeek.FRIDAY, LocalTime.of(14, 0), LocalTime.of(16, 0))
        ),
        color = eventColors[3],
        recurring = true,
        importance = EventImportance.LOW,
        notes = "Trazer lista de exercícios 3 e 4"
    ),
    Event(
        title = "TESTE",
        occurrences = listOf(
            EventOccurrence(DayOfWeek.WEDNESDAY, LocalTime.of(18, 0), LocalTime.of(20, 0))
        ),
        color = eventColors[3],
        recurring = true,
        importance = EventImportance.LOW,
        notes = "SEI LA CARA"
    )
)

val recurringEvents = sampleEvents.filter { it.recurring }.flatMap { generateRecurringEvents(it, 4) }
val allEvents = sampleEvents + recurringEvents