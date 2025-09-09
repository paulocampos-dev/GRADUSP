package com.prototype.gradusp.data.model

import java.util.Locale
import java.time.LocalDate
import java.time.LocalTime
import java.time.DayOfWeek

// Data models with enhanced search and validation capabilities

data class Course(
    val code: String,
    val name: String,
    val unit: String,
    val period: String,
    val periods: Map<String, List<LectureInfo>> = mapOf(),
    val totalCredits: Int = 0,
    val duration: String = "",
    val coordinator: String = "",
    val lastUpdated: LocalDate? = null
) {
    /**
     * Checks if this course matches the search query
     */
    fun matchesSearch(searchQuery: String): Boolean {
        val normalizedQuery = searchQuery.lowercase(Locale.getDefault()).trim()
        if (normalizedQuery.isEmpty()) return true

        return code.lowercase().contains(normalizedQuery) ||
               name.lowercase().contains(normalizedQuery) ||
               unit.lowercase().contains(normalizedQuery) ||
               period.lowercase().contains(normalizedQuery) ||
               coordinator.lowercase().contains(normalizedQuery)
    }

    /**
     * Gets all lectures from all periods
     */
    fun getAllLectures(): List<LectureInfo> {
        return periods.values.flatten()
    }

    /**
     * Gets lectures for a specific period
     */
    fun getLecturesForPeriod(periodNumber: String): List<LectureInfo> {
        return periods[periodNumber] ?: emptyList()
    }

    /**
     * Calculates total credits for the course
     */
    fun calculateTotalCredits(): Int {
        // Sums the credits of all lectures across all periods.
        // Assumes that each LectureInfo object has a `credits` property.
        return periods.values.flatten().sumOf { it.credits }
    }
}

data class LectureInfo(
    val code: String,
    val type: LectureType, // Enum for better type safety
    val reqWeak: List<String> = listOf(),
    val reqStrong: List<String> = listOf(),
    val indConjunto: List<String> = listOf(),
    val credits: Int = 0,
    val workload: Int = 0
) {
    /**
     * Checks if this lecture has any prerequisites
     */
    fun hasPrerequisites(): Boolean {
        return reqWeak.isNotEmpty() || reqStrong.isNotEmpty()
    }

    /**
     * Gets all prerequisite codes
     */
    fun getAllPrerequisites(): List<String> {
        return reqWeak + reqStrong + indConjunto
    }
}

enum class LectureType {
    OBRIGATORIA,
    OPTATIVA_ELETIVA,
    OPTATIVA_LIVRE;

    companion object {
        fun fromString(type: String): LectureType {
            return when (type.lowercase()) {
                "obrigatoria", "obrigatória" -> OBRIGATORIA
                "optativa_eletiva", "optativa eletiva" -> OPTATIVA_ELETIVA
                "optativa_livre", "optativa livre" -> OPTATIVA_LIVRE
                else -> OPTATIVA_LIVRE // Default fallback
            }
        }
    }

    override fun toString(): String {
        return when (this) {
            OBRIGATORIA -> "obrigatoria"
            OPTATIVA_ELETIVA -> "optativa_eletiva"
            OPTATIVA_LIVRE -> "optativa_livre"
        }
    }
}

