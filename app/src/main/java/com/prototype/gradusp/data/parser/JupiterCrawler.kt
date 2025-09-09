package com.prototype.gradusp.data.parser

import android.util.Log
import com.prototype.gradusp.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.jsoup.nodes.Document
import java.io.IOException

/**
 * JupiterWeb crawler for academic data (courses, lectures, classrooms).
 * Handles parsing of JupiterWeb pages from USP's academic system.
 */
class JupiterCrawler(
    override val client: OkHttpClient
) : BaseCrawler {

    override val tag: String = "JupiterCrawler"

    private val coursePageParser = CoursePageParser()
    private val lecturePageParser = LecturePageParser()

    companion object {
        private const val BASE_URL = "https://uspdigital.usp.br/jupiterweb"
        private val DISCIPLINE_REGEX = Regex("sgldis=([A-Z0-9]+)")
        private val UNIT_CODE_REGEX = Regex("codcg=(\\d+)")
        private val COURSE_CODE_REGEX = Regex("codcur=(\\d+)&codhab=(\\d+)")
    }

    /**
     * Fetches all available units/colleges from JupiterWeb
     */
    suspend fun fetchUnits(): CrawlerResult<Map<String, String>> = withContext(Dispatchers.IO) {
        try {
            val url = "$BASE_URL/jupColegiadoLista?tipo=T"
            val document = fetchDocument(url) ?: return@withContext CrawlerResult.Error("Failed to fetch units document")

            val units = mutableMapOf<String, String>()
            document.select("a[href~=jupColegiadoMenu]").forEach { link ->
                val unitName = safeText(link)
                val href = safeAttr(link, "href")
                UNIT_CODE_REGEX.find(href)?.let { match ->
                    val unitCode = match.groupValues[1]
                    units[unitName] = unitCode
                }
            }

            CrawlerResult.Success(units)
        } catch (e: Exception) {
            Log.e(tag, "Error fetching units", e)
            CrawlerResult.Error("Failed to fetch units", e)
        }
    }

    /**
     * Fetches all disciplines for a specific unit
     */
    suspend fun fetchDisciplinesForUnit(unitCode: String): CrawlerResult<List<String>> = withContext(Dispatchers.IO) {
        try {
            val url = "$BASE_URL/jupDisciplinaLista?letra=A-Z&tipo=T&codcg=$unitCode"
            val document = fetchDocument(url) ?: return@withContext CrawlerResult.Error("Failed to fetch disciplines document")

            val disciplines = document.select("a[href~=obterTurma]").mapNotNull { link ->
                val href = safeAttr(link, "href")
                DISCIPLINE_REGEX.find(href)?.groupValues?.get(1)
            }.distinct()

            CrawlerResult.Success(disciplines)
        } catch (e: Exception) {
            Log.e(tag, "Error fetching disciplines for unit $unitCode", e)
            CrawlerResult.Error("Failed to fetch disciplines", e)
        }
    }

    /**
     * Fetches all courses for a specific unit
     */
    suspend fun fetchCoursesForUnit(unitCode: String): CrawlerResult<List<Course>> = withContext(Dispatchers.IO) {
        try {
            val url = "$BASE_URL/jupCursoLista?tipo=N&codcg=$unitCode"
            val document = fetchDocument(url) ?: return@withContext CrawlerResult.Error("Failed to fetch courses document")

            coroutineScope {
                val courses = document.select("a[href~=listarGradeCurricular]").map { link ->
                    async {
                        val href = safeAttr(link, "href")
                        val period = safeText(link.parent()?.parent()?.children()?.lastOrNull())
                        fetchCourseDetails(href, period)
                    }
                }.awaitAll().filterNotNull()

                CrawlerResult.Success(courses)
            }
        } catch (e: Exception) {
            Log.e(tag, "Error fetching courses for unit $unitCode", e)
            CrawlerResult.Error("Failed to fetch courses", e)
        }
    }

    /**
     * Fetches detailed information about a specific course
     */
    private suspend fun fetchCourseDetails(courseLink: String, period: String): Course? = withContext(Dispatchers.IO) {
        try {
            val fullUrl = "$BASE_URL/$courseLink"
            val document = fetchDocument(fullUrl) ?: return@withContext null

            coursePageParser.parse(document, fullUrl, period)
        } catch (e: Exception) {
            Log.e(tag, "Error fetching course details: $courseLink", e)
            null
        }
    }

    /**
     * Fetches detailed information about a specific lecture including classrooms
     */
    suspend fun fetchLectureDetails(lectureCode: String): CrawlerResult<Lecture> = withContext(Dispatchers.IO) {
        try {
            val infoUrl = "$BASE_URL/obterDisciplina?print=true&sgldis=$lectureCode"
            val classroomsUrl = "$BASE_URL/obterTurma?print=true&sgldis=$lectureCode"

            val infoDoc = fetchDocument(infoUrl)
            val classroomsDoc = fetchDocument(classroomsUrl)

            if (infoDoc == null || classroomsDoc == null) {
                return@withContext CrawlerResult.Error("Failed to fetch lecture documents")
            }

            val lecture = lecturePageParser.parse(infoDoc, classroomsDoc, lectureCode)
            CrawlerResult.Success(lecture)
        } catch (e: Exception) {
            Log.e(tag, "Error fetching lecture $lectureCode", e)
            CrawlerResult.Error("Failed to fetch lecture details", e)
        }
    }

    /**
     * Searches for lectures by code or name across all units
     */
    suspend fun searchLectures(query: String, units: Map<String, String>): CrawlerResult<List<Lecture>> = withContext(Dispatchers.IO) {
        try {
            val allLectures = mutableListOf<Lecture>()

            coroutineScope {
                val searchTasks = units.values.map { unitCode ->
                    async {
                        fetchDisciplinesForUnit(unitCode).getOrNull()?.let { disciplineCodes ->
                            disciplineCodes.mapNotNull { code ->
                                if (code.contains(query, ignoreCase = true)) {
                                    fetchLectureDetails(code).getOrNull()
                                } else null
                            }
                        } ?: emptyList()
                    }
                }

                val results = searchTasks.awaitAll()
                results.forEach { lectures ->
                    allLectures.addAll(lectures)
                }
            }

            CrawlerResult.Success(allLectures.distinctBy { it.code })
        } catch (e: Exception) {
            Log.e(tag, "Error searching lectures", e)
            CrawlerResult.Error("Failed to search lectures", e)
        }
    }

    /**
     * Fetches curriculum information for a specific course
     */
    suspend fun fetchCurriculum(courseCode: String, unitCode: String): CrawlerResult<Course> = withContext(Dispatchers.IO) {
        try {
            val url = "$BASE_URL/listarGradeCurricular?codcg=$unitCode&codcur=$courseCode&codhab=1"
            val document = fetchDocument(url) ?: return@withContext CrawlerResult.Error("Failed to fetch curriculum document")

            val course = coursePageParser.parse(document, url, "")
            CrawlerResult.Success(course)
        } catch (e: Exception) {
            Log.e(tag, "Error fetching curriculum for course $courseCode", e)
            CrawlerResult.Error("Failed to fetch curriculum", e)
        }
    }
}
