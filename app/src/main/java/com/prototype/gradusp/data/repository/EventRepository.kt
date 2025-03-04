package com.prototype.gradusp.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.prototype.gradusp.data.dataStore
import com.prototype.gradusp.data.model.Event
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.io.IOException
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Repository for managing Event data with persistence using DataStore
 */
@Singleton
class EventRepository @Inject constructor(
    private val context: Context
) {
    companion object {
        private val EVENTS_KEY = stringPreferencesKey("user_events")
    }

    private val gson = GsonBuilder()
        .registerTypeAdapter(LocalTime::class.java, LocalTimeTypeAdapter())
        .registerTypeAdapter(DayOfWeek::class.java, DayOfWeekTypeAdapter())
        .create()

    private val _events = MutableStateFlow<List<Event>>(emptyList())
    val events: StateFlow<List<Event>> = _events.asStateFlow()

    init {
        // Load saved events when repository is initialized
        CoroutineScope(Dispatchers.IO).launch {
            loadEvents()
        }
    }

    private suspend fun loadEvents() {
        try {
            context.dataStore.data
                .catch { exception ->
                    if (exception is IOException) {
                        emit(androidx.datastore.preferences.core.emptyPreferences())
                    } else {
                        throw exception
                    }
                }
                .collect { preferences ->
                    val eventsJson = preferences[EVENTS_KEY] ?: "[]"
                    val type = object : TypeToken<List<Event>>() {}.type
                    val savedEvents = gson.fromJson<List<Event>>(eventsJson, type) ?: emptyList()
                    _events.value = savedEvents
                }
        } catch (e: Exception) {
            // If there's an error loading events, the _events flow will remain with its initial empty list
            e.printStackTrace()
        }
    }

    /**
     * Save events to persistent storage
     */
    private suspend fun saveEvents(events: List<Event>) {
        try {
            val eventsJson = gson.toJson(events)
            context.dataStore.edit { preferences ->
                preferences[EVENTS_KEY] = eventsJson
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

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
        // Save to persistent storage
        saveEvents(_events.value)
    }

    /**
     * Add a new event
     */
    suspend fun addEvent(newEvent: Event) {
        _events.update { currentEvents ->
            currentEvents + newEvent
        }
        // Save to persistent storage
        saveEvents(_events.value)
    }

    /**
     * Delete an event
     */
    suspend fun deleteEvent(event: Event) {
        _events.update { currentEvents ->
            currentEvents.filter { it.title != event.title }
        }
        // Save to persistent storage
        saveEvents(_events.value)
    }
}