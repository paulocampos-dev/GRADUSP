package com.prototype.gradusp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.prototype.gradusp.data.model.Event
import com.prototype.gradusp.ui.theme.GraduspShapes
import com.prototype.gradusp.ui.theme.GraduspSpacing
import com.prototype.gradusp.utils.AccessibilityUtils
import com.prototype.gradusp.utils.DateTimeUtils

@Composable
fun EventCard(event: Event, onClick: () -> Unit) {
    // Create accessibility description
    val firstOccurrence = event.occurrences.firstOrNull()
    val accessibilityText = firstOccurrence?.let {
        val dayName = DateTimeUtils.getDayName(it.day)
        val timeRange = DateTimeUtils.formatTimeRange(it.startTime, it.endTime)
        AccessibilityUtils.getEventAccessibilityDescription(
            title = event.title,
            location = event.location,
            time = "$dayName, $timeRange"
        )
    } ?: event.title

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 2.dp)
            .clickable { onClick() }
            .semantics { contentDescription = accessibilityText },
        shape = GraduspShapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 2.dp)
        ) {
            // Color bar
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .height(100.dp)
                    .background(event.color)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(GraduspSpacing.md)
            ) {
                // Title
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Spacer(modifier = Modifier.height(GraduspSpacing.xs))

                // Times
                event.occurrences.take(2).forEach { occurrence ->
                    val dayName = DateTimeUtils.getShortDayName(occurrence.day)
                    val timeRange = DateTimeUtils.formatTimeRange(occurrence.startTime, occurrence.endTime)

                    Text(
                        text = "$dayName $timeRange",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (event.occurrences.size > 2) {
                    Text(
                        text = "... e ${event.occurrences.size - 2} mais",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(GraduspSpacing.xs))

                // Additional info row
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Teacher
                    event.teacher?.let {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = GraduspSpacing.md)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Professor",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )

                            Spacer(modifier = Modifier.width(GraduspSpacing.xs))

                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Location
                    event.location?.let {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Local",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )

                            Spacer(modifier = Modifier.width(GraduspSpacing.xs))

                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}