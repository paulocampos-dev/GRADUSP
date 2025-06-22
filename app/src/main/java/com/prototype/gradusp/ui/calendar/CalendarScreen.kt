package com.prototype.gradusp.ui.calendar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.hilt.navigation.compose.hiltViewModel
import com.prototype.gradusp.data.model.Event
import com.prototype.gradusp.data.model.Lecture
import com.prototype.gradusp.data.model.Classroom
import com.prototype.gradusp.data.model.sampleEvents
import com.prototype.gradusp.ui.calendar.daily.DailyView
import com.prototype.gradusp.ui.calendar.monthly.MonthlyView
import com.prototype.gradusp.ui.calendar.weekly.WeeklyView
import com.prototype.gradusp.ui.components.CalendarViewSelector
import com.prototype.gradusp.ui.components.ExpandableFab
import com.prototype.gradusp.ui.components.dialogs.AddEventDialog
import com.prototype.gradusp.ui.components.dialogs.AddLectureDialog
import com.prototype.gradusp.ui.components.dialogs.DayDetailsDialog
import com.prototype.gradusp.ui.components.dialogs.EventDetailsDialog
import com.prototype.gradusp.ui.theme.GRADUSPTheme
import com.prototype.gradusp.utils.DateTimeUtils
import com.prototype.gradusp.utils.EventProcessingUtil
import com.prototype.gradusp.viewmodel.CalendarViewModel
import java.time.LocalDate
import java.time.YearMonth

enum class CalendarView(val title: String, val contentDescription: String) {
    DAILY("Diário", "Visualização diária do calendário"),
    WEEKLY("Semanal", "Visualização semanal do calendário"),
    MONTHLY("Mensal", "Visualização mensal do calendário")
}

@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    CalendarScreenContent(
        uiState = uiState,
        onViewSelected = viewModel::onViewSelected,
        onPreviousDay = viewModel::onPreviousDay,
        onNextDay = viewModel::onNextDay,
        onPreviousMonth = viewModel::onPreviousMonth,
        onNextMonth = viewModel::onNextMonth,
        onTodayClick = viewModel::onTodayClick,
        onEventClick = viewModel::onEventClick,
        onDayClick = viewModel::onDayClick,
        onAddEventClick = viewModel::onAddEventClick,
        onAddLectureClick = viewModel::onAddLectureClick,
        onDialogDismiss = viewModel::onDialogDismiss,
        onAddEvent = viewModel::addEvent,
        onUpdateEvent = viewModel::updateEvent,
        onDeleteEvent = viewModel::deleteEvent,
        onAddLecture = viewModel::addLectureEvent
    )
}

