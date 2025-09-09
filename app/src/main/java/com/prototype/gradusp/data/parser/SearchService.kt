package com.prototype.gradusp.data.parser

import android.util.Log
import com.prototype.gradusp.data.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.*

/**
 * Enhanced search service for finding lectures and courses with advanced filtering and ranking.
 * Provides multiple search strategies and result ranking based on relevance.
 */
class SearchService(
    private val parser: EnhancedUspParser,
    private val dataTransformer: DataTransformer = DataTransformer()
) {

    companion object {
        private const val TAG = "SearchService"
        private const val MAX_SEARCH_RESULTS = 100
        private const val SEARCH_TIMEOUT_MS = 10000L
    }

    /**
     * Search result with relevance scoring
     */
    data class SearchResult<T>(
        val item: T,
        val relevanceScore: Int,
        val matchReasons: List<String> = emptyList()
    )

    /**
     * Search filters for advanced querying
     */
    data class SearchFilters(
        val campus: String? = null,
        val unit: String? = null,
        val hasVacancies: Boolean? = null,
        val dayOfWeek: DayOfWeek? = null,
        val timeRange: ClosedRange<LocalTime>? = null,
        val minCredits: Int? = null,
        val maxCredits: Int? = null,
        val lectureType: LectureType? = null,
        val classroomType: ClassroomType? = null
    )

    /**
     * Comprehensive lecture search with advanced filtering and ranking
     */
    suspend fun searchLecturesAdvanced(
        query: String,
        filters: SearchFilters = SearchFilters(),
        maxResults: Int = MAX_SEARCH_RESULTS
    ): List<SearchResult<Lecture>> = withContext(Dispatchers.IO) {
        try {
            withTimeout(SEARCH_TIMEOUT_MS) {
                val allLectures = parser.searchLectures(query)

                val filteredLectures = applyFilters(allLectures, filters, query)
                val rankedResults = rankResults(filteredLectures, query)

                rankedResults.take(maxResults)
            }
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "Search timed out for query: $query")
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error in advanced lecture search", e)
            emptyList()
        }
    }

    /**
     * Search courses with curriculum information
     */
    suspend fun searchCourses(
        query: String,
        filters: SearchFilters = SearchFilters(),
        includeCurriculum: Boolean = false
    ): List<SearchResult<Course>> = withContext(Dispatchers.IO) {
        try {
            val units = parser.fetchCampusUnits()
            val allCourses = mutableListOf<Course>()

            // Search courses across all units
            coroutineScope {
                units.map { (campus, unitList) ->
                    async {
                        unitList.flatMap { unitName ->
                            val unitCode = parser.getUnitCode(unitName)
                            unitCode?.let { code ->
                                parser.fetchCoursesForUnit(code)
                            } ?: emptyList()
                        }
                    }
                }.awaitAll().forEach { courses ->
                    allCourses.addAll(courses)
                }
            }

            val filteredCourses = applyCourseFilters(allCourses, filters, query, units)
            val rankedResults = rankCourseResults(filteredCourses, query)

            if (includeCurriculum) {
                // Load curriculum details for top results
                rankedResults.take(10).map { result ->
                    async {
                        val course = result.item
                        val unitCode = parser.getUnitCode(course.unit)
                        unitCode?.let { code ->
                            parser.fetchCurriculum(course.code, code)?.let { detailedCourse ->
                                result.copy(item = detailedCourse)
                            }
                        } ?: result
                    }
                }.awaitAll()
            } else {
                rankedResults
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error searching courses", e)
            emptyList()
        }
    }

    /**
     * Find lectures that don't conflict with a given schedule
     */
    suspend fun findNonConflictingLectures(
        existingLectures: List<Lecture>,
        query: String,
        filters: SearchFilters = SearchFilters()
    ): List<SearchResult<Lecture>> = withContext(Dispatchers.IO) {
        try {
            val candidateLectures = parser.searchLectures(query)
            val filteredCandidates = applyFilters(candidateLectures, filters, query)

            val nonConflicting = filteredCandidates.filter { candidate ->
                !conflictsWithAny(existingLectures, candidate)
            }

            rankResults(nonConflicting, query)
        } catch (e: Exception) {
            Log.e(TAG, "Error finding non-conflicting lectures", e)
            emptyList()
        }
    }

    /**
     * Progressive search that returns results as they become available
     */
    fun searchLecturesProgressive(
        query: String,
        filters: SearchFilters = SearchFilters()
    ): Flow<List<SearchResult<Lecture>>> = flow {
        try {
            val units = parser.fetchCampusUnits()
            val allResults = mutableListOf<Lecture>()

            // Emit partial results for each unit
            for ((campus, unitList) in units) {
                if (filters.campus != null && campus != filters.campus) continue

                for (unitName in unitList) {
                    if (filters.unit != null && unitName != filters.unit) continue

                    val unitCode = parser.getUnitCode(unitName)
                    unitCode?.let { code ->
                        val disciplines = parser.fetchAllDisciplinesForUnit(code)

                        val unitLectures = disciplines.mapNotNull { disciplineCode ->
                            if (disciplineCode.contains(query, ignoreCase = true)) {
                                parser.fetchLecture(disciplineCode)
                            } else null
                        }

                        allResults.addAll(unitLectures)
                        val filtered = applyFilters(allResults, filters, query)
                        val ranked = rankResults(filtered, query)

                        emit(ranked.take(50)) // Emit top 50 results so far
                    }
                }
            }

            // Final emission with all results
            val finalFiltered = applyFilters(allResults, filters, query)
            val finalRanked = rankResults(finalFiltered, query)
            emit(finalRanked)

        } catch (e: Exception) {
            Log.e(TAG, "Error in progressive search", e)
            emit(emptyList())
        }
    }

    // Filtering methods

    private fun applyFilters(
        lectures: List<Lecture>,
        filters: SearchFilters,
        query: String
    ): List<Lecture> {
        return lectures.filter { lecture ->
            // Campus filter
            if (filters.campus != null && !lecture.campus.equals(filters.campus, ignoreCase = true)) {
                return@filter false
            }

            // Unit filter
            if (filters.unit != null && !lecture.unit.equals(filters.unit, ignoreCase = true)) {
                return@filter false
            }

            // Vacancy filter
            if (filters.hasVacancies == true && !lecture.classrooms.any { it.isAvailable() }) {
                return@filter false
            }

            // Day of week filter
            if (filters.dayOfWeek != null) {
                val hasScheduleOnDay = lecture.schedules.any { schedule ->
                    schedule.getDayOfWeek() == filters.dayOfWeek
                }
                if (!hasScheduleOnDay) return@filter false
            }

            // Time range filter
            if (filters.timeRange != null) {
                val hasScheduleInRange = lecture.schedules.any { schedule ->
                    val startTime = schedule.startTimeAsLocalTime()
                    val endTime = schedule.endTimeAsLocalTime()
                    filters.timeRange.start.isBefore(endTime) &&
                    filters.timeRange.endInclusive.isAfter(startTime)
                }
                if (!hasScheduleInRange) return@filter false
            }

            // Credits filter
            if (filters.minCredits != null && lecture.totalCredits < filters.minCredits) {
                return@filter false
            }
            if (filters.maxCredits != null && lecture.totalCredits > filters.maxCredits) {
                return@filter false
            }

            // Classroom type filter
            if (filters.classroomType != null) {
                val hasMatchingClassroom = lecture.classrooms.any { classroom ->
                    classroom.type == filters.classroomType
                }
                if (!hasMatchingClassroom) return@filter false
            }

            true
        }
    }

    private fun applyCourseFilters(
        courses: List<Course>,
        filters: SearchFilters,
        query: String,
        units: Map<String, List<String>>
    ): List<Course> {
        return courses.filter { course ->
            // Campus filter via unit lookup
            if (filters.campus != null) {
                val unitCode = parser.getUnitCode(course.unit)
                val campus = units.entries.find { (_, unitList) ->
                    unitList.contains(course.unit)
                }?.key

                if (campus != filters.campus) return@filter false
            }

            // Unit filter
            if (filters.unit != null && !course.unit.equals(filters.unit, ignoreCase = true)) {
                return@filter false
            }

            // Search query filter
            if (!course.matchesSearch(query)) return@filter false

            true
        }
    }

    // Ranking methods

    private fun rankResults(lectures: List<Lecture>, query: String): List<SearchResult<Lecture>> {
        return lectures.map { lecture ->
            val (score, reasons) = calculateRelevanceScore(lecture, query)
            SearchResult(lecture, score, reasons)
        }.sortedByDescending { it.relevanceScore }
    }

    private fun rankCourseResults(courses: List<Course>, query: String): List<SearchResult<Course>> {
        return courses.map { course ->
            val (score, reasons) = calculateCourseRelevanceScore(course, query)
            SearchResult(course, score, reasons)
        }.sortedByDescending { it.relevanceScore }
    }

    private fun calculateRelevanceScore(lecture: Lecture, query: String): Pair<Int, List<String>> {
        var score = 0
        val reasons = mutableListOf<String>()

        val normalizedQuery = query.lowercase().trim()

        // Exact code match
        if (lecture.code.lowercase() == normalizedQuery) {
            score += 100
            reasons.add("Código exato")
        }

        // Code prefix match
        if (lecture.code.lowercase().startsWith(normalizedQuery)) {
            score += 50
            reasons.add("Prefixo do código")
        }

        // Code contains query
        if (lecture.code.lowercase().contains(normalizedQuery)) {
            score += 25
            reasons.add("Código contém termo")
        }

        // Name contains query
        if (lecture.name.lowercase().contains(normalizedQuery)) {
            score += 20
            reasons.add("Nome contém termo")
        }

        // Department match
        if (lecture.department.lowercase().contains(normalizedQuery)) {
            score += 10
            reasons.add("Departamento")
        }

        // Summary match
        if (lecture.summary.lowercase().contains(normalizedQuery)) {
            score += 5
            reasons.add("Descrição contém termo")
        }

        // Teacher match
        if (lecture.teachers.any { it.lowercase().contains(normalizedQuery) }) {
            score += 15
            reasons.add("Professor")
        }

        // Availability bonus
        if (lecture.isAvailable) {
            score += 10
            reasons.add("Disponível")
        }

        // Has vacancies bonus
        if (lecture.classrooms.any { it.isAvailable() }) {
            score += 5
            reasons.add("Vagas disponíveis")
        }

        return score to reasons
    }

    private fun calculateCourseRelevanceScore(course: Course, query: String): Pair<Int, List<String>> {
        var score = 0
        val reasons = mutableListOf<String>()

        val normalizedQuery = query.lowercase().trim()

        // Exact code match
        if (course.code.lowercase() == normalizedQuery) {
            score += 100
            reasons.add("Código exato")
        }

        // Code contains query
        if (course.code.lowercase().contains(normalizedQuery)) {
            score += 30
            reasons.add("Código contém termo")
        }

        // Name contains query
        if (course.name.lowercase().contains(normalizedQuery)) {
            score += 25
            reasons.add("Nome contém termo")
        }

        // Unit match
        if (course.unit.lowercase().contains(normalizedQuery)) {
            score += 15
            reasons.add("Unidade")
        }

        return score to reasons
    }

    // Utility methods

    private fun conflictsWithAny(existingLectures: List<Lecture>, candidate: Lecture): Boolean {
        return existingLectures.any { existing ->
            candidate.conflictsWith(existing, DayOfWeek.MONDAY, LocalTime.now()) ||
            candidate.conflictsWith(existing, DayOfWeek.TUESDAY, LocalTime.now()) ||
            candidate.conflictsWith(existing, DayOfWeek.WEDNESDAY, LocalTime.now()) ||
            candidate.conflictsWith(existing, DayOfWeek.THURSDAY, LocalTime.now()) ||
            candidate.conflictsWith(existing, DayOfWeek.FRIDAY, LocalTime.now())
        }
    }

    /**
     * Get search suggestions based on partial query
     */
    suspend fun getSearchSuggestions(partialQuery: String, maxSuggestions: Int = 5): List<String> {
        return try {
            val lectures = parser.searchLectures(partialQuery).take(20)

            val suggestions = mutableSetOf<String>()

            lectures.forEach { lecture ->
                // Add code suggestions
                if (lecture.code.lowercase().startsWith(partialQuery.lowercase())) {
                    suggestions.add(lecture.code)
                }

                // Add name suggestions
                val nameWords = lecture.name.split(" ")
                nameWords.forEach { word ->
                    if (word.lowercase().startsWith(partialQuery.lowercase())) {
                        suggestions.add(word)
                    }
                }

                // Add teacher suggestions
                lecture.teachers.forEach { teacher ->
                    val teacherWords = teacher.split(" ")
                    teacherWords.forEach { word ->
                        if (word.lowercase().startsWith(partialQuery.lowercase())) {
                            suggestions.add(word)
                        }
                    }
                }
            }

            suggestions.take(maxSuggestions)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting search suggestions", e)
            emptyList()
        }
    }
}
