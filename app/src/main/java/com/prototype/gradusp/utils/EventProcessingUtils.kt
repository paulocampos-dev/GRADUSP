package com.prototype.gradusp.utils

import com.prototype.gradusp.data.model.Event
import com.prototype.gradusp.ui.calendar.daily.TimeBlock
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Utility class for event processing operations
 */
object EventProcessingUtil {

    /**
     * Process events into time blocks for display in the daily view
     */
    fun processEventsIntoTimeBlocks(
        events: List<Event>,
        dayOfWeek: DayOfWeek,
        startHour: Int = 7,
        endHour: Int = 22
    ): List<TimeBlock> {
        // Get all events for this day with their occurrences
        val eventsWithOccurrences = events.flatMap { event ->
            event.occurrences
                .filter { it.day == dayOfWeek }
                .map { occurrence -> Pair(event, occurrence) }
        }

        if (eventsWithOccurrences.isEmpty()) {
            return emptyList()
        }

        // Find the actual time range needed for events
        val firstEventHour = eventsWithOccurrences.minOf { (_, occurrence) ->
            occurrence.startTime.hour
        }

        val lastEventHour = eventsWithOccurrences.maxOf { (_, occurrence) ->
            occurrence.endTime.hour
        }

        // Use either the specified range or the actual event range, whichever is more inclusive
        val effectiveStartHour = maxOf(startHour, firstEventHour)
        val effectiveEndHour = lastEventHour.coerceAtLeast(endHour).coerceAtMost(23)

        val timeBlocks = mutableListOf<TimeBlock>()

        // Create a time block for each hour in our display range
        for (hour in effectiveStartHour..effectiveEndHour) {
            // Get events that are active during this hour
            val eventsAtThisHour = eventsWithOccurrences.filter { (_, occurrence) ->
                val eventStart = occurrence.startTime.hour
                val eventEnd = occurrence.endTime.hour

                // Event is active if it starts at or before this hour and ends after this hour
                (eventStart <= hour && eventEnd > hour) ||
                        // Or if it starts during this hour
                        (eventStart == hour)
            }

            if (eventsAtThisHour.isNotEmpty()) {
                // Only create a new block if we don't have one already for these events
                val existingBlock = timeBlocks.find { block ->
                    block.events.all { it in eventsAtThisHour } &&
                            eventsAtThisHour.all { it in block.events } &&
                            block.endHour == hour
                }

                if (existingBlock != null) {
                    // Extend existing block if it contains exactly the same events
                    val index = timeBlocks.indexOf(existingBlock)
                    timeBlocks[index] = existingBlock.copy(endHour = hour + 1)
                } else {
                    // Create a new block
                    timeBlocks.add(TimeBlock(hour, hour + 1, eventsAtThisHour))
                }
            } else {
                // For empty hours, only create a block if it's between events
                // This avoids creating empty blocks at the beginning and end of the day
                if (hour in effectiveStartHour..effectiveEndHour &&
                    (timeBlocks.isNotEmpty() || eventsWithOccurrences.any { (_, o) -> o.startTime.hour > hour })) {
                    timeBlocks.add(TimeBlock(hour, hour + 1, emptyList()))
                }
            }
        }

        // Merge adjacent blocks with the same events
        return mergeAdjacentBlocks(timeBlocks)
    }

    /**
     * Merge adjacent time blocks that contain the same events
     */
    private fun mergeAdjacentBlocks(blocks: List<TimeBlock>): List<TimeBlock> {
        val result = mutableListOf<TimeBlock>()
        var currentBlock: TimeBlock? = null

        for (block in blocks) {
            if (currentBlock == null) {
                currentBlock = block
            } else if (currentBlock.endHour == block.startHour &&
                currentBlock.events.size == block.events.size &&
                currentBlock.events.containsAll(block.events)) {
                // Merge blocks if they are adjacent and have the same events
                currentBlock = currentBlock.copy(endHour = block.endHour)
            } else {
                result.add(currentBlock)
                currentBlock = block
            }
        }

        // Add the last block if there is one
        currentBlock?.let { result.add(it) }

        return result
    }

    /**
     * Get events for a specific date
     */
    fun getEventsForDate(date: LocalDate, events: List<Event>): List<Event> {
        return events.filter { event ->
            event.occurrences.any { it.day == date.dayOfWeek }
        }
    }

    /**
     * Get events for a specific day of week
     */
    fun getEventsForDayOfWeek(dayOfWeek: DayOfWeek, events: List<Event>): List<Event> {
        return events.filter { event ->
            event.occurrences.any { it.day == dayOfWeek }
        }
    }
}