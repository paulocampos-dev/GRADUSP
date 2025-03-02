package com.prototype.gradusp.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.prototype.gradusp.data.dataStore
import com.prototype.gradusp.data.model.CourseGrade
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GradesRepository @Inject constructor(
    private val context: Context
) {
    companion object {
        private val GRADES_KEY = stringPreferencesKey("user_grades")
    }

    private val gson = Gson()

    /**
     * Flow of all courses with their grades
     */
    val coursesFlow: Flow<List<CourseGrade>> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(androidx.datastore.preferences.core.emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val gradesJson = preferences[GRADES_KEY] ?: "[]"
            val type = object : TypeToken<List<CourseGrade>>() {}.type
            gson.fromJson(gradesJson, type)
        }

    /**
     * Save the list of courses
     */
    suspend fun saveCourses(courses: List<CourseGrade>) {
        context.dataStore.edit { preferences ->
            val gradesJson = gson.toJson(courses)
            preferences[GRADES_KEY] = gradesJson
        }
    }
}