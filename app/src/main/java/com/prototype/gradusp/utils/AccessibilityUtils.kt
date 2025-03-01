package com.prototype.gradusp.utils

import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics

/**
 * Utility functions for accessibility
 */
object AccessibilityUtils {
    /**
     * Create an accessibility description for an event
     */
    fun getEventAccessibilityDescription(
        title: String,
        location: String?,
        time: String
    ): String {
        val locationText = location?.let { ", localizado em $it" } ?: ""
        return "$title$locationText, horário $time"
    }

    /**
     * Create an accessibility description for a date
     */
    fun getDateAccessibilityDescription(
        dayName: String,
        date: String,
        eventCount: Int
    ): String {
        val eventText = when (eventCount) {
            0 -> "Sem eventos"
            1 -> "1 evento"
            else -> "$eventCount eventos"
        }
        return "$dayName, $date, $eventText"
    }
}