data class Lecture(
    val code: String,
    val name: String,
    val unit: String,
    val department: String,
    val campus: String,
    val objectives: String = "",
    val summary: String = "",
    val lectureCredits: Int = 0,
    val workCredits: Int = 0,
    val classrooms: List<Classroom> = listOf(),
    val prerequisites: List<String> = listOf(),
    val bibliography: String = "",
    val lastUpdated: LocalDate? = null,
    val isActive: Boolean = true
) {
    /**
     * Total credits for this lecture
     */
    val totalCredits: Int
        get() = lectureCredits + workCredits

    /**
     * All teachers across all classrooms
     */
    val teachers: List<String>
        get() = classrooms.flatMap { it.teachers }.distinct()

    /**
     * All schedules across all classrooms
     */
    val schedules: List<Schedule>
        get() = classrooms.flatMap { it.schedules }

    /**
     * Checks if this lecture is currently available
     */
    val isAvailable: Boolean
        get() = isActive && classrooms.isNotEmpty()

    /**
     * Enhanced search functionality with better matching algorithms
     */
    fun matchesSearch(searchQuery: String): Boolean {
        val normalizedQuery = searchQuery.lowercase(Locale.getDefault()).trim()

        // Empty query matches everything
        if (normalizedQuery.isEmpty()) return true

        // Split the query into words for multi-word matching
        val queryWords = normalizedQuery.split(Regex("\\s+"))

        // Check each field for matches
        return queryWords.all { word ->
            matchesWord(word)
        }
    }

    private fun matchesWord(word: String): Boolean {
        return code.lowercase().contains(word) ||
               name.lowercase().contains(word) ||
               unit.lowercase().contains(word) ||
               department.lowercase().contains(word) ||
               campus.lowercase().contains(word) ||
               summary.lowercase().contains(word) ||
               objectives.lowercase().contains(word) ||
               bibliography.lowercase().contains(word) ||
               matchesPartialCode(word) ||
               matchesAcronym(word) ||
               matchesTeacher(word)
    }

    private fun matchesPartialCode(word: String): Boolean {
        // MAC0110 would match "mac", "110", "m11", etc.
        return code.lowercase().windowed(word.length, 1, true).any { it == word }
    }

    private fun matchesAcronym(word: String): Boolean {
        // Match acronyms (e.g. "CD" would match "Calculo Diferencial")
        if (word.length <= 1 || !word.all { it.isLetter() }) return false

        val nameWords = name.split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (nameWords.size < word.length) return false

        return nameWords.take(word.length)
            .map { it.first().lowercase() }
            .joinToString("") == word
    }

    private fun matchesTeacher(word: String): Boolean {
        return teachers.any { teacher ->
            teacher.lowercase().contains(word)
        }
    }

    /**
     * Gets all available classrooms for this lecture
     */
    fun getAvailableClassrooms(): List<Classroom> {
        return classrooms.filter { it.isAvailable() }
    }

    /**
     * Gets schedules for a specific day
     */
    fun getSchedulesForDay(day: DayOfWeek): List<Schedule> {
        return schedules.filter { schedule ->
            schedule.getDayOfWeek() == day
        }
    }

    /**
     * Checks if this lecture conflicts with another lecture at a given time
     */
    fun conflictsWith(other: Lecture, day: DayOfWeek, time: LocalTime): Boolean {
        val mySchedules = getSchedulesForDay(day)
        val otherSchedules = other.getSchedulesForDay(day)

        return mySchedules.any { mySchedule ->
            otherSchedules.any { otherSchedule ->
                mySchedule.conflictsWith(otherSchedule, time)
            }
        }
    }

    /**
     * Gets the next schedule for this lecture
     */
    fun getNextSchedule(fromTime: LocalTime = LocalTime.now()): Schedule? {
        val today = LocalDate.now().dayOfWeek
        val tomorrow = today.plus(1)

        // Check today's remaining schedules
        val todaySchedules = getSchedulesForDay(today)
            .filter { schedule ->
                schedule.startTimeAsLocalTime().isAfter(fromTime)
            }

        if (todaySchedules.isNotEmpty()) {
            return todaySchedules.minByOrNull { it.startTimeAsLocalTime() }
        }

        // Check tomorrow's schedules
        val tomorrowSchedules = getSchedulesForDay(tomorrow)
        return tomorrowSchedules.minByOrNull { it.startTimeAsLocalTime() }
    }
}

data class Classroom(
    val code: String,
    val startDate: String,
    val endDate: String,
    val observations: String = "",
    val teachers: List<String> = listOf(),
    val schedules: List<Schedule> = listOf(),
    val vacancies: Map<String, VacancyInfo> = mapOf(),
    val type: ClassroomType? = null,
    val theoreticalCode: String? = null,
    val enrolledCount: Int = 0,
    val maxCapacity: Int = 0
) {
    /**
     * Checks if this classroom is currently available for enrollment
     */
    fun isAvailable(): Boolean {
        return enrolledCount < maxCapacity && isActive()
    }

    /**
     * Checks if this classroom is currently active (within date range)
     */
    fun isActive(): Boolean {
        return try {
            val now = LocalDate.now()
            val start = LocalDate.parse(startDate)
            val end = LocalDate.parse(endDate)
            now.isAfter(start.minusDays(1)) && now.isBefore(end.plusDays(1))
        } catch (e: Exception) {
            true // If date parsing fails, assume it's active
        }
    }

    /**
     * Gets the total number of vacancies available
     */
    fun getTotalVacancies(): Int {
        return vacancies.values.sumOf { it.total - it.enrolled }
    }

    /**
     * Gets schedules for a specific day
     */
    fun getSchedulesForDay(day: DayOfWeek): List<Schedule> {
        return schedules.filter { it.getDayOfWeek() == day }
    }

    /**
     * Checks if this classroom has any schedules
     */
    fun hasSchedules(): Boolean = schedules.isNotEmpty()

    /**
     * Gets the next schedule from now
     */
    fun getNextSchedule(fromTime: LocalTime = LocalTime.now()): Schedule? {
        val today = LocalDate.now().dayOfWeek
        val todaySchedules = getSchedulesForDay(today).filter { schedule ->
            schedule.startTimeAsLocalTime().isAfter(fromTime)
        }

        if (todaySchedules.isNotEmpty()) {
            return todaySchedules.minByOrNull { it.startTimeAsLocalTime() }
        }

        // Check next days
        for (i in 1..7) {
            val day = today.plus(i.toLong())
            val daySchedules = getSchedulesForDay(day)
            if (daySchedules.isNotEmpty()) {
                return daySchedules.minByOrNull { it.startTimeAsLocalTime() }
            }
        }

        return null
    }
}

