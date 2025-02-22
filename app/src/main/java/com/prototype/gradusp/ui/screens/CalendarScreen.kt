package com.prototype.gradusp.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.prototype.gradusp.data.model.sampleEvents
import com.prototype.gradusp.ui.components.EventCard
import java.time.DayOfWeek

enum class CalendarView {DAILY, WEEKLY, MONTHLY}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    navController: NavController
) {
    var selectedView by remember { mutableStateOf(CalendarView.WEEKLY) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Calendário") }
                )
                CalendarViewSelector(
                    selectedView = selectedView,
                    onViewSelected = { selectedView = it }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {/* Lógica para adicionar um novo evento */}
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar Evento")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Text(text = "Calendar Screen")
            when(selectedView) {
                CalendarView.DAILY -> DailyView()
                CalendarView.WEEKLY -> WeeklyView()
                CalendarView.MONTHLY -> MonthlyView()
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
fun WeeklyView() {
    val daysOfWeek = DayOfWeek.values().toList()
    val eventsByDay = daysOfWeek.associateWith { day ->
        sampleEvents.filter { event -> event.occurrences.any { it.day == day } }
    }

    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        daysOfWeek.forEach { day ->
            val dayEvents = eventsByDay[day] ?: emptyList()
            if (dayEvents.isNotEmpty()) {
                Text(text = day.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))

                dayEvents.forEach { event ->
                    event.occurrences.filter { it.day == day }.forEach { occurrence ->
                        EventCard(event.title, occurrence.startTime, occurrence.endTime, event.color)
                    }
                }
            }
        }
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
