package com.prototype.gradusp.viewmodel

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
            // Convert lecture and classroom to an event
            val occurrences = classroom.schedules.map { schedule ->
                EventOccurrence(
                    day = DateTimeUtils.convertDayStringToDayOfWeek(schedule.day),
                    startTime = LocalTime.parse(schedule.startTime),
                    endTime =LocalTime.parse(schedule.endTime)
                )
            }

            val event = Event(
                title = "${lecture.code} - ${lecture.name}",
                occurrences = occurrences,
                color = getNextColor(),
                recurring = true,
                teacher = classroom.teachers.firstOrNull(),
                location = classroom.observations.takeIf { it.isNotBlank() },
                importance = EventImportance.HIGH
            )

            eventRepository.addEvent(event)
        }

    }

    private fun getNextColor(): Color {
        // Simple rotation through the available colors
        val colorIndex = (events.value.size % eventColors.size)
        return eventColors[colorIndex]
    }
}

