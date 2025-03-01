package com.prototype.gradusp.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import androidx.wear.compose.material.ExperimentalWearMaterialApi
import androidx.wear.compose.material.FractionalThreshold
import androidx.wear.compose.material.rememberSwipeableState
import androidx.wear.compose.material.swipeable
import com.prototype.gradusp.data.AnimationSpeed
import com.prototype.gradusp.data.model.Event
import com.prototype.gradusp.data.model.EventImportance
import com.prototype.gradusp.data.model.sampleEvents
import com.prototype.gradusp.ui.components.EventCard
import com.prototype.gradusp.ui.components.dialogs.DayDetailsDialog
import com.prototype.gradusp.ui.components.dialogs.EventDetailsDialog
import com.prototype.gradusp.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.ceil

enum class CalendarView { DAILY, WEEKLY, MONTHLY }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalWearMaterialApi::class)
@Composable
fun CalendarScreen(navController: NavController) {
    var selectedView by remember { mutableStateOf(CalendarView.WEEKLY) }
    var events by remember { mutableStateOf(sampleEvents) }

    val coroutineScope = rememberCoroutineScope()

    // Observe settings
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val animationSpeed by settingsViewModel.animationSpeed.collectAsState(initial = AnimationSpeed.MEDIUM)
    val invertSwipeDirection by settingsViewModel.invertSwipeDirection.collectAsState(initial = false)

    // Configure swipe behavior with user-defined animation duration
    val swappableState = rememberSwipeableState(
        initialValue = selectedView,
        animationSpec = tween(
            durationMillis = animationSpeed.swipe,
            easing = FastOutSlowInEasing
        )
    )

    val screenWidth = with(LocalConfiguration.current) { screenWidthDp.dp }
    val anchors = remember(invertSwipeDirection) {
        if (!invertSwipeDirection) {
            mapOf(
                0f to CalendarView.DAILY,
                screenWidth.value to CalendarView.WEEKLY,
                (screenWidth.value * 2) to CalendarView.MONTHLY
            )
        } else {
            // Inverted order for reversed swipe direction
            mapOf(
                0f to CalendarView.MONTHLY,
                screenWidth.value to CalendarView.WEEKLY,
                (screenWidth.value * 2) to CalendarView.DAILY
            )
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(title = { Text("Calendário") })
                CalendarViewSelector(
                    selectedView = selectedView,
                    onViewSelected = { newView ->
                        selectedView = newView
                        coroutineScope.launch {
                            swappableState.animateTo(
                                targetValue = newView,
                                anim = tween(animationSpeed.tab)
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
                    state = swappableState,
                    anchors = anchors,
                    thresholds = { _, _ -> FractionalThreshold(0.15f) },
                    orientation = Orientation.Horizontal,
                    resistance = null
                )
        ) {
            LaunchedEffect(swappableState.currentValue) {
                selectedView = swappableState.currentValue
            }

            AnimatedContent(
                targetState = selectedView,
                transitionSpec = {
                    val normalDirection = if (targetState.ordinal > initialState.ordinal)
                        AnimatedContentTransitionScope.SlideDirection.Left
                    else
                        AnimatedContentTransitionScope.SlideDirection.Right

                    // Apply inversion if the setting is enabled
                    val direction = if (invertSwipeDirection) {
                        if (normalDirection == AnimatedContentTransitionScope.SlideDirection.Left)
                            AnimatedContentTransitionScope.SlideDirection.Right
                        else
                            AnimatedContentTransitionScope.SlideDirection.Left
                    } else {
                        normalDirection
                    }

                    slideIntoContainer(direction, animationSpec = tween(animationSpeed.transition)) togetherWith
                            slideOutOfContainer(direction, animationSpec = tween(animationSpeed.transition)) using
                            SizeTransform(clip = false, sizeAnimationSpec = { _, _ -> tween(animationSpeed.transition) })
                }
            ) { targetView ->
                when (targetView) {
                    CalendarView.DAILY -> DailyView(events) { updatedEvent ->
                        events = events.map { if (it.title == updatedEvent.title) updatedEvent else it }
                    }
                    CalendarView.WEEKLY -> WeeklyView(events) { updatedEvent ->
                        events = events.map { if (it.title == updatedEvent.title) updatedEvent else it }
                    }
                    CalendarView.MONTHLY -> MonthlyView(events) { updatedEvent ->
                        events = events.map { if (it.title == updatedEvent.title) updatedEvent else it }
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarViewSelector(selectedView: CalendarView, onViewSelected: (CalendarView) -> Unit) {
    val options = CalendarView.values()
    TabRow(selectedTabIndex = options.indexOf(selectedView)) {
        options.forEach { view ->
            Tab(
                selected = selectedView == view,
                onClick = { onViewSelected(view) },
                text = { Text(view.name) }
            )
        }
    }
}

@Composable
fun DailyView(events: List<Event>, onUpdateEvent: (Event) -> Unit) {
    var selectedEvent by remember { mutableStateOf<Event?>(null) }

    // Use the current date as initial state but allow navigation
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val currentDayOfWeek = selectedDate.dayOfWeek

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header with day navigation and today button
        DailyViewHeader(
            selectedDate = selectedDate,
            onPreviousDay = { selectedDate = selectedDate.minusDays(1) },
            onNextDay = { selectedDate = selectedDate.plusDays(1) },
            onTodayClick = { selectedDate = LocalDate.now() }
        )

        // Time slots
        val startHour = 7 // 7 AM
        val endHour = 22 // 10 PM

        LazyColumn {
            items(endHour - startHour + 1) { hourOffset ->
                val hour = startHour + hourOffset
                val timeSlotEvents = events.filter { event ->
                    event.occurrences.any {
                        it.day == currentDayOfWeek &&
                                (it.startTime.hour == hour ||
                                        (it.startTime.hour < hour && it.endTime.hour > hour))
                    }
                }

                TimeSlot(
                    hour = hour,
                    events = timeSlotEvents,
                    onClick = { selectedEvent = it }
                )
            }
        }
    }

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
fun DailyViewHeader(
    selectedDate: LocalDate,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onTodayClick: () -> Unit
) {
    val isToday = selectedDate.equals(LocalDate.now())

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPreviousDay) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowLeft,
                    contentDescription = "Dia anterior"
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = selectedDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("pt", "BR")),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onNextDay) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Próximo dia"
                )
            }
        }

        // Today button
        AnimatedVisibility(visible = !isToday) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                OutlinedButton(
                    onClick = onTodayClick,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Hoje",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Hoje")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun TimeSlot(hour: Int, events: List<Event>, onClick: (Event) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Hour indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hour text with AM/PM indicator
            Text(
                text = String.format("%02d:00", hour) + if (hour < 12) " AM" else " PM",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )

            HorizontalDivider(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            )
        }

        // Events at this hour
        if (events.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp) // Indentation to align with hour markers
            ) {
                events.forEach { event ->
                    val occurrence = event.occurrences.firstOrNull { it.startTime.hour <= hour && it.endTime.hour > hour }
                    occurrence?.let {
                        // Show event with time information
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            EventCard(
                                event = event.copy(
                                    occurrences = listOf(occurrence)
                                ),
                                onClick = { onClick(event) }
                            )
                        }
                    }
                }
            }
        } else {
            // Empty time slot indicator
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .padding(start = 24.dp)
            ) {
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                )
            }
        }
    }
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
                    text = day.getDisplayName(TextStyle.FULL, Locale("pt", "BR")),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp, start = 4.dp)
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
                        text = "Sem eventos",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 12.dp)
                    )
                }

                if (day != daysOfWeek.last()) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
        }
    }

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