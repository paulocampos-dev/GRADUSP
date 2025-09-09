package com.prototype.gradusp.data.parser

import android.content.Context
import android.util.Log
import com.prototype.gradusp.data.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.OkHttpClient

/**
 * Enhanced USP Parser that mimics USPolis structure with separate crawlers and transformers.
 * This is the main entry point for all USP data parsing operations.
 */
class EnhancedUspParser(
    private val context: Context,
    private val client: OkHttpClient = OkHttpClient()
) {

    private val jupiterCrawler = JupiterCrawler(client)
    private val janusCrawler = JanusCrawler(client)
    private val dataTransformer = DataTransformer()

    private val campusByUnit = mapOf(
        listOf(86, 27, 39, 7, 22, 3, 16, 9, 2, 12, 48, 8, 5, 10, 67, 23, 6, 66, 14, 26, 93, 41, 92, 42, 4, 37, 43, 44, 45, 83, 47, 46, 87, 21, 31, 85, 71, 32, 38, 33) to "São Paulo",
        listOf(98, 94, 60, 89, 81, 59, 96, 91, 17, 58, 95) to "Ribeirão Preto",
        listOf(88) to "Lorena",
        listOf(18, 97, 99, 55, 76, 75, 90) to "São Carlos",
        listOf(11, 64) to "Piracicaba",
        listOf(25, 61) to "Bauru",
        listOf(74) to "Pirassununga",
        listOf(30) to "São Sebastião"
    )

    companion object {
        private const val TAG = "EnhancedUspParser"
    }

    // JupiterWeb Operations (Academic Data)

    /**
     * Fetches all available units/colleges from JupiterWeb
     */
    suspend fun fetchCampusUnits(): Map<String, List<String>> = withContext(Dispatchers.IO) {
        try {
            val result = jupiterCrawler.fetchUnits()
            result.getOrNull()?.let { units ->
                // Group units by campus
                val campusMap = mutableMapOf<String, MutableList<String>>()
                units.forEach { (unitName, unitCode) ->
                    val campus = campusByUnit.entries.find { it.key.contains(unitCode.toInt()) }?.value ?: "Outro"
                    campusMap.getOrPut(campus) { mutableListOf() }.add(unitName)
                }
                campusMap
            } ?: emptyMap()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching campus units", e)
            emptyMap()
        }
    }

    /**
     * Fetches all disciplines for a specific unit
     */
    suspend fun fetchAllDisciplinesForUnit(unitCode: String): List<String> = withContext(Dispatchers.IO) {
        try {
            val result = jupiterCrawler.fetchDisciplinesForUnit(unitCode)
            result.getOrNull() ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching disciplines for unit $unitCode", e)
            emptyList()
        }
    }

    /**
     * Fetches all courses for a specific unit
     */
    suspend fun fetchCoursesForUnit(unitCode: String): List<Course> = withContext(Dispatchers.IO) {
        try {
            val result = jupiterCrawler.fetchCoursesForUnit(unitCode)
            result.getOrNull()?.mapNotNull { course ->
                dataTransformer.transformCourse(course)
            } ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching courses for unit $unitCode", e)
            emptyList()
        }
    }

    /**
     * Fetches detailed information about a specific lecture
     */
    suspend fun fetchLecture(lectureCode: String): Lecture? = withContext(Dispatchers.IO) {
        try {
            val result = jupiterCrawler.fetchLectureDetails(lectureCode)
            result.getOrNull()?.let { lecture ->
                dataTransformer.transformLecture(lecture).let { transformed ->
                    dataTransformer.enrichLecture(transformed)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching lecture $lectureCode", e)
            null
        }
    }

    /**
     * Searches for lectures across all units
     */
    suspend fun searchLectures(query: String): List<Lecture> = withContext(Dispatchers.IO) {
        try {
            val units = jupiterCrawler.fetchUnits().getOrNull() ?: emptyMap()
            val result = jupiterCrawler.searchLectures(query, units)
            val lectures = result.getOrNull() ?: emptyList()

            val transformedLectures = lectures.map { lecture ->
                dataTransformer.transformLecture(lecture).let { transformed ->
                    dataTransformer.enrichLecture(transformed)
                }
            }

            dataTransformer.sortLecturesByRelevance(transformedLectures, query)
        } catch (e: Exception) {
            Log.e(TAG, "Error searching lectures", e)
            emptyList()
        }
    }

    /**
     * Fetches curriculum information for a specific course
     */
    suspend fun fetchCurriculum(courseCode: String, unitCode: String): Course? = withContext(Dispatchers.IO) {
        try {
            val result = jupiterCrawler.fetchCurriculum(courseCode, unitCode)
            result.getOrNull()?.let { course ->
                dataTransformer.transformCourse(course)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching curriculum for course $courseCode", e)
            null
        }
    }

    // Janus Operations (Student Data)

    /**
     * Authenticates student with USP credentials
     */
    suspend fun authenticateStudent(username: String, password: String): CrawlerResult<JanusCrawler.StudentSession> {
        return janusCrawler.authenticateStudent(username, password)
    }

    /**
     * Fetches student's enrolled courses
     */
    suspend fun fetchStudentEnrollments(session: JanusCrawler.StudentSession): List<StudentEnrollment> {
        return janusCrawler.fetchStudentEnrollments(session).getOrNull() ?: emptyList()
    }

    /**
     * Fetches student's grades
     */
    suspend fun fetchStudentGrades(session: JanusCrawler.StudentSession): List<StudentGrade> {
        return janusCrawler.fetchStudentGrades(session).getOrNull() ?: emptyList()
    }

    /**
     * Fetches student's academic history
     */
    suspend fun fetchAcademicHistory(session: JanusCrawler.StudentSession): StudentHistory? {
        return janusCrawler.fetchAcademicHistory(session).getOrNull()
    }

    /**
     * Searches for available courses that student can enroll in
     */
    suspend fun searchAvailableCourses(session: JanusCrawler.StudentSession, query: String): List<AvailableCourse> {
        return janusCrawler.searchAvailableCourses(session, query).getOrNull() ?: emptyList()
    }

    // Utility methods

    /**
     * Gets unit code from unit name
     */
    suspend fun getUnitCode(unitName: String): String? {
        return jupiterCrawler.fetchUnits().getOrNull()?.get(unitName)
    }

    /**
     * Provides a flow for progressive lecture search results
     */
    fun searchLecturesFlow(query: String): Flow<List<Lecture>> = flow {
        try {
            val units = jupiterCrawler.fetchUnits().getOrNull() ?: emptyMap()

            // Emit partial results as they come in
            val allLectures = mutableListOf<Lecture>()

            coroutineScope {
                val searchTasks = units.values.map { unitCode ->
                    async {
                        jupiterCrawler.fetchDisciplinesForUnit(unitCode).getOrNull()?.let { disciplineCodes ->
                            disciplineCodes.mapNotNull { code ->
                                if (code.contains(query, ignoreCase = true)) {
                                    jupiterCrawler.fetchLectureDetails(code).getOrNull()?.let { lecture ->
                                        dataTransformer.transformLecture(lecture).let { transformed ->
                                            dataTransformer.enrichLecture(transformed)
                                        }
                                    }
                                } else null
                            }
                        } ?: emptyList()
                    }
                }

                searchTasks.forEach { task ->
                    val lectures = task.await()
                    allLectures.addAll(lectures)
                    emit(dataTransformer.sortLecturesByRelevance(allLectures.toList(), query))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in lecture search flow", e)
            emit(emptyList())
        }
    }

    /**
     * Validates if a lecture code follows USP format
     */
    fun isValidLectureCode(code: String): Boolean {
        return Regex("^[A-Z]{3}\\d{4}$").matches(code.uppercase())
    }

    /**
     * Validates if a course code follows USP format
     */
    fun isValidCourseCode(code: String): Boolean {
        return Regex("^\\d+-\\d+$").matches(code)
    }
}

// Extension functions for easier usage

fun EnhancedUspParser.filterLecturesByCampus(lectures: List<Lecture>, campus: String): List<Lecture> {
    return lectures.filter { it.campus.equals(campus, ignoreCase = true) }
}

fun EnhancedUspParser.filterLecturesByUnit(lectures: List<Lecture>, unit: String): List<Lecture> {
    return lectures.filter { it.unit.equals(unit, ignoreCase = true) }
}

fun EnhancedUspParser.filterLecturesWithSchedule(lectures: List<Lecture>): List<Lecture> {
    return lectures.filter { lecture ->
        lecture.classrooms.any { it.schedules.isNotEmpty() }
    }
}
