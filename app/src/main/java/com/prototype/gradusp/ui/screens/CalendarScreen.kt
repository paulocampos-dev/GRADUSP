package com.prototype.gradusp.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.Orientation
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.wear.compose.material.ExperimentalWearMaterialApi
import androidx.wear.compose.material.FractionalThreshold
import androidx.wear.compose.material.rememberSwipeableState
import androidx.wear.compose.material.swipeable
import com.prototype.gradusp.data.AnimationSpeed
import com.prototype.gradusp.data.model.sampleEvents
import com.prototype.gradusp.ui.screens.views.DailyView
import com.prototype.gradusp.ui.screens.views.MonthlyView
import com.prototype.gradusp.ui.screens.views.WeeklyView
import com.prototype.gradusp.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

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
                    val direction = if (targetState.ordinal > initialState.ordinal)
                        AnimatedContentTransitionScope.SlideDirection.Left
                    else
                        AnimatedContentTransitionScope.SlideDirection.Right

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

