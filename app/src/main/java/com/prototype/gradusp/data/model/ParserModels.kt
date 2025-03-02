package com.prototype.gradusp.data.model

import java.util.Locale

// Data models
data class Course(
    val code: String,
    val name: String,
    val unit: String,
    val period: String,
    val periods: Map<String, List<LectureInfo>> = mapOf()
)

data class LectureInfo(
    val code: String,
    val type: String, // "obrigatoria", "optativa_eletiva", "optativa_livre"
    val reqWeak: List<String> = listOf(),
    val reqStrong: List<String> = listOf(),
    val indConjunto: List<String> = listOf()
)

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
    val classrooms: List<Classroom> = listOf()
) {
    fun matchesSearch(searchQuery: String): Boolean {
        val normalizedQuery = searchQuery.lowercase(Locale.getDefault()).trim()

        // Empty query matches everything
        if (normalizedQuery.isEmpty()) return true

        // Split the query into words for multi-word matching
        val queryWords = normalizedQuery.split(Regex("\\s+"))

        // Check each field for matches
        return queryWords.all { word ->
            code.lowercase().contains(word) ||
                    name.lowercase().contains(word) ||
                    unit.lowercase().contains(word) ||
                    department.lowercase().contains(word) ||
                    campus.lowercase().contains(word) ||
                    summary.lowercase().contains(word) ||

                    // Check for partial matches in lecture code
                    // MAC0110 would match "mac", "110", "m11", etc.
                    code.lowercase().windowed(word.length, 1, true)
                        .any { it == word } ||

                    // Match acronyms (e.g. "CD" would match "Calculo Diferencial")
                    (word.length > 1 && word.all { it.isLetter() } &&
                            name.split(Regex("\\s+")).filter { it.isNotEmpty() }
                                .map { it.first().lowercase() }
                                .joinToString("").contains(word)) ||

                    // Check teacher names in classrooms
                    classrooms.any { classroom ->
                        classroom.teachers.any { teacher ->
                            teacher.lowercase().contains(word)
                        }
                    }
        }
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
    val type: String? = null,
    val theoreticalCode: String? = null
)
data class Schedule(
    val day: String, // "seg", "ter", "qua", "qui", "sex", "sab", "dom"
    val startTime: String, // "08:00"
    val endTime: String,   // "09:40"
    val teachers: List<String> = listOf()
)

data class VacancyInfo(
    val total: Int,
    val subscribed: Int,
    val pending: Int,
    val enrolled: Int,
    val groups: Map<String, VacancyInfo> = mapOf()
)