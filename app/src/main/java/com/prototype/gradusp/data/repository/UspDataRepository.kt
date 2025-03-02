package com.prototype.gradusp.data.repository

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.prototype.gradusp.data.dataStore
import com.prototype.gradusp.data.model.Course
import com.prototype.gradusp.data.model.Lecture
import com.prototype.gradusp.data.parser.UspParser
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.prototype.gradusp.data.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UspDataRepository @Inject constructor(
    private val context: Context,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    private val parser = UspParser(context)
    private val gson = Gson()

    companion object {
        private val LAST_UPDATE_KEY = longPreferencesKey("usp_last_update")
        private val UPDATE_IN_PROGRESS_KEY = booleanPreferencesKey("usp_update_in_progress")
        private val CAMPUS_UNITS_KEY = stringPreferencesKey("campus_units")
        private const val TAG = "UspDataRepository"
    }

    val lastUpdateTime: Flow<Long> = context.dataStore.data
        .map { preferences -> preferences[LAST_UPDATE_KEY] ?: 0L }

    val isUpdateInProgress: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[UPDATE_IN_PROGRESS_KEY] ?: false }

    // Add progress tracking
    private val _updateProgress = MutableStateFlow(0f)
    val updateProgress: StateFlow<Float> = _updateProgress

    suspend fun getCampusUnits(): Map<String, List<String>> {
        // Try to load from preferences first
        val preferences = context.dataStore.data.first()
        val campusUnitsJson = preferences[CAMPUS_UNITS_KEY]

        if (!campusUnitsJson.isNullOrEmpty()) {
            val type = object : TypeToken<Map<String, List<String>>>() {}.type
            return gson.fromJson(campusUnitsJson, type)
        }

        // If not in preferences, fetch from the parser
        return parser.fetchCampusUnits()
    }

    suspend fun getLecture(code: String): Lecture? {
        // Try to load from storage
        val file = File(context.filesDir, "lectures/${code}.json")
        if (file.exists()) {
            return withContext(Dispatchers.IO) {
                file.reader().use {
                    gson.fromJson(it, Lecture::class.java)
                }
            }
        }

        // If not in storage, fetch from the parser
        return parser.fetchLecture(code)
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    suspend fun getCourses(unitCode: String): List<Course> {
        // Try to load from storage
        val file = File(context.filesDir, "courses/${unitCode}.json")
        if (file.exists()) {
            return withContext(Dispatchers.IO) {
                file.reader().use {
                    val type = object : TypeToken<List<Course>>() {}.type
                    gson.fromJson(it, type)
                }
            }
        }

        // If not in storage, fetch from the parser
        return parser.fetchCoursesForUnit(unitCode)
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    suspend fun updateUspData(): Boolean {
        try {
            // Reset progress
            _updateProgress.value = 0f

            // Set update in progress flag
            context.dataStore.edit { preferences ->
                preferences[UPDATE_IN_PROGRESS_KEY] = true
            }

            // Fetch and store campus and units
            _updateProgress.value = 0.1f
            val campusUnits = parser.fetchCampusUnits()
            context.dataStore.edit { preferences ->
                preferences[CAMPUS_UNITS_KEY] = gson.toJson(campusUnits)
            }
            _updateProgress.value = 0.2f

            // CLEANUP STEP: Delete existing data directories and recreate them
            withContext(Dispatchers.IO) {
                // Delete courses directory
                val coursesDir = File(context.filesDir, "courses")
                if (coursesDir.exists()) {
                    coursesDir.deleteRecursively()
                }
                coursesDir.mkdirs()

                // Delete lectures directory
                val lecturesDir = File(context.filesDir, "lectures")
                if (lecturesDir.exists()) {
                    lecturesDir.deleteRecursively()
                }
                lecturesDir.mkdirs()

                Log.d("UspParser", "Cleaned up existing data directories")
            }

            // Create directories for storage
            File(context.filesDir, "courses").mkdirs()
            File(context.filesDir, "lectures").mkdirs()

            // Get selected schools from preferences
            val selectedSchools = userPreferencesRepository.selectedSchoolsFlow.first()

            // Determine which units to process
            val unitsToProcess = if (selectedSchools.isEmpty()) {
                // Process a sample if none selected (to avoid processing everything)
                campusUnits.values.flatten().take(3)
            } else {
                // Process only selected schools
                selectedSchools.toList()
            }

            // Total progress allocation:
            // - 20% for setup (0.0-0.2)
            // - 60% for processing units and their disciplines (0.2-0.8)
            // - 20% for finishing up (0.8-1.0)
            val progressPerUnit = 0.6f / unitsToProcess.size.toFloat()

            // Track progress through units
            for ((unitIndex, unit) in unitsToProcess.withIndex()) {
                val unitStartProgress = 0.2f + (progressPerUnit * unitIndex)
                _updateProgress.value = unitStartProgress

                val unitCode = parser.getUnitCode(unit) ?: continue
                Log.d(TAG, "Processing unit: $unit ($unitCode)")

                // Fetch courses for this unit
                val courses = parser.fetchCoursesForUnit(unitCode)

                // Save courses
                withContext(Dispatchers.IO) {
                    File(context.filesDir, "courses/${unitCode}.json").writer().use {
                        gson.toJson(courses, it)
                    }
                }

                // Fetch ALL disciplines for this unit directly
                val disciplineCodes = parser.fetchAllDisciplinesForUnit(unitCode)
                Log.d(TAG, "Found ${disciplineCodes.size} disciplines for $unit")

                // Process disciplines in parallel batches to avoid overwhelming the network
                val batchSize = 20
                val batches = disciplineCodes.chunked(batchSize)

                // Allocate progress for discipline processing within this unit
                val progressPerBatch = progressPerUnit / batches.size.toFloat()

                for ((batchIndex, batch) in batches.withIndex()) {
                    val batchProgress = unitStartProgress + (progressPerBatch * batchIndex)
                    _updateProgress.value = batchProgress

                    // Process each batch concurrently
                    coroutineScope {
                        val deferredResults = batch.map { code ->
                            async(Dispatchers.IO) {
                                try {
                                    val lecture = parser.fetchLecture(code)
                                    if (lecture != null) {
                                        File(context.filesDir, "lectures/${lecture.code}.json").writer().use {
                                            gson.toJson(lecture, it)
                                        }
                                    }
                                    true
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error fetching lecture $code", e)
                                    false
                                }
                            }
                        }
                        deferredResults.awaitAll()
                    }
                }
            }

            // Final progress before completing
            _updateProgress.value = 0.95f

            // Update last update time
            context.dataStore.edit { preferences ->
                preferences[LAST_UPDATE_KEY] = Date().time
                preferences[UPDATE_IN_PROGRESS_KEY] = false
            }

            // Complete progress
            _updateProgress.value = 1.0f

            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating USP data", e)
            // Update failed
            context.dataStore.edit { preferences ->
                preferences[UPDATE_IN_PROGRESS_KEY] = false
            }
            _updateProgress.value = 0f
            return false
        }
    }

    /**
     * Gets the count of available lectures
     */
    suspend fun getAvailableLecturesCount(): Int = withContext(Dispatchers.IO) {
        val lecturesDir = File(context.filesDir, "lectures")
        if (!lecturesDir.exists()) return@withContext 0

        val lectureFiles = lecturesDir.listFiles { file -> file.extension == "json" }
        return@withContext lectureFiles?.size ?: 0
    }

    /**
     * Gets lectures for a specific unit from local storage
     */
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    suspend fun getLecturesForUnit(unitCode: String): List<Lecture> = withContext(Dispatchers.IO) {
        val lecturesDir = File(context.filesDir, "lectures")
        if (!lecturesDir.exists()) return@withContext emptyList()

        // Get course information for this unit to find lecture codes
        val courses = getCourses(unitCode)
        val lectureCodes = mutableSetOf<String>()

        courses.forEach { course ->
            course.periods.forEach { (_, lectureInfos) ->
                lectureInfos.forEach { lectureInfo ->
                    lectureCodes.add(lectureInfo.code)
                }
            }
        }

        // Load lectures from storage
        return@withContext lectureCodes.mapNotNull { code ->
            val lectureFile = File(lecturesDir, "${code}.json")
            if (lectureFile.exists()) {
                try {
                    lectureFile.reader().use {
                        gson.fromJson(it, Lecture::class.java)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error reading lecture file: ${lectureFile.name}", e)
                    null
                }
            } else {
                null
            }
        }
    }

    /**
     * Searches for lectures across all stored lectures
     */
    suspend fun searchLectures(query: String, limit: Int = 50): List<Lecture> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()

        val lecturesDir = File(context.filesDir, "lectures")
        if (!lecturesDir.exists()) return@withContext emptyList()

        val lectureFiles = lecturesDir.listFiles { file -> file.extension == "json" } ?: return@withContext emptyList()

        // For large numbers of lectures, we should avoid loading all into memory
        // Instead, we'll load them one by one and filter as we go
        val results = mutableListOf<Lecture>()

        for (file in lectureFiles) {
            if (results.size >= limit) break

            try {
                val lecture = file.reader().use {
                    gson.fromJson(it, Lecture::class.java)
                }

                if (lecture.matchesSearch(query)) {
                    results.add(lecture)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reading lecture file during search: ${file.name}", e)
            }
        }

        return@withContext results
    }
}