@Composable
private fun CalendarScreenContent(
    uiState: CalendarUiState,
    onViewSelected: (CalendarView) -> Unit,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onTodayClick: () -> Unit,
    onEventClick: (Event) -> Unit,
    onDayClick: (LocalDate) -> Unit,
    onAddEventClick: () -> Unit,
    onAddLectureClick: () -> Unit,
    onDialogDismiss: () -> Unit,
    onAddEvent: (Event) -> Unit,
    onUpdateEvent: (Event) -> Unit,
    onDeleteEvent: (Event) -> Unit,
    onAddLecture: (Lecture, Classroom) -> Unit
) {

    val dragModifier = Modifier.pointerInput(uiState.invertSwipeDirection) {
        var totalDragAmount = 0f
        var hasSwiped = false
        var lastSwipeTime = 0L
        val swipeCooldown = 500 // ms
        val swipeThreshold = 100f

        detectHorizontalDragGestures(
            onDragStart = {
                totalDragAmount = 0f
                hasSwiped = false
            },
            onDragEnd = { totalDragAmount = 0f },
            onDragCancel = { totalDragAmount = 0f },
            onHorizontalDrag = { _, dragAmount ->
                if (hasSwiped) return@detectHorizontalDragGestures

                totalDragAmount += dragAmount
                val currentTime = System.currentTimeMillis()

                if (currentTime - lastSwipeTime > swipeCooldown && kotlin.math.abs(totalDragAmount) > swipeThreshold) {
                    val direction = if (totalDragAmount > 0) -1 else 1
                    val effectiveDirection = if (uiState.invertSwipeDirection) -direction else direction

                    val currentIndex = uiState.selectedView.ordinal
                    val newIndex = currentIndex + effectiveDirection

                    // Only proceed if the new index is within valid bounds
                    if (newIndex in 0 until CalendarView.entries.size) {
                        onViewSelected(CalendarView.entries[newIndex])
                        lastSwipeTime = currentTime
                        hasSwiped = true
                    }

                    totalDragAmount = 0f
                }
            }
        )
    }


    Column(modifier = Modifier.fillMaxSize()) {
        CalendarViewSelector(
            selectedView = uiState.selectedView,
            onViewSelected = onViewSelected
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(dragModifier)
        ) {
            AnimatedContent(
                targetState = uiState.selectedView,
                label = "Calendar View Animation",
                transitionSpec = {
                    val isForwardTransition = targetState.ordinal > initialState.ordinal
                    val effectiveForward = if (uiState.invertSwipeDirection) !isForwardTransition else isForwardTransition

                    val slideInDirection = if (effectiveForward) 1 else -1
                    val slideOutDirection = if (effectiveForward) -1 else 1

                    val offsetAnimationSpec = tween<IntOffset>(
                        durationMillis = uiState.animationSpeed.transition,
                        easing = FastOutSlowInEasing
                    )
                    val fadeAnimationSpec = tween<Float>(
                        durationMillis = uiState.animationSpeed.transition,
                        easing = FastOutSlowInEasing
                    )

                    slideInHorizontally(animationSpec = offsetAnimationSpec) { fullWidth -> slideInDirection * fullWidth } + fadeIn(fadeAnimationSpec) togetherWith
                            slideOutHorizontally(animationSpec = offsetAnimationSpec) { fullWidth -> slideOutDirection * fullWidth } + fadeOut(fadeAnimationSpec)
                }
            ) { targetView ->
                when (targetView) {
                    CalendarView.DAILY -> DailyView(
                        selectedDate = uiState.selectedDate,
                        timeBlocks = uiState.dailyViewTimeBlocks,
                        onPreviousDay = onPreviousDay,
                        onNextDay = onNextDay,
                        onTodayClick = onTodayClick,
                        onEventClick = onEventClick
                    )
                    CalendarView.WEEKLY -> WeeklyView(
                        events = uiState.events,
                        onEventClick = onEventClick
                    )
                    CalendarView.MONTHLY -> MonthlyView(
                        selectedMonth = uiState.selectedMonth,
                        daysInGrid = uiState.daysInMonthGrid,
                        events = uiState.events,
                        onPreviousMonth = onPreviousMonth,
                        onNextMonth = onNextMonth,
                        onTodayClick = onTodayClick,
                        onDayClick = onDayClick
                    )
                }
            }

            ExpandableFab(
                onAddEvent = onAddEventClick,
                onAddLecture = onAddLectureClick
            )
        }
    }

    // --- Dialogs ---

    if (uiState.showAddEventDialog) {
        AddEventDialog(
            onDismiss = onDialogDismiss,
            onEventAdded = onAddEvent
        )
    }

    if (uiState.showAddLectureDialog) {
        AddLectureDialog(
            onDismiss = onDialogDismiss,
            onLectureSelected = onAddLecture
        )
    }

    uiState.eventForDetailsDialog?.let { event ->
        EventDetailsDialog(
            event = event,
            onDismiss = onDialogDismiss,
            onSave = onUpdateEvent,
            onDelete = onDeleteEvent
        )
    }

    uiState.dateForDetailsDialog?.let { date ->
        DayDetailsDialog(
            date = date,
            events = EventProcessingUtil.getEventsForDate(date, uiState.events),
            onDismiss = onDialogDismiss,
            onEventClick = { event -> onEventClick(event) }
        )
    }
}


@Preview(showBackground = true, name = "Calendar Screen - Weekly View")
@Composable
fun CalendarScreenPreview_Weekly() {
    GRADUSPTheme {
        CalendarScreenContent(
            uiState = CalendarUiState(
                selectedView = CalendarView.WEEKLY,
                events = sampleEvents
            ),
            onViewSelected = {}, onPreviousDay = {}, onNextDay = {},
            onPreviousMonth = {}, onNextMonth = {}, onTodayClick = {},
            onEventClick = {}, onDayClick = {}, onAddEventClick = {},
            onAddLectureClick = {}, onDialogDismiss = {}, onAddEvent = {},
            onUpdateEvent = {}, onDeleteEvent = {}, onAddLecture = { _, _ ->}
        )
    }
}

@Preview(showBackground = true, name = "Calendar Screen - Monthly View")
@Composable
fun CalendarScreenPreview_Monthly() {
    val month = YearMonth.of(2023, 10)
    GRADUSPTheme {
        CalendarScreenContent(
            uiState = CalendarUiState(
                selectedView = CalendarView.MONTHLY,
                selectedMonth = month,
                daysInMonthGrid = DateTimeUtils.getDatesForMonthGrid(month),
                events = sampleEvents
            ),
            onViewSelected = {}, onPreviousDay = {}, onNextDay = {},
            onPreviousMonth = {}, onNextMonth = {}, onTodayClick = {},
            onEventClick = {}, onDayClick = {}, onAddEventClick = {},
            onAddLectureClick = {}, onDialogDismiss = {}, onAddEvent = {},
            onUpdateEvent = {}, onDeleteEvent = {}, onAddLecture = { _, _ ->}
        )
    }
}