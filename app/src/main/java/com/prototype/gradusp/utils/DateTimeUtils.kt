package com.prototype.gradusp.utils

import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Utility functions for date and time operations
 */
object DateTimeUtils {
    private val ptBrLocale = Locale("pt", "BR")

    private val dayMonthFormatter = DateTimeFormatter.ofPattern("dd/MM", ptBrLocale)
    private val fullDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", ptBrLocale)
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", ptBrLocale)
    private val monthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", ptBrLocale)

    /**
     * Format a date as day/month (e.g., "25/10")
     */
    fun formatDayMonth(date: LocalDate): String {
        return date.format(dayMonthFormatter)
    }

    /**
     * Format a date as day/month/year (e.g., "25/10/2023")
     */
    fun formatFullDate(date: LocalDate): String {
        return date.format(fullDateFormatter)
    }

    /**
     * Format a time as hour:minute (e.g., "14:30")
     */
    fun formatTime(time: LocalTime): String {
        return time.format(timeFormatter)
    }

    /**
     * Format a date as month year (e.g., "Outubro 2023")
     */
    fun formatMonthYear(yearMonth: YearMonth): String {
        return yearMonth.format(monthYearFormatter)
    }

    /**
     * Get the localized day name (e.g., "Segunda-feira")
     */
    fun getDayName(day: DayOfWeek, textStyle: TextStyle = TextStyle.FULL): String {
        return day.getDisplayName(textStyle, ptBrLocale)
    }

    /**
     * Get short day name (e.g., "Seg")
     */
    fun getShortDayName(day: DayOfWeek): String {
        return getDayName(day, TextStyle.SHORT)
    }

    /**
     * Get a formatted time range (e.g., "14:30 - 16:00")
     */
    fun formatTimeRange(start: LocalTime, end: LocalTime): String {
        return "${formatTime(start)} - ${formatTime(end)}"
    }

    /**
     * Check if a date is today
     */
    fun isToday(date: LocalDate): Boolean {
        return date == LocalDate.now()
    }

    /**
     * Get a list of dates for the month grid display
     */
    fun getDatesForMonthGrid(yearMonth: YearMonth): List<LocalDate?> {
        val firstDayOfMonth = yearMonth.atDay(1)
        val totalDays = yearMonth.lengthOfMonth()
        // Using ISO (Monday=1...Sunday=7), convert so Sunday becomes index 0.
        val firstDayOffset = firstDayOfMonth.dayOfWeek.value % 7
        val totalCells = firstDayOffset + totalDays
        val extra = if (totalCells % 7 == 0) 0 else 7 - (totalCells % 7)
        val cells = mutableListOf<LocalDate?>()
        repeat(firstDayOffset) { cells.add(null) }
        for (day in 1..totalDays) {
            cells.add(yearMonth.atDay(day))
        }
        repeat(extra) { cells.add(null) }
        return cells
    }
}