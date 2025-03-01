package com.prototype.gradusp.ui.calendar.daily

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prototype.gradusp.data.model.Event
import com.prototype.gradusp.ui.components.EventCard
import java.time.format.DateTimeFormatter


@Composable
fun TimeBlockDisplay(
    timeBlock: TimeBlock,
    onClick: (Event) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Hour indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Format time range for the block
            val startTime = String.format("%02d:00", timeBlock.startHour) +
                    if (timeBlock.startHour < 12) " AM" else " PM"

            Text(
                text = startTime,
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

        // Events in this time block
        if (timeBlock.events.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp)
            ) {
                timeBlock.events.forEach { (event, occurrence) ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        // Create a formatted time string for this occurrence
                        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
                        val timeText = "${occurrence.startTime.format(timeFormatter)} - ${occurrence.endTime.format(timeFormatter)}"

                        // Display event card with the specific occurrence
                        EventCard(
                            event = event.copy(
                                occurrences = listOf(occurrence)
                            ),
                            onClick = { onClick(event) }
                        )
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
