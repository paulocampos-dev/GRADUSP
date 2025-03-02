package com.prototype.gradusp.viewmodel

import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prototype.gradusp.data.model.Classroom
import com.prototype.gradusp.data.model.Event
import com.prototype.gradusp.data.model.EventImportance
import com.prototype.gradusp.data.model.EventOccurrence
import com.prototype.gradusp.data.model.Lecture
import com.prototype.gradusp.data.model.eventColors
import com.prototype.gradusp.data.repository.EventRepository
import com.prototype.gradusp.utils.DateTimeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val eventRepository: EventRepository
) : ViewModel() {
    // Calendar state
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate

    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    val selectedMonth: StateFlow<YearMonth> = _selectedMonth

    // All events
    val events = eventRepository.events.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Update event
    fun updateEvent(event: Event) {
        viewModelScope.launch {
            eventRepository.updateEvent(event)
        }
    }

    // Add event
    fun addEvent(event: Event) {
        viewModelScope.launch {
            eventRepository.addEvent(event)
        }
    }

    // Delete event
    fun deleteEvent(event: Event) {
        viewModelScope.launch {
            eventRepository.deleteEvent(event)
        }
    }

    // Navigation functions
    fun navigateToDate(date: LocalDate) {
        _selectedDate.value = date
        _selectedMonth.value = YearMonth.from(date)
    }

    fun navigateToPreviousDay() {
        _selectedDate.value = _selectedDate.value.minusDays(1)
    }

    fun navigateToNextDay() {
        _selectedDate.value = _selectedDate.value.plusDays(1)
    }

    fun navigateToPreviousMonth() {
        _selectedMonth.value = _selectedMonth.value.minusMonths(1)
    }

    fun navigateToNextMonth() {
        _selectedMonth.value = _selectedMonth.value.plusMonths(1)
    }

    fun navigateToToday() {
        _selectedDate.value = LocalDate.now()
        _selectedMonth.value = YearMonth.now()
    }

    // Helper functions for event processing
    fun getEventsForDate(date: LocalDate): List<Event> {
        return events.value.filter { event ->
            event.occurrences.any { it.day == date.dayOfWeek }
        }
    }

    fun getEventsForDayOfWeek(dayOfWeek: DayOfWeek): List<Event> {
        return events.value.filter { event ->
            event.occurrences.any { it.day == dayOfWeek }
        }
    }

    fun addLectureEvent(lecture: Lecture, classroom: Classroom) {
        viewModelScope.launch {
            try {
                // Filter out invalid schedules first
                val validSchedules = classroom.schedules.filter { schedule ->
                    val dayString = schedule.day.lowercase().trim()
                    // Skip headers or invalid day strings
                    !dayString.contains("horário") &&
                            (dayString.startsWith("seg") ||
                                    dayString.startsWith("ter") ||
                                    dayString.startsWith("qua") ||
                                    dayString.startsWith("qui") ||
                                    dayString.startsWith("sex") ||
                                    dayString.startsWith("sab") ||
                                    dayString.startsWith("sáb") ||
                                    dayString.startsWith("dom"))
                }

                // Convert lecture and classroom to an event
                val occurrences = validSchedules.mapNotNull { schedule ->
                    try {
                        val day = try {
                            DateTimeUtils.convertDayStringToDayOfWeek(schedule.day)
                        } catch (e: Exception) {
                            Log.e("CalendarViewModel", "Error parsing day: ${schedule.day}", e)
                            return@mapNotNull null
                        }

                        val startTime = try {
                            LocalTime.parse(schedule.startTime)
                        } catch (e: Exception) {
                            Log.e("CalendarViewModel", "Error parsing start time: ${schedule.startTime}", e)
                            return@mapNotNull null
                        }

                        val endTime = try {
                            LocalTime.parse(schedule.endTime)
                        } catch (e: Exception) {
                            Log.e("CalendarViewModel", "Error parsing end time: ${schedule.endTime}", e)
                            return@mapNotNull null
                        }

                        EventOccurrence(
                            day = day,
                            startTime = startTime,
                            endTime = endTime
                        )
                    } catch (e: Exception) {
                        Log.e("CalendarViewModel", "Error creating occurrence for schedule", e)
                        null
                    }
                }

                // Only create event if we have valid occurrences
                if (occurrences.isNotEmpty()) {
                    // Create a nice title that always includes the code
                    val eventTitle = if (lecture.name.isNotBlank()) {
                        "${lecture.code} - ${lecture.name}"
                    } else {
                        // Just use the code if name is empty
                        lecture.code
                    }

                    Log.d("CalendarViewModel", "Adding event with title: $eventTitle")

                    val event = Event(
                        title = eventTitle,
                        occurrences = occurrences,
                        color = getNextColor(),
                        recurring = true,
                        teacher = classroom.teachers.firstOrNull(),
                        location = classroom.observations.takeIf { it.isNotBlank() },
                        importance = EventImportance.HIGH
                    )

                    eventRepository.addEvent(event)
                } else {
                    Log.w("CalendarViewModel", "No valid occurrences found for ${lecture.code}")
                }
            } catch (e: Exception) {
                Log.e("CalendarViewModel", "Error adding lecture event: ${lecture.code}", e)
            }
        }
    }

    private fun getNextColor(): Color {
        // Simple rotation through the available colors
        val colorIndex = (events.value.size % eventColors.size)
        return eventColors[colorIndex]
    }
}

