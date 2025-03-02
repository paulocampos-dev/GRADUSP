// Create a new file: app/src/main/java/com/prototype/gradusp/data/repository/UspDataRepository.kt
package com.prototype.gradusp.data.repository

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.prototype.gradusp.data.dataStore
import com.prototype.gradusp.data.model.Course
import com.prototype.gradusp.data.model.Lecture
import com.prototype.gradusp.data.parser.UspParser
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

class UspDataRepository( private val context: Context ) {
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
            // Set update in progress flag
            context.dataStore.edit { preferences ->
                preferences[UPDATE_IN_PROGRESS_KEY] = true
            }

            // Fetch and store campus and units
            val campusUnits = parser.fetchCampusUnits()
            context.dataStore.edit { preferences ->
                preferences[CAMPUS_UNITS_KEY] = gson.toJson(campusUnits)
            }

            // Create directories for storage
            File(context.filesDir, "courses").mkdirs()
            File(context.filesDir, "lectures").mkdirs()

            // Fetch and store courses for each unit
            // (limiting to a few units to prevent excessive network usage)
            val sampleUnits = campusUnits.values.flatten().take(3) // Just 3 units for example
            for (unit in sampleUnits) {
                val unitCode = parser.getUnitCode(unit) ?: continue
                val courses = parser.fetchCoursesForUnit(unitCode)

                // Save courses
                File(context.filesDir, "courses/${unitCode}.json").writer().use {
                    gson.toJson(courses, it)
                }

                // Fetch and save a sample of lectures from each course
                for (course in courses.take(2)) { // Just 2 courses per unit
                    for ((_, lectures) in course.periods) {
                        for (lectureInfo in lectures.take(3)) { // Just 3 lectures per course
                            val lecture = parser.fetchLecture(lectureInfo.code) ?: continue

                            // Save lecture
                            File(context.filesDir, "lectures/${lecture.code}.json").writer().use {
                                gson.toJson(lecture, it)
                            }
                        }
                    }
                }
            }

            // Update last update time
            context.dataStore.edit { preferences ->
                preferences[LAST_UPDATE_KEY] = Date().time
                preferences[UPDATE_IN_PROGRESS_KEY] = false
            }

            return true
        } catch (e: Exception) {
            // Update failed
            context.dataStore.edit { preferences ->
                preferences[UPDATE_IN_PROGRESS_KEY] = false
            }
            return false
        }
    }
}

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@Composable
fun UspDataUpdateSection(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uspDataRepository = remember { UspDataRepository(context) }
    val coroutineScope = rememberCoroutineScope()

    // We'll manually collect the flows since we're not using a ViewModel
    var lastUpdateTime by remember { mutableStateOf(0L) }
    var isUpdateInProgress by remember { mutableStateOf(false) }
    var updateResult by remember { mutableStateOf<String?>(null) }

    // Collect the flows
    LaunchedEffect(uspDataRepository) {
        uspDataRepository.lastUpdateTime.collect { lastUpdateTime = it }
    }

    LaunchedEffect(uspDataRepository) {
        uspDataRepository.isUpdateInProgress.collect { isUpdateInProgress = it }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Dados da USP",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Last update time
        if (lastUpdateTime > 0) {
            val lastUpdateDate = Date(lastUpdateTime)
            val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))

            Text(
                text = "Última atualização: ${formatter.format(lastUpdateDate)}",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(8.dp))
        }

        // Update button
        Button(
            onClick = {
                updateResult = null
                coroutineScope.launch {
                    val success = uspDataRepository.updateUspData()
                    updateResult = if (success) {
                        "Dados atualizados com sucesso!"
                    } else {
                        "Falha ao atualizar dados. Tente novamente."
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isUpdateInProgress
        ) {
            if (isUpdateInProgress) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Text(
                text = if (isUpdateInProgress) "Atualizando..." else "Atualizar Dados da USP"
            )
        }

        // Update result message
        updateResult?.let {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = if (it.startsWith("Falha"))
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.primary
            )
        }

        // Info text
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Atualizar os dados da USP permite adicionar matérias mais rapidamente, sem precisar buscar online toda vez.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}