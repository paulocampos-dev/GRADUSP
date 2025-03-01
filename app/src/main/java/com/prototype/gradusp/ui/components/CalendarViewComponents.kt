package com.prototype.gradusp.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prototype.gradusp.ui.calendar.CalendarView

@Composable
fun CalendarViewSelector(
    selectedView: CalendarView,
    onViewSelected: (CalendarView) -> Unit
) {
    val options = CalendarView.values()

    TabRow(
        selectedTabIndex = options.indexOf(selectedView),
        modifier = Modifier.semantics { contentDescription = "Opções de visualização de calendário" }
    ) {
        options.forEach { view ->
            Tab(
                selected = selectedView == view,
                onClick = { onViewSelected(view) },
                modifier = Modifier.semantics {
                    contentDescription = view.contentDescription + if (selectedView == view) ", selecionado" else ""
                },
                text = {
                    Text(
                        text = view.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (selectedView == view) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            )
        }
    }
}