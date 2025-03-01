package com.prototype.gradusp.data.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.prototype.gradusp.data.model.Course
import com.prototype.gradusp.data.model.Lecture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileReader
import java.io.FileWriter

suspend fun saveDataToLocalStorage(context: Context, campusUnits: Map<String, List<String>>) {
    withContext(Dispatchers.IO) {
        try {
            val sharedPrefs = context.getSharedPreferences("usp_parser_data", Context.MODE_PRIVATE)
            val json = Gson().toJson(campusUnits)
            sharedPrefs.edit().putString("campus_units", json).apply()
        } catch (e: Exception) {
            Log.e("UspParser", "Error saving campus units to local storage", e)
        }
    }
}

suspend fun loadCampusUnitsFromLocalStorage(context: Context): Map<String, List<String>>? {
    return withContext(Dispatchers.IO) {
        try {
            val sharedPrefs = context.getSharedPreferences("usp_parser_data", Context.MODE_PRIVATE)
            val json = sharedPrefs.getString("campus_units", null) ?: return@withContext null

            val type = object : TypeToken<Map<String, List<String>>>() {}.type
            return@withContext Gson().fromJson<Map<String, List<String>>>(json, type)
        } catch (e: Exception) {
            Log.e("UspParser", "Error loading campus units from local storage", e)
            return@withContext null
        }
    }
}

suspend fun saveCourseToLocalStorage(context: Context, course: Course) {
    withContext(Dispatchers.IO) {
        try {
            val file = File(context.filesDir, "courses/${course.code}.json")
            file.parentFile?.mkdirs()

            FileWriter(file).use { writer ->
                Gson().toJson(course, writer)
            }
        } catch (e: Exception) {
            Log.e("UspParser", "Error saving course to local storage", e)
        }
    }
}

suspend fun loadCourseFromLocalStorage(context: Context, courseCode: String): Course? {
    return withContext(Dispatchers.IO) {
        try {
            val file = File(context.filesDir, "courses/$courseCode.json")
            if (!file.exists()) return@withContext null

            FileReader(file).use { reader ->
                return@withContext Gson().fromJson(reader, Course::class.java)
            }
        } catch (e: Exception) {
            Log.e("UspParser", "Error loading course from local storage", e)
            return@withContext null
        }
    }
}

suspend fun saveLectureToLocalStorage(context: Context, lecture: Lecture) {
    withContext(Dispatchers.IO) {
        try {
            val file = File(context.filesDir, "lectures/${lecture.code}.json")
            file.parentFile?.mkdirs()

            FileWriter(file).use { writer ->
                Gson().toJson(lecture, writer)
            }
        } catch (e: Exception) {
            Log.e("UspParser", "Error saving lecture to local storage", e)
        }
    }
}

suspend fun loadLectureFromLocalStorage(context: Context, lectureCode: String): Lecture? {
    return withContext(Dispatchers.IO) {
        try {
            val file = File(context.filesDir, "lectures/$lectureCode.json")
            if (!file.exists()) return@withContext null

            FileReader(file).use { reader ->
                return@withContext Gson().fromJson(reader, Lecture::class.java)
            }
        } catch (e: Exception) {
            Log.e("UspParser", "Error loading lecture from local storage", e)
            return@withContext null
        }
    }
}