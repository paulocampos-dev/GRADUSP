package com.prototype.gradusp.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.animation.with
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import androidx.wear.compose.material.ExperimentalWearMaterialApi
import androidx.wear.compose.material.FractionalThreshold
import androidx.wear.compose.material.rememberSwipeableState
import androidx.wear.compose.material.swipeable
import com.prototype.gradusp.data.model.Event
import com.prototype.gradusp.data.model.EventOccurrence
import com.prototype.gradusp.data.model.sampleEvents
import com.prototype.gradusp.ui.components.EventCard
import com.prototype.gradusp.ui.components.EventList
import com.prototype.gradusp.ui.components.dialogs.EventDetailsDialog
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime

enum class CalendarView {DAILY, WEEKLY, MONTHLY}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalWearMaterialApi::class,
    ExperimentalAnimationApi::class
)
@Composable
fun CalendarScreen(navController: NavController) {

    var selectedView by remember { mutableStateOf(CalendarView.WEEKLY) }
    var events by remember { mutableStateOf(sampleEvents) }

    val scope = rememberCoroutineScope()

    // Configure swipe behavior with faster animations
    val swipeableState = rememberSwipeableState(
        initialValue = selectedView,
        animationSpec = tween(
            durationMillis = 100,
            easing = FastOutSlowInEasing
        )
    )

    // Calculate swipe anchors with lower resistance
    val screenWidth = with(LocalConfiguration.current) { screenWidthDp.dp }
    val anchors = remember {
        mapOf(
            0f to CalendarView.DAILY,
            screenWidth.value to CalendarView.WEEKLY,
            (screenWidth.value * 2) to CalendarView.MONTHLY
        )
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(title = { Text("Calendário") })
                CalendarViewSelector(
                    selectedView = selectedView,
                    onViewSelected = { newView ->
                        selectedView = newView
                        scope.launch {
                            swipeableState.animateTo(
                                targetValue = newView,
                                anim = tween(80) // Faster tab selection animation
                            )
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* Open event creation dialog */ }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar Evento")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .swipeable(
                    state = swipeableState,
                    anchors = anchors,
                    thresholds = { _, _ -> FractionalThreshold(0.15f) }, // More responsive threshold
                    orientation = Orientation.Horizontal,
                    resistance = null // Remove resistance for faster swipes
                )
        ) {
            LaunchedEffect(swipeableState.currentValue) {
                selectedView = swipeableState.currentValue
            }

            AnimatedContent(
                targetState = selectedView,
                transitionSpec = {
                    val direction = if (
                        targetState.ordinal > initialState.ordinal
                    ) AnimatedContentTransitionScope.SlideDirection.Left
                    else AnimatedContentTransitionScope.SlideDirection.Right

                    slideIntoContainer(direction, animationSpec = tween(120)) togetherWith
                            slideOutOfContainer(direction, animationSpec = tween(120)) using
                            SizeTransform(clip = false, sizeAnimationSpec = {_, _ -> tween(120)})
                }
            ) { targetView ->
                when (targetView) {
                    CalendarView.DAILY -> DailyView()
                    CalendarView.WEEKLY -> WeeklyView(events) { updatedEvent ->
                        events = events.map { if (it.title == updatedEvent.title) updatedEvent else it }
                    }
                    CalendarView.MONTHLY -> MonthlyView()
                }
            }
        }
    }
}


@Composable
fun CalendarViewSelector(
    selectedView: CalendarView,
    onViewSelected: (CalendarView) -> Unit,
) {
    val options = CalendarView.values()

    TabRow(selectedTabIndex = options.indexOf(selectedView)) {
        options.forEachIndexed{ _, view ->
            Tab(
                selected = selectedView == view,
                onClick = { onViewSelected(view) },
                text = { Text(view.name) }
            )
        }
    }
}

@Composable
fun DailyView() {
//    EventList(events = sampleEvents.filter { it.date == LocalDate.now() })
}

@Composable
fun WeeklyView(events: List<Event>, onUpdateEvent: (Event) -> Unit) {
    val daysOfWeek = DayOfWeek.values().toList()
    var selectedEvent by remember { mutableStateOf<Event?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        items(daysOfWeek) { day ->
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = day.name,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 8.dp)
                )

                val dayEvents = events.filter { event ->
                    event.occurrences.any { it.day == day }
                }

                if (dayEvents.isNotEmpty()) {
                    dayEvents.forEach { event ->
                        val relevantOccurrences = event.occurrences.filter { it.day == day }
                        if (relevantOccurrences.isNotEmpty()) {
                            EventCard(
                                event = event.copy(occurrences = relevantOccurrences),
                                onClick = { selectedEvent = event }
                            )
                        }
                    }
                } else {
                    Text(
                        text = "No events",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
            }
        }
    }

    // Handle dialog outside the LazyColumn
    selectedEvent?.let { event ->
        EventDetailsDialog(
            event = event,
            onDismiss = { selectedEvent = null },
            onSave = { updatedEvent ->
                onUpdateEvent(updatedEvent)
                selectedEvent = null
            }
        )
    }
}

@Composable
fun MonthlyView() {
//    EventList(events = sampleEvents.filter { it.date.month == LocalDate.now().month })
}

@Preview(showBackground = true)
@Composable
fun PreviewCalendarScreen() {
    val navController = rememberNavController()
    CalendarScreen(navController)
}