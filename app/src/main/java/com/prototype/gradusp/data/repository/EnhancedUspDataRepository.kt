package com.prototype.gradusp.data.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.prototype.gradusp.data.UserPreferencesRepository
import com.prototype.gradusp.data.dataStore
import com.prototype.gradusp.data.model.*
import com.prototype.gradusp.data.parser.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

sealed class EnhancedSyncResult {
    data object Success : EnhancedSyncResult()
    data class Progress(val percentage: Float, val message: String) : EnhancedSyncResult()
    data class Error(val message: String, val cause: Exception? = null) : EnhancedSyncResult()
}

@Singleton
class EnhancedUspDataRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val parser: EnhancedUspParser,
    private val searchService: SearchService,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    private val gson = Gson()
    private val cache = UspDataCache(context)

    companion object {
        private const val TAG = "EnhancedUspDataRepository"
        private const val LECTURE_BATCH_SIZE = 20
        private const val CACHE_VERSION = 1
    }

    private val _syncProgress = MutableStateFlow<EnhancedSyncResult>(EnhancedSyncResult.Success)
    val syncProgress: StateFlow<EnhancedSyncResult> = _syncProgress

    // Public API

    /**
     * Enhanced search with advanced filtering and relevance ranking
     */
    suspend fun searchLecturesAdvanced(
        query: String,
        filters: SearchService.SearchFilters = SearchService.SearchFilters(),
        maxResults: Int = 50
    ): List<SearchService.SearchResult<Lecture>> = withContext(Dispatchers.IO) {
        try {
            // Try cache first
            val cacheKey = "search_${query}_${filters.hashCode()}"
            cache.getCachedSearchResults(cacheKey)?.let { cached ->
                if (!isCacheExpired(cached.timestamp)) {
                    Log.d(TAG, "Returning cached search results for: $query")
                    return@withContext cached.results.take(maxResults)
                }
            }

            // Perform search
            val results = searchService.searchLecturesAdvanced(query, filters, maxResults)

            // Cache results
            val cacheEntry = CachedSearchResults(
                query = query,
                filters = filters,
                results = results,
                timestamp = System.currentTimeMillis()
            )
            cache.cacheSearchResults(cacheKey, cacheEntry)

            results
        } catch (e: Exception) {
            Log.e(TAG, "Error in enhanced search", e)
            emptyList()
        }
    }

    /**
     * Progressive search that emits results as they become available
     */
    fun searchLecturesProgressive(
        query: String,
        filters: SearchService.SearchFilters = SearchService.SearchFilters()
    ): Flow<List<SearchService.SearchResult<Lecture>>> {
        return searchService.searchLecturesProgressive(query, filters)
    }

    /**
     * Get search suggestions based on partial query
     */
    suspend fun getSearchSuggestions(partialQuery: String, maxSuggestions: Int = 5): List<String> {
        return searchService.getSearchSuggestions(partialQuery, maxSuggestions)
    }

    /**
     * Find non-conflicting lectures
     */
    suspend fun findNonConflictingLectures(
        existingLectures: List<Lecture>,
        query: String,
        filters: SearchService.SearchFilters = SearchService.SearchFilters()
    ): List<SearchService.SearchResult<Lecture>> = withContext(Dispatchers.IO) {
        try {
            searchService.findNonConflictingLectures(existingLectures, query, filters)
        } catch (e: Exception) {
            Log.e(TAG, "Error finding non-conflicting lectures", e)
            emptyList()
        }
    }

    /**
     * Get campus units with caching
     */
    suspend fun getCampusUnits(): Map<String, List<String>> = withContext(Dispatchers.IO) {
        try {
            // Try cache first
            cache.getCachedCampusUnits()?.let { cached ->
                if (!isCacheExpired(cached.timestamp)) {
                    return@withContext cached.units
                }
            }

            // Fetch from parser
            val units = parser.fetchCampusUnits()

            // Cache results
            val cacheEntry = CachedCampusUnits(
                units = units,
                timestamp = System.currentTimeMillis()
            )
            cache.cacheCampusUnits(cacheEntry)

            units
        } catch (e: Exception) {
            Log.e(TAG, "Error getting campus units", e)
            emptyMap()
        }
    }

    /**
     * Get lecture with enhanced caching and fallback
     */
    suspend fun getLecture(code: String): Lecture? = withContext(Dispatchers.IO) {
        try {
            // Try cache first
            cache.getCachedLecture(code)?.let { cached ->
                if (!isCacheExpired(cached.timestamp)) {
                    Log.d(TAG, "Returning cached lecture: $code")
                    return@withContext cached.lecture
                }
            }

            // Fetch from parser
            val lecture = parser.fetchLecture(code)

            // Cache if successful
            lecture?.let { cache.cacheLecture(code, it) }

            lecture
        } catch (e: Exception) {
            Log.e(TAG, "Error getting lecture $code", e)
            null
        }
    }

    /**
     * Get courses for unit with caching
     */
    suspend fun getCoursesForUnit(unitCode: String): List<Course> = withContext(Dispatchers.IO) {
        try {
            // Try cache first
            cache.getCachedCourses(unitCode)?.let { cached ->
                if (!isCacheExpired(cached.timestamp)) {
                    Log.d(TAG, "Returning cached courses for unit: $unitCode")
                    return@withContext cached.courses
                }
            }

            // Fetch from parser
            val courses = parser.fetchCoursesForUnit(unitCode)

            // Cache results
            val cacheEntry = CachedCourses(
                unitCode = unitCode,
                courses = courses,
                timestamp = System.currentTimeMillis()
            )
            cache.cacheCourses(unitCode, cacheEntry)

            courses
        } catch (e: Exception) {
            Log.e(TAG, "Error getting courses for unit $unitCode", e)
            emptyList()
        }
    }

    /**
     * Get curriculum with caching
     */
    suspend fun getCurriculum(courseCode: String, unitCode: String): Course? = withContext(Dispatchers.IO) {
        try {
            // Try cache first
            cache.getCachedCurriculum(courseCode, unitCode)?.let { cached ->
                if (!isCacheExpired(cached.timestamp)) {
                    return@withContext cached.course
                }
            }

            // Fetch from parser
            val course = parser.fetchCurriculum(courseCode, unitCode)

            // Cache if successful
            course?.let {
                val cacheEntry = CachedCurriculum(
                    courseCode = courseCode,
                    unitCode = unitCode,
                    course = it,
                    timestamp = System.currentTimeMillis()
                )
                cache.cacheCurriculum(cacheEntry)
            }

            course
        } catch (e: Exception) {
            Log.e(TAG, "Error getting curriculum for $courseCode", e)
            null
        }
    }

    /**
     * Enhanced data synchronization with progress tracking
     */
    suspend fun syncData(): Flow<EnhancedSyncResult> = flow {
        try {
            emit(EnhancedSyncResult.Progress(0f, "Starting synchronization..."))

            // Fetch campus units
            emit(EnhancedSyncResult.Progress(0.1f, "Fetching campus units..."))
            val campusUnits = parser.fetchCampusUnits()

            // Cache campus units
            val campusCacheEntry = CachedCampusUnits(
                units = campusUnits,
                timestamp = System.currentTimeMillis()
            )
            cache.cacheCampusUnits(campusCacheEntry)

            emit(EnhancedSyncResult.Progress(0.2f, "Campus units cached"))

            // Get selected schools from preferences
            val selectedSchools = userPreferencesRepository.selectedSchoolsFlow.first()
            val unitsToProcess = if (selectedSchools.isEmpty()) {
                campusUnits.values.flatten().take(3)
            } else {
                selectedSchools.toList()
            }

            if (unitsToProcess.isEmpty()) {
                emit(EnhancedSyncResult.Error("No units selected for synchronization"))
                return@flow
            }

            val progressPerUnit = 0.7f / unitsToProcess.size.toFloat()

            for ((index, unitName) in unitsToProcess.withIndex()) {
                val unitCode = parser.getUnitCode(unitName) ?: continue
                val unitStartProgress = 0.2f + (progressPerUnit * index)

                emit(EnhancedSyncResult.Progress(unitStartProgress, "Processing $unitName..."))

                // Fetch and cache courses
                val courses = parser.fetchCoursesForUnit(unitCode)
                val coursesCacheEntry = CachedCourses(
                    unitCode = unitCode,
                    courses = courses,
                    timestamp = System.currentTimeMillis()
                )
                cache.cacheCourses(unitCode, coursesCacheEntry)

                emit(EnhancedSyncResult.Progress(
                    unitStartProgress + progressPerUnit * 0.3f,
                    "Courses cached for $unitName"
                ))

                // Fetch and cache lectures in batches
                val disciplineCodes = parser.fetchAllDisciplinesForUnit(unitCode)
                val batches = disciplineCodes.chunked(LECTURE_BATCH_SIZE)

                for ((batchIndex, batch) in batches.withIndex()) {
                    val batchProgress = unitStartProgress + progressPerUnit * 0.4f +
                                      (progressPerUnit * 0.6f * batchIndex / batches.size)

                    emit(EnhancedSyncResult.Progress(
                        batchProgress,
                        "Processing lectures ${batchIndex + 1}/${batches.size} for $unitName..."
                    ))

                    coroutineScope {
                        batch.map { code ->
                            async(Dispatchers.IO) {
                                parser.fetchLecture(code)?.let { lecture ->
                                    cache.cacheLecture(code, lecture)
                                }
                            }
                        }.awaitAll()
                    }
                }

                emit(EnhancedSyncResult.Progress(
                    unitStartProgress + progressPerUnit,
                    "$unitName completed"
                ))
            }

            emit(EnhancedSyncResult.Progress(0.95f, "Cleaning up cache..."))
            cache.cleanup()

            emit(EnhancedSyncResult.Success)

        } catch (e: Exception) {
            Log.e(TAG, "Error during synchronization", e)
            emit(EnhancedSyncResult.Error("Synchronization failed: ${e.message}", e))
        }
    }

    /**
     * Get offline statistics
     */
    suspend fun getOfflineStats(): OfflineStats = withContext(Dispatchers.IO) {
        OfflineStats(
            cachedLectures = cache.getCachedLectureCount(),
            cachedCourses = cache.getCachedCoursesCount(),
            cachedUnits = cache.getCachedUnitsCount(),
            cacheSize = cache.getCacheSize(),
            lastSync = cache.getLastSyncTime()
        )
    }

    /**
     * Clear cache
     */
    suspend fun clearCache(): Boolean = withContext(Dispatchers.IO) {
        try {
            cache.clearAll()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing cache", e)
            false
        }
    }

    /**
     * Validate lecture code format
     */
    fun isValidLectureCode(code: String): Boolean {
        return parser.isValidLectureCode(code)
    }

    /**
     * Validate course code format
     */
    fun isValidCourseCode(code: String): Boolean {
        return parser.isValidCourseCode(code)
    }

    // Cache management

    private fun isCacheExpired(timestamp: Long): Boolean {
        val cacheExpiryTime = TimeUnit.HOURS.toMillis(24) // 24 hours
        return System.currentTimeMillis() - timestamp > cacheExpiryTime
    }

    // Student data methods (placeholders for future Janus integration)

    suspend fun authenticateStudent(username: String, password: String): CrawlerResult<JanusCrawler.StudentSession> {
        return parser.authenticateStudent(username, password)
    }

    suspend fun getStudentEnrollments(session: JanusCrawler.StudentSession): List<StudentEnrollment> {
        return parser.fetchStudentEnrollments(session)
    }

    suspend fun getStudentGrades(session: JanusCrawler.StudentSession): List<StudentGrade> {
        return parser.fetchStudentGrades(session)
    }

    suspend fun getAcademicHistory(session: JanusCrawler.StudentSession): StudentHistory? {
        return parser.fetchAcademicHistory(session)
    }

    suspend fun searchAvailableCourses(session: JanusCrawler.StudentSession, query: String): List<AvailableCourse> {
        return parser.searchAvailableCourses(session, query)
    }
}

// Cache-related data classes

data class OfflineStats(
    val cachedLectures: Int,
    val cachedCourses: Int,
    val cachedUnits: Int,
    val cacheSize: Long, // in bytes
    val lastSync: Long? // timestamp
)

data class CachedLecture(
    val lecture: Lecture,
    val timestamp: Long
)

data class CachedCourses(
    val unitCode: String,
    val courses: List<Course>,
    val timestamp: Long
)

data class CachedCampusUnits(
    val units: Map<String, List<String>>,
    val timestamp: Long
)

data class CachedCurriculum(
    val courseCode: String,
    val unitCode: String,
    val course: Course,
    val timestamp: Long
)

data class CachedSearchResults(
    val query: String,
    val filters: SearchService.SearchFilters,
    val results: List<SearchService.SearchResult<Lecture>>,
    val timestamp: Long
)
