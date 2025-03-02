package com.prototype.gradusp.ui.calendar

import android.os.Build
import androidx.annotation.RequiresApi
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
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.prototype.gradusp.data.AnimationSpeed
import com.prototype.gradusp.ui.calendar.daily.DailyView
import com.prototype.gradusp.ui.calendar.monthly.MonthlyView
import com.prototype.gradusp.ui.calendar.weekly.WeeklyView
import com.prototype.gradusp.ui.components.CalendarViewSelector
import com.prototype.gradusp.ui.components.ExpandableFab
import com.prototype.gradusp.ui.components.dialogs.AddEventDialog
import com.prototype.gradusp.ui.components.dialogs.AddLectureDialog
import com.prototype.gradusp.viewmodel.CalendarViewModel
import com.prototype.gradusp.viewmodel.SettingsViewModel

enum class CalendarView(val title: String, val contentDescription: String) {
    DAILY("Diário", "Visualização diária do calendário"),
    WEEKLY("Semanal", "Visualização semanal do calendário"),
    MONTHLY("Mensal", "Visualização mensal do calendário")
}

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    navController: NavController,
    calendarViewModel: CalendarViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    var selectedView by remember { mutableStateOf(CalendarView.WEEKLY) }
    var showAddEventDialog by remember { mutableStateOf(false) }
    var showAddLectureDialog by remember { mutableStateOf(false) }

    // Observe settings for animation configuration
    val animationSpeed by settingsViewModel.animationSpeed.collectAsState(initial = AnimationSpeed.MEDIUM)
    val invertSwipeDirection by settingsViewModel.invertSwipeDirection.collectAsState(initial = false)

    // Track drag gesture for view switching
    val dragModifier = Modifier.pointerInput(invertSwipeDirection) {
        var totalDragAmount = 0f
        var lastSwipeTime = 0L
        val swipeCooldown = 500 // Cooldown in ms to prevent accidental multi-swipes

        detectHorizontalDragGestures(
            onDragStart = {
                totalDragAmount = 0f
            },
            onDragEnd = {
                // Reset total drag amount when drag ends
                totalDragAmount = 0f
            },
            onDragCancel = {
                // Reset total drag amount when drag is canceled
                totalDragAmount = 0f
            },
            onHorizontalDrag = { _, dragAmount ->
                val effectiveDragAmount = if (invertSwipeDirection) -dragAmount else dragAmount

                // Accumulate total drag distance for this gesture
                totalDragAmount += dragAmount

                // Require a more significant swipe (increased threshold)
                val swipeThreshold = 40f // Increased from 5 to 40

                val currentTime = System.currentTimeMillis()
                val timeSinceLastSwipe = currentTime - lastSwipeTime

                // Only process swipe if we've passed the threshold and cooldown period
                if (Math.abs(totalDragAmount) > swipeThreshold && timeSinceLastSwipe > swipeCooldown) {
                    // Determine swipe direction
                    val dragDirection = if (totalDragAmount > 0) -1 else 1
                    val effectiveDirection = if (invertSwipeDirection) -dragDirection else dragDirection

                    // Calculate the new view index
                    val currentIndex = selectedView.ordinal
                    val newIndex = (currentIndex + effectiveDirection).coerceIn(0, CalendarView.values().size - 1)

                    // Only update if we actually changed views
                    if (newIndex != currentIndex) {
                        selectedView = CalendarView.values()[newIndex]
                        lastSwipeTime = currentTime
                        totalDragAmount = 0f // Reset after processing
                    }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Calendário") }
                )
                CalendarViewSelector(
                    selectedView = selectedView,
                    onViewSelected = { newView ->
                        selectedView = newView
                    }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .then(dragModifier)
        ) {
            // Content area with animated transitions between views
            AnimatedContent(
                targetState = selectedView,
                transitionSpec = {
                    // Determine if we're moving forward or backward in the view sequence
                    val isForwardTransition = targetState.ordinal > initialState.ordinal

                    // Apply inversion if the setting is enabled
                    val effectiveForward = if (invertSwipeDirection) !isForwardTransition else isForwardTransition

                    // Create the animation with the appropriate direction
                    val slideInDirection = if (effectiveForward) 1 else -1
                    val slideOutDirection = if (effectiveForward) -1 else 1

                    // Animation setup with custom durations
                    slideInHorizontally(
                        animationSpec = tween(animationSpeed.transition, easing = FastOutSlowInEasing)
                    ) { fullWidth -> slideInDirection * fullWidth } +
                            fadeIn(tween(animationSpeed.transition)) togetherWith
                            slideOutHorizontally(
                                animationSpec = tween(animationSpeed.transition, easing = FastOutSlowInEasing)
                            ) { fullWidth -> slideOutDirection * fullWidth } +
                            fadeOut(tween(animationSpeed.transition))
                }
            ) { targetView ->
                when (targetView) {
                    CalendarView.DAILY -> DailyView(calendarViewModel)
                    CalendarView.WEEKLY -> WeeklyView(calendarViewModel)
                    CalendarView.MONTHLY -> MonthlyView(calendarViewModel)
                }
            }

            ExpandableFab(
                onAddEvent = { showAddEventDialog = true },
                onAddLecture = { showAddLectureDialog = true }
            )
        }
    }

    // Add Event Dialog
    if (showAddEventDialog) {
        AddEventDialog(
            onDismiss = { showAddEventDialog = false },
            onEventAdded = { event ->
                calendarViewModel.addEvent(event)
                showAddEventDialog = false
            }
        )
    }

    // Add Lecture Dialog
    if (showAddLectureDialog) {
        AddLectureDialog(
            onDismiss = { showAddLectureDialog = false },
            onLectureSelected = { lecture, selectedClassroom ->
                calendarViewModel.addLectureEvent(lecture, selectedClassroom)
                showAddLectureDialog = false
            }
        )
    }
}
