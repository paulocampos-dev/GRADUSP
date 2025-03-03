package com.prototype.gradusp.data.repository

import com.prototype.gradusp.data.model.Event
import com.prototype.gradusp.data.model.sampleEvents
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing Event data
 */
@Singleton
class EventRepository @Inject constructor() {
    // In a real app, this would be backed by a Room database
//    private val _events = MutableStateFlow(sampleEvents)
    private val _events = MutableStateFlow<List<Event>>(emptyList())
    val events: StateFlow<List<Event>> = _events.asStateFlow()

    /**
     * Get events filtered by day of week
     */
    fun getEventsByDayOfWeek(dayOfWeek: DayOfWeek): Flow<List<Event>> {
        return events.map { eventList ->
            eventList.filter { event ->
                event.occurrences.any { it.day == dayOfWeek }
            }
        }
    }

    /**
     * Get events for a specific date
     */
    fun getEventsByDate(date: LocalDate): Flow<List<Event>> {
        return getEventsByDayOfWeek(date.dayOfWeek)
    }

    /**
     * Update an existing event
     */
    suspend fun updateEvent(updatedEvent: Event) {
        _events.update { currentEvents ->
            currentEvents.map {
                if (it.title == updatedEvent.title) updatedEvent else it
            }
        }
    }

    /**
     * Add a new event
     */
    suspend fun addEvent(newEvent: Event) {
        _events.update { currentEvents ->
            currentEvents + newEvent
        }
    }

    /**
     * Delete an event
     */
    suspend fun deleteEvent(event: Event) {
        _events.update { currentEvents ->
            currentEvents.filter { it.title != event.title }
        }
    }
}