package com.prototype.gradusp.viewmodel

import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prototype.gradusp.data.UserPreferencesRepository
import com.prototype.gradusp.data.model.Classroom
import com.prototype.gradusp.data.model.Event
import com.prototype.gradusp.data.model.EventImportance
import com.prototype.gradusp.data.model.EventOccurrence
import com.prototype.gradusp.data.model.Lecture
import com.prototype.gradusp.data.model.eventColors
import com.prototype.gradusp.data.repository.EventRepository
import com.prototype.gradusp.ui.calendar.CalendarUiState
import com.prototype.gradusp.ui.calendar.CalendarView
import com.prototype.gradusp.utils.DateTimeUtils
import com.prototype.gradusp.utils.EventProcessingUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState = _uiState.asStateFlow()

    init {
        // Combine multiple flows to build the final UI state
        viewModelScope.launch {
            combine(
                eventRepository.events,
                userPreferencesRepository.animationSpeedFlow,
                userPreferencesRepository.invertSwipeDirectionFlow,
            ) { events, animationSpeed, invertSwipe ->
                // This block will be re-executed whenever any of the source flows emit a new value
                val currentState = _uiState.value
                val dailyTimeBlocks = EventProcessingUtil.processEventsIntoTimeBlocks(
                    events = events,
                    dayOfWeek = currentState.selectedDate.dayOfWeek,
                    startHour = 7,
                    endHour = 22
                )
                val monthGrid = DateTimeUtils.getDatesForMonthGrid(currentState.selectedMonth)

                currentState.copy(
                    events = events,
                    animationSpeed = animationSpeed,
                    invertSwipeDirection = invertSwipe,
                    dailyViewTimeBlocks = dailyTimeBlocks,
                    daysInMonthGrid = monthGrid
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    // --- Event Handlers ---

    fun onViewSelected(view: CalendarView) {
        _uiState.update { it.copy(selectedView = view) }
    }

    fun onDateSelected(date: LocalDate) {
        _uiState.update {
            it.copy(
                selectedDate = date,
                selectedMonth = YearMonth.from(date)
            )
        }
        // Recalculate daily and monthly views
        recalculateDerivedState()
    }

    fun onPreviousDay() = onDateSelected(_uiState.value.selectedDate.minusDays(1))
    fun onNextDay() = onDateSelected(_uiState.value.selectedDate.plusDays(1))

    fun onPreviousMonth() {
        val newMonth = _uiState.value.selectedMonth.minusMonths(1)
        _uiState.update { it.copy(selectedMonth = newMonth) }
        recalculateDerivedState()
    }

    fun onNextMonth() {
        val newMonth = _uiState.value.selectedMonth.plusMonths(1)
        _uiState.update { it.copy(selectedMonth = newMonth) }
        recalculateDerivedState()
    }

    fun onTodayClick() = onDateSelected(LocalDate.now())

    fun onAddEventClick() {
        _uiState.update { it.copy(showAddEventDialog = true) }
    }

    fun onAddLectureClick() {
        _uiState.update { it.copy(showAddLectureDialog = true) }
    }

    fun onDialogDismiss() {
        _uiState.update {
            it.copy(
                showAddEventDialog = false,
                showAddLectureDialog = false,
                eventForDetailsDialog = null,
                dateForDetailsDialog = null
            )
        }
    }

    fun onEventClick(event: Event) {
        _uiState.update { it.copy(eventForDetailsDialog = event) }
    }

    fun onDayClick(date: LocalDate) {
        _uiState.update { it.copy(dateForDetailsDialog = date) }
    }


    fun addEvent(event: Event) = viewModelScope.launch {
        eventRepository.addEvent(event)
        onDialogDismiss() // Close dialog after adding
    }

    fun updateEvent(event: Event) = viewModelScope.launch {
        eventRepository.updateEvent(event)
        onDialogDismiss()
    }

    fun deleteEvent(event: Event) = viewModelScope.launch {
        eventRepository.deleteEvent(event)
        onDialogDismiss()
    }

    fun addLectureEvent(lecture: Lecture, classroom: Classroom) {
        viewModelScope.launch {
            try {
                val validSchedules = classroom.schedules.filter { schedule ->
                    val dayString = schedule.day.lowercase().trim()
                    !dayString.contains("horário") && (dayString.startsWith("seg") || dayString.startsWith("ter") || dayString.startsWith("qua") || dayString.startsWith("qui") || dayString.startsWith("sex") || dayString.startsWith("sab") || dayString.startsWith("sáb") || dayString.startsWith("dom"))
                }

                val occurrences = validSchedules.mapNotNull { schedule ->
                    try {
                        EventOccurrence(
                            day = DateTimeUtils.convertDayStringToDayOfWeek(schedule.day),
                            startTime = LocalTime.parse(schedule.startTime),
                            endTime = LocalTime.parse(schedule.endTime)
                        )
                    } catch (e: Exception) {
                        Log.e("CalendarViewModel", "Error creating occurrence", e)
                        null
                    }
                }

                if (occurrences.isNotEmpty()) {
                    val eventTitle = "${lecture.code} - ${lecture.name}".takeIf { lecture.name.isNotBlank() } ?: lecture.code
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
                    Log.w("CalendarViewModel", "No valid occurrences for ${lecture.code}")
                }
            } catch (e: Exception) {
                Log.e("CalendarViewModel", "Error adding lecture event", e)
            } finally {
                onDialogDismiss()
            }
        }
    }

    // --- Private Helpers ---

    private fun recalculateDerivedState() {
        val currentState = _uiState.value
        val dailyTimeBlocks = EventProcessingUtil.processEventsIntoTimeBlocks(
            events = currentState.events,
            dayOfWeek = currentState.selectedDate.dayOfWeek,
            startHour = 7,
            endHour = 22
        )
        val monthGrid = DateTimeUtils.getDatesForMonthGrid(currentState.selectedMonth)
        _uiState.update {
            it.copy(
                dailyViewTimeBlocks = dailyTimeBlocks,
                daysInMonthGrid = monthGrid
            )
        }
    }

    private fun getNextColor(): Color {
        val colorIndex = (_uiState.value.events.size % eventColors.size)
        return eventColors[colorIndex]
    }
}