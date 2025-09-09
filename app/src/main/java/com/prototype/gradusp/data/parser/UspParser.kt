package com.prototype.gradusp.data.parser

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.prototype.gradusp.data.model.Course
import com.prototype.gradusp.data.model.Lecture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException

class UspParser(
    private val context: Context,
    private val client: OkHttpClient
) {

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

    private val unitCodes = mutableMapOf<String, String>()

    private val lecturePageParser = LecturePageParser(campusByUnit, unitCodes)
    private val coursePageParser = CoursePageParser()

    companion object {
        private val DISCIPLINE_REGEX = Regex("sgldis=([A-Z0-9]+)")
        private val TAG = "UspParser"
    }


    suspend fun fetchCampusUnits(): Map<String, List<String>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url("https://uspdigital.usp.br/jupiterweb/jupColegiadoLista?tipo=T").build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Unexpected response $response")
                val document = Jsoup.parse(response.body?.string() ?: "")
                val campusMap = mutableMapOf<String, MutableList<String>>()
                document.select("a[href~=jupColegiadoMenu]").forEach { link ->
                    val unitName = link.text()
                    Regex("codcg=(\\d+)").find(link.attr("href"))?.let { match ->
                        val unitCode = match.groupValues[1]
                        unitCodes[unitName] = unitCode
                        val campus = campusByUnit.entries.find { it.key.contains(unitCode.toInt()) }?.value ?: "Outro"
                        campusMap.getOrPut(campus) { mutableListOf() }.add(unitName)
                    }
                }
                return@withContext campusMap
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching campus units", e)
            return@withContext emptyMap()
        }
    }

    suspend fun fetchAllDisciplinesForUnit(unitCode: String): List<String> = withContext(Dispatchers.IO) {
        try {
            val url = "https://uspdigital.usp.br/jupiterweb/jupDisciplinaLista?letra=A-Z&tipo=T&codcg=$unitCode"
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Unexpected response $response")
                val document = Jsoup.parse(response.body?.string() ?: "")
                return@withContext document.select("a[href~=obterTurma]").mapNotNull {
                    DISCIPLINE_REGEX.find(it.attr("href"))?.groupValues?.get(1)
                }.toSet().toList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching all disciplines for unit $unitCode", e)
            return@withContext emptyList()
        }
    }

    suspend fun fetchCoursesForUnit(unitCode: String): List<Course> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url("https://uspdigital.usp.br/jupiterweb/jupCursoLista?tipo=N&codcg=$unitCode").build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Unexpected response $response")
                val document = Jsoup.parse(response.body?.string() ?: "")
                coroutineScope {
                    document.select("a[href~=listarGradeCurricular]").map { link ->
                        async {
                            val courseLink = link.attr("href")
                            val period = link.parent()?.parent()?.children()?.lastOrNull()?.text()?.trim() ?: ""
                            fetchCourseDetails(courseLink, period)
                        }
                    }.awaitAll().filterNotNull()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching courses for unit $unitCode", e)
            emptyList()
        }
    }

    private suspend fun fetchCourseDetails(courseLink: String, period: String): Course? = withContext(Dispatchers.IO) {
        try {
            val fullUrl = "https://uspdigital.usp.br/jupiterweb/$courseLink"
            val request = Request.Builder().url(fullUrl).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Unexpected response $response")
                val document = Jsoup.parse(response.body?.string() ?: "")
                return@withContext coursePageParser.parse(document, fullUrl, period)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching course details: $courseLink", e)
            null
        }
    }

    suspend fun fetchLecture(lectureCode: String): Lecture? = withContext(Dispatchers.IO) {
        try {
            val infoRequest = Request.Builder().url("https://uspdigital.usp.br/jupiterweb/obterDisciplina?print=true&sgldis=$lectureCode").build()
            val classroomsRequest = Request.Builder().url("https://uspdigital.usp.br/jupiterweb/obterTurma?print=true&sgldis=$lectureCode").build()

            val infoDoc: Document
            val classroomsDoc: Document

            client.newCall(infoRequest).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Unexpected response $response")
                infoDoc = Jsoup.parse(response.body?.string() ?: "")
            }
            client.newCall(classroomsRequest).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Unexpected response $response")
                classroomsDoc = Jsoup.parse(response.body?.string() ?: "")
            }

            return@withContext lecturePageParser.parse(infoDoc, classroomsDoc, lectureCode)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching lecture $lectureCode", e)
            return@withContext null
        }
    }

    fun getUnitCode(unitName: String): String? {
        return unitCodes[unitName]
    }
}