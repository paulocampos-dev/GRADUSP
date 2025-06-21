package com.prototype.gradusp.ui.calendar

import com.prototype.gradusp.data.AnimationSpeed
import com.prototype.gradusp.data.model.Event
import com.prototype.gradusp.ui.calendar.daily.TimeBlock
import java.time.LocalDate
import java.time.YearMonth

data class CalendarUiState(
    // View selection state
    val selectedView: CalendarView = CalendarView.WEEKLY,

    // Date states
    val selectedDate: LocalDate = LocalDate.now(),
    val selectedMonth: YearMonth = YearMonth.now(),

    // Event data
    val events: List<Event> = emptyList(),
    val dailyViewTimeBlocks: List<TimeBlock> = emptyList(),
    val daysInMonthGrid: List<LocalDate?> = emptyList(),

    // Dialog visibility states
    val showAddEventDialog: Boolean = false,
    val showAddLectureDialog: Boolean = false,
    val eventForDetailsDialog: Event? = null,
    val dateForDetailsDialog: LocalDate? = null,

    // UI settings from other view models
    val animationSpeed: AnimationSpeed = AnimationSpeed.MÉDIA,
    val invertSwipeDirection: Boolean = false,
)