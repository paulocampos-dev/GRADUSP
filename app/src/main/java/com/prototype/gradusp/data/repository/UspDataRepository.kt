package com.prototype.gradusp.data.repository

import android.content.Context
import android.os.Build
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
import java.io.File
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

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
            return file.reader().use {
                gson.fromJson(it, Lecture::class.java)
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
            return file.reader().use {
                val type = object : TypeToken<List<Course>>() {}.type
                gson.fromJson(it, type)
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

            // Track progress through units (20% to 70% of total progress)
            for ((index, unit) in unitsToProcess.withIndex()) {
                val unitProgress = 0.2f + (0.5f * (index.toFloat() / unitsToProcess.size.toFloat()))
                _updateProgress.value = unitProgress

                val unitCode = parser.getUnitCode(unit) ?: continue
                val courses = parser.fetchCoursesForUnit(unitCode)

                // Save courses
                File(context.filesDir, "courses/${unitCode}.json").writer().use {
                    gson.toJson(courses, it)
                }

                // Fetch and save a sample of lectures from each course
                val coursesToProcess = courses.take(if (selectedSchools.isEmpty()) 2 else 5) // More courses if specifically selected
                for ((courseIndex, course) in coursesToProcess.withIndex()) {
                    // Update progress for each course
                    val courseProgress = unitProgress +
                            (0.5f / unitsToProcess.size) * (courseIndex.toFloat() / coursesToProcess.size.toFloat())
                    _updateProgress.value = courseProgress

                    for ((_, lectures) in course.periods) {
                        val lecturesToProcess = lectures.take(if (selectedSchools.isEmpty()) 3 else 6) // More lectures if specifically selected
                        for ((lectureIndex, lectureInfo) in lecturesToProcess.withIndex()) {
                            // Fine-grained progress updates for lectures
                            val lectureProgress = courseProgress +
                                    (0.5f / unitsToProcess.size / coursesToProcess.size.toFloat()) *
                                    (lectureIndex.toFloat() / lecturesToProcess.size.toFloat())
                            _updateProgress.value = Math.min(0.9f, lectureProgress) // Cap at 90%

                            val lecture = parser.fetchLecture(lectureInfo.code) ?: continue

                            // Save lecture
                            File(context.filesDir, "lectures/${lecture.code}.json").writer().use {
                                gson.toJson(lecture, it)
                            }
                        }
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
            // Update failed
            context.dataStore.edit { preferences ->
                preferences[UPDATE_IN_PROGRESS_KEY] = false
            }
            _updateProgress.value = 0f
            return false
        }
    }
}