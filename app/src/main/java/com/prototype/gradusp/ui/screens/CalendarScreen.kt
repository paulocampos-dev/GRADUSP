package com.prototype.gradusp.ui.screens

import android.inputmethodservice.Keyboard.Row
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalWearMaterialApi::class, ExperimentalAnimationApi::class)
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
                    imageVector = Icons.Default.KeyboardArrowRight,
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

            Divider(
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
                Divider(
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
fun MonthlyView(events: List<Event>, onUpdateEvent: (Event) -> Unit) {
    var selectedEvent by remember { mutableStateOf<Event?>(null) }
    val currentMonth = remember { YearMonth.now() }
    var selectedMonth by remember { mutableStateOf(currentMonth) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        // Month navigation header with today button
        MonthNavigationHeader(
            currentMonth = selectedMonth,
            onPreviousMonth = { selectedMonth = selectedMonth.minusMonths(1) },
            onNextMonth = { selectedMonth = selectedMonth.plusMonths(1) },
            onTodayClick = { selectedMonth = YearMonth.now() }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Days of week header
        WeekdaysHeader()

        // Calendar grid
        CalendarGrid(
            yearMonth = selectedMonth,
            events = events,
            onEventClick = { selectedEvent = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // List of events for the selected month
        val monthEvents = events.filter { event ->
            event.occurrences.any { occurrence ->
                // In a real app, you'd filter by the actual date
                // Here we're just showing all events as example
                true
            }
        }

        Text(
            text = "Eventos deste mês",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(monthEvents) { event ->
                EventCard(
                    event = event,
                    onClick = { selectedEvent = event }
                )
            }

            if (monthEvents.isEmpty()) {
                item {
                    Text(
                        text = "Não há eventos para este mês",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        modifier = Modifier
                            .padding(vertical = 16.dp)
                            .fillMaxWidth(),
                        textAlign = TextAlign.Center
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


@Composable
fun MonthNavigationHeader(
    currentMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onTodayClick: () -> Unit = {}
) {
    val isCurrentMonth = currentMonth.equals(YearMonth.now())

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPreviousMonth) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowLeft,
                    contentDescription = "Mês anterior"
                )
            }

            Text(
                text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale("pt", "BR"))),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            IconButton(onClick = onNextMonth) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "Próximo mês"
                )
            }
        }

        // Today button - only show if not on current month
        AnimatedVisibility(visible = !isCurrentMonth) {
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
                        contentDescription = "Mês atual",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Hoje")
                }
            }
        }
    }
}


@Composable
fun WeekdaysHeader() {
    Row(modifier = Modifier.fillMaxWidth()) {
        val daysOfWeek = DayOfWeek.values()

        for (dayOfWeek in daysOfWeek) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("pt", "BR")),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun CalendarGrid(
    yearMonth: YearMonth,
    events: List<Event>,
    onEventClick: (Event) -> Unit
) {
    val firstDayOfMonth = yearMonth.atDay(1)
    val lastDayOfMonth = yearMonth.atEndOfMonth()
    val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7 // Adjust for Sunday as first day
    val totalDays = lastDayOfMonth.dayOfMonth
    val totalCells = firstDayOfWeek + totalDays
    val totalRows = ceil(totalCells / 7.0).toInt()

    Column(modifier = Modifier.fillMaxWidth()) {
        for (row in 0 until totalRows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (column in 0 until 7) {
                    val cellIndex = row * 7 + column
                    val dayOfMonth = cellIndex - firstDayOfWeek + 1

                    if (dayOfMonth in 1..totalDays) {
                        val date = yearMonth.atDay(dayOfMonth)
                        val dayOfWeek = date.dayOfWeek

                        // Get events for this specific day
                        val dayEvents = events.filter { event ->
                            event.occurrences.any { it.day == dayOfWeek }
                        }

                        val hasEvents = dayEvents.isNotEmpty()

                        CalendarDay(
                            day = dayOfMonth,
                            isToday = date.equals(LocalDate.now()),
                            hasEvents = hasEvents,
                            events = dayEvents,
                            onClick = {
                                if (hasEvents) {
                                    onEventClick(dayEvents.first())
                                }
                            }
                        )
                    } else {
                        // Empty cell
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun RowScope.CalendarDay(
    day: Int,
    isToday: Boolean,
    hasEvents: Boolean,
    events: List<Event> = emptyList(),
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .aspectRatio(1f)
            .padding(2.dp)
            .background(
                color = if (isToday) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                shape = RoundedCornerShape(4.dp)
            )
            .border(
                width = if (isToday) 2.dp else 1.dp,
                color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(4.dp)
            )
            .clickable(enabled = hasEvents, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(2.dp)
        ) {
            // Day number
            Text(
                text = day.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                color = if (isToday) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(1.dp))

            // Event indicators
            if (events.isNotEmpty()) {
                DayEventIndicators(events = events)
            }
        }
    }
}


@Composable
fun MiniEventIndicator(
    event: Event,
    modifier: Modifier = Modifier
) {
    // Determine importance level for visual emphasis
    val borderWidth = when (event.importance) {
        EventImportance.HIGH -> 0.7.dp
        EventImportance.NORMAL -> 0.5.dp
        EventImportance.LOW -> 0.dp
    }

    // Create a mini indicator with the event's color
    Box(
        modifier = modifier
            .size(width = 10.dp, height = 4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(event.color)
            .border(
                width = borderWidth,
                color = Color.White.copy(alpha = 0.7f),
                shape = RoundedCornerShape(2.dp)
            )
    )
}

@Composable
fun DayEventIndicators(
    events: List<Event>,
    modifier: Modifier = Modifier
) {
    if (events.isEmpty()) return

    // Group by color to avoid duplicates
    val eventsGrouped = events.groupBy { it.color }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(top = 2.dp)
    ) {
        // Show up to 3 mini bars representing events
        val displayEvents = eventsGrouped.entries.take(3)
        val hasMoreEvents = eventsGrouped.size > 3

        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(vertical = 1.dp)
        ) {
            displayEvents.forEach { (_, eventsWithColor) ->
                val event = eventsWithColor.maxByOrNull { it.importance.ordinal } ?: eventsWithColor.first()
                MiniEventIndicator(
                    event = event,
                    modifier = Modifier.padding(horizontal = 1.dp)
                )
            }
        }

        // If there are more events, show a count
        if (hasMoreEvents) {
            Text(
                text = "+${eventsGrouped.size - 3}",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 8.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewCalendarScreen() {
    val navController = rememberNavController()
    CalendarScreen(navController)
}