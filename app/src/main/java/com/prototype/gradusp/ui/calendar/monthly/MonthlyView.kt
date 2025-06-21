package com.prototype.gradusp.ui.calendar.monthly

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.prototype.gradusp.data.model.Event
import com.prototype.gradusp.utils.EventProcessingUtil
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun MonthlyView(
    selectedMonth: YearMonth,
    daysInGrid: List<LocalDate?>,
    events: List<Event>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onTodayClick: () -> Unit,
    onDayClick: (LocalDate) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(6.dp)
    ) {
        MonthNavigationHeader(
            currentMonth = selectedMonth,
            onPreviousMonth = onPreviousMonth,
            onNextMonth = onNextMonth,
            onTodayClick = onTodayClick
        )

        WeekdaysHeader()

        Spacer(modifier = Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            contentPadding = PaddingValues(4.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(daysInGrid) { date ->
                if (date != null) {
                    val dayEvents = remember(events, date) {
                        EventProcessingUtil.getEventsForDate(date, events)
                    }

                    DayCell(
                        date = date,
                        dayEvents = dayEvents,
                        onClick = { onDayClick(date) }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .fillMaxWidth()
                            .height(100.dp)
                    )
                }
            }
        }
    }
}