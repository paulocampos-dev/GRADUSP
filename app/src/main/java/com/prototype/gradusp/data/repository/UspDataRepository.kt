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
import okhttp3.OkHttpClient
import java.io.File
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

sealed class SyncResult {
    data object Success : SyncResult()
    data class Error(val message: String) : SyncResult()
}

@Singleton
class UspDataRepository @Inject constructor(
    private val context: Context,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()
    private val parser = UspParser(context, client)

    companion object {
        private val LAST_UPDATE_KEY = longPreferencesKey("usp_last_update")
        private val UPDATE_IN_PROGRESS_KEY = booleanPreferencesKey("usp_update_in_progress")
        private val CAMPUS_UNITS_KEY = stringPreferencesKey("campus_units")

        private const val LECTURES_DIR_NAME = "lectures"
        private const val COURSES_DIR_NAME = "courses"
        private const val TEMP_LECTURES_DIR_NAME = "lectures_temp"
        private const val TEMP_COURSES_DIR_NAME = "courses_temp"
        private const val LECTURE_FETCH_BATCH_SIZE = 20
        private const val TAG = "UspDataRepository"
    }

    val lastUpdateTime: Flow<Long> = context.dataStore.data.map { it[LAST_UPDATE_KEY] ?: 0L }
    val isUpdateInProgress: Flow<Boolean> = context.dataStore.data.map { it[UPDATE_IN_PROGRESS_KEY] ?: false }
    private val _updateProgress = MutableStateFlow(0f)
    val updateProgress: StateFlow<Float> = _updateProgress

    // ... getCampusUnits, getLecture, getCourses remain the same ...

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

    suspend fun updateUspData(): SyncResult {
        val tempLecturesDir = File(context.filesDir, TEMP_LECTURES_DIR_NAME)
        val tempCoursesDir = File(context.filesDir, TEMP_COURSES_DIR_NAME)

        try {
            _updateProgress.value = 0f
            context.dataStore.edit { it[UPDATE_IN_PROGRESS_KEY] = true }

            // Cleanup and create temp directories
            withContext(Dispatchers.IO) {
                if (tempLecturesDir.exists()) tempLecturesDir.deleteRecursively()
                if (tempCoursesDir.exists()) tempCoursesDir.deleteRecursively()
                tempLecturesDir.mkdirs()
                tempCoursesDir.mkdirs()
            }

            // --- Start Fetching Data ---
            _updateProgress.value = 0.1f
            val campusUnits = parser.fetchCampusUnits()
            context.dataStore.edit { it[CAMPUS_UNITS_KEY] = gson.toJson(campusUnits) }
            _updateProgress.value = 0.2f

            val selectedSchools = userPreferencesRepository.selectedSchoolsFlow.first()
            val unitsToProcess = if (selectedSchools.isEmpty()) campusUnits.values.flatten().take(3) else selectedSchools.toList()
            if (unitsToProcess.isEmpty()) return SyncResult.Error("Nenhuma unidade selecionada para atualização.")

            val progressPerUnit = 0.7f / unitsToProcess.size.toFloat()

            for ((unitIndex, unit) in unitsToProcess.withIndex()) {
                val unitCode = parser.getUnitCode(unit) ?: continue
                val unitStartProgress = 0.2f + (progressPerUnit * unitIndex)
                _updateProgress.value = unitStartProgress

                // Fetch and save courses to temp dir
                val courses = parser.fetchCoursesForUnit(unitCode)
                withContext(Dispatchers.IO) {
                    File(tempCoursesDir, "${unitCode}.json").writer().use { gson.toJson(courses, it) }
                }

                // Fetch and save all lectures for the unit to temp dir
                val disciplineCodes = parser.fetchAllDisciplinesForUnit(unitCode)
                val batches = disciplineCodes.chunked(LECTURE_FETCH_BATCH_SIZE)
                val progressPerBatch = progressPerUnit / batches.size.toFloat()

                for ((batchIndex, batch) in batches.withIndex()) {
                    _updateProgress.value = unitStartProgress + (progressPerBatch * batchIndex)
                    coroutineScope {
                        batch.map { code ->
                            async(Dispatchers.IO) {
                                parser.fetchLecture(code)?.let { lecture ->
                                    File(tempLecturesDir, "${lecture.code}.json").writer().use { gson.toJson(lecture, it) }
                                }
                            }
                        }.awaitAll()
                    }
                }
            }

            // --- Finalize: Atomic Swap ---
            _updateProgress.value = 0.95f
            withContext(Dispatchers.IO) {
                val lecturesDir = File(context.filesDir, LECTURES_DIR_NAME)
                val coursesDir = File(context.filesDir, COURSES_DIR_NAME)

                if (lecturesDir.exists()) lecturesDir.deleteRecursively()
                if (coursesDir.exists()) coursesDir.deleteRecursively()

                tempLecturesDir.renameTo(lecturesDir)
                tempCoursesDir.renameTo(coursesDir)
            }

            context.dataStore.edit {
                it[LAST_UPDATE_KEY] = Date().time
            }
            return SyncResult.Success
        } catch (e: Exception) {
            Log.e(TAG, "Error updating USP data", e)
            return SyncResult.Error(e.message ?: "Ocorreu um erro desconhecido.")
        } finally {
            withContext(Dispatchers.IO) {
                if (tempLecturesDir.exists()) tempLecturesDir.deleteRecursively()
                if (tempCoursesDir.exists()) tempCoursesDir.deleteRecursively()
            }
            _updateProgress.value = 0f
            context.dataStore.edit { it[UPDATE_IN_PROGRESS_KEY] = false }
        }
    }

    suspend fun getAvailableLecturesCount(): Int = withContext(Dispatchers.IO) {
        val lecturesDir = File(context.filesDir, "lectures")
        if (!lecturesDir.exists()) return@withContext 0

        val lectureFiles = lecturesDir.listFiles { file -> file.extension == "json" }
        return@withContext lectureFiles?.size ?: 0
    }

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