enum class ClassroomType {
    TEORICA,
    PRATICA,
    TEORICA_PRATICA;

    companion object {
        fun fromString(type: String?): ClassroomType? {
            return when (type?.lowercase()) {
                "teórica", "teorica" -> TEORICA
                "prática", "pratica" -> PRATICA
                "teórica+prática", "teorica+pratica", "teórica prática" -> TEORICA_PRATICA
                else -> null
            }
        }
    }

    override fun toString(): String {
        return when (this) {
            TEORICA -> "Teórica"
            PRATICA -> "Prática"
            TEORICA_PRATICA -> "Teórica+Prática"
        }
    }
}

data class Schedule(
    val day: String, // "seg", "ter", "qua", "qui", "sex", "sab", "dom"
    val startTime: String, // "08:00"
    val endTime: String,   // "09:40"
    val teachers: List<String> = listOf(),
    val location: String = ""
) {
    /**
     * Converts day string to DayOfWeek enum
     */
    fun getDayOfWeek(): DayOfWeek? {
        return when (day.lowercase()) {
            "seg" -> DayOfWeek.MONDAY
            "ter" -> DayOfWeek.TUESDAY
            "qua" -> DayOfWeek.WEDNESDAY
            "qui" -> DayOfWeek.THURSDAY
            "sex" -> DayOfWeek.FRIDAY
            "sab" -> DayOfWeek.SATURDAY
            "dom" -> DayOfWeek.SUNDAY
            else -> null
        }
    }

    /**
     * Converts start time string to LocalTime
     */
    fun startTimeAsLocalTime(): LocalTime {
        return try {
            LocalTime.parse(startTime)
        } catch (e: Exception) {
            LocalTime.MIDNIGHT
        }
    }

    /**
     * Converts end time string to LocalTime
     */
    fun endTimeAsLocalTime(): LocalTime {
        return try {
            LocalTime.parse(endTime)
        } catch (e: Exception) {
            LocalTime.MIDNIGHT
        }
    }

    /**
     * Gets the duration of this schedule in minutes
     */
    fun getDurationMinutes(): Int {
        return try {
            val start = startTimeAsLocalTime()
            val end = endTimeAsLocalTime()
            java.time.Duration.between(start, end).toMinutes().toInt()
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Checks if this schedule conflicts with another schedule
     */
    fun conflictsWith(other: Schedule, specificTime: LocalTime? = null): Boolean {
        if (day != other.day) return false

        val myStart = startTimeAsLocalTime()
        val myEnd = endTimeAsLocalTime()
        val otherStart = other.startTimeAsLocalTime()
        val otherEnd = other.endTimeAsLocalTime()

        val checkTime = specificTime ?: myStart

        return checkTime.isBefore(otherEnd) && otherStart.isBefore(myEnd)
    }

    /**
     * Checks if a given time falls within this schedule
     */
    fun containsTime(time: LocalTime): Boolean {
        val start = startTimeAsLocalTime()
        val end = endTimeAsLocalTime()
        return !time.isBefore(start) && time.isBefore(end)
    }
}

data class VacancyInfo(
    val total: Int,
    val subscribed: Int,
    val pending: Int,
    val enrolled: Int,
    val groups: Map<String, VacancyInfo> = mapOf()
)