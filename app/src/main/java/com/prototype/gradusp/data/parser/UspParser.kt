package com.prototype.gradusp.data.parser

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.IOException
import java.util.concurrent.TimeUnit
import com.prototype.gradusp.data.model.Classroom
import com.prototype.gradusp.data.model.Course
import com.prototype.gradusp.data.model.Lecture
import com.prototype.gradusp.data.model.LectureInfo
import com.prototype.gradusp.data.model.Schedule

class UspParser(private val context: Context) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    // Campus mapping similar to Python script
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

    companion object {
        private val CODE_REGEX = Regex("codcur=(.+?)&codhab=(.+?)(&|$)")
        private val COURSE_NAME_REGEX = Regex("Curso:\\s*(.+)\\s*")
        private val UNIT_CODE_REGEX = Regex("codcg=([1-9]+)")
        private val PERIOD_REGEX = Regex("([0-9]+)º Período Ideal")
    }

    suspend fun fetchCampusUnits(): Map<String, List<String>> = withContext(Dispatchers.IO) {
        val campusMap = mutableMapOf<String, MutableList<String>>()

        try {
            val request = Request.Builder()
                .url("https://uspdigital.usp.br/jupiterweb/jupColegiadoLista?tipo=T")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Unexpected response $response")

                val bodyString = response.body?.string() ?: ""
                val document = Jsoup.parse(bodyString)
                val links = document.select("a[href~=jupColegiadoMenu]")

                links.forEach { link ->
                    val unitName = link.text()
                    val unitCodeMatch = Regex("codcg=(\\d+)").find(link.attr("href"))

                    if (unitCodeMatch != null) {
                        val unitCode = unitCodeMatch.groupValues[1]
                        unitCodes[unitName] = unitCode

                        // Determine campus for this unit
                        val unitCodeInt = unitCode.toInt()
                        val campus = campusByUnit.entries.find { (codes, _) ->
                            codes.contains(unitCodeInt)
                        }?.value ?: "Outro"

                        campusMap.getOrPut(campus) { mutableListOf() }.add(unitName)
                    }
                }
            }

            return@withContext campusMap
        } catch (e: Exception) {
            Log.e("UspParser", "Error fetching campus units", e)
            return@withContext emptyMap<String, List<String>>()
        }
    }

    // COURSES PARSERS
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    suspend fun fetchCoursesForUnit(unitCode: String): List<Course> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://uspdigital.usp.br/jupiterweb/jupCursoLista?tipo=N&codcg=$unitCode")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Unexpected response $response")

                val bodyString = response.body?.string() ?: ""
                val document = Jsoup.parse(bodyString)
                val links = document.select("a[href~=listarGradeCurricular]")

                // Fetch course details concurrently
                val coursesDeferred = coroutineScope {
                    links.map { link ->
                        async {
                            val courseLink = link.attr("href")
                            val periodElement = link.parent()?.parent()?.children()?.lastOrNull()
                            val period = periodElement?.text()?.trim() ?: ""
                            fetchCourseDetails(courseLink, period)
                        }
                    }
                }
                return@withContext coursesDeferred.awaitAll().filterNotNull()
            }
        } catch (e: Exception) {
            Log.e("UspParser", "Error fetching courses for unit $unitCode", e)
            return@withContext emptyList<Course>()
        }
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private suspend fun fetchCourseDetails(courseLink: String, period: String): Course? = withContext(Dispatchers.IO) {
        try {
            val fullUrl = "https://uspdigital.usp.br/jupiterweb/$courseLink"
            val request = Request.Builder()
                .url(fullUrl)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Unexpected response $response")

                val bodyString = response.body?.string() ?: ""
                val document = Jsoup.parse(bodyString)

                // Extract course information
                val codeMatch = CODE_REGEX.find(fullUrl)
                val code = if (codeMatch != null) "${codeMatch.groupValues[1]}-${codeMatch.groupValues[2]}" else ""

                val courseText = document.text()
                val nameMatches = COURSE_NAME_REGEX.findAll(courseText)
                val name = nameMatches.map { it.groupValues[1] }.joinToString(" - ")

                val unitCodeMatch = UNIT_CODE_REGEX.find(fullUrl)
                val unitCodeVal = unitCodeMatch?.groupValues?.get(1) ?: ""
                val unit = unitCodes.entries.find { it.value == unitCodeVal }?.key ?: ""

                // Parse periods and lectures
                val periods = mutableMapOf<String, List<LectureInfo>>()
                val periodTables = document.select("table").filter { table ->
                    table.text().contains("Disciplinas Obrigatórias")
                }

                if (periodTables.isNotEmpty()) {
                    val periodMap = parsePeriods(periodTables.first())
                    periods.putAll(periodMap)
                }

                return@withContext Course(code, name, unit, period, periods)
            }
        } catch (e: Exception) {
            Log.e("UspParser", "Error fetching course details: $courseLink", e)
            return@withContext null
        }
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private fun parsePeriods(table: Element): Map<String, List<LectureInfo>> {
        val periods = mutableMapOf<String, MutableList<LectureInfo>>()

        val typeMap = mapOf(
            "Disciplinas Obrigatórias" to "obrigatoria",
            "Disciplinas Optativas Eletivas" to "optativa_eletiva",
            "Disciplinas Optativas Livres" to "optativa_livre"
        )

        var currentType = ""
        var currentPeriod = ""

        table.select("tr").forEach { row ->
            val rowText = row.text().replace("\\s+".toRegex(), " ").trim()

            // Check if this is a type header
            typeMap[rowText]?.let {
                currentType = it
                return@forEach
            }

            // Check if this is a period header
            val periodMatch = PERIOD_REGEX.find(rowText)
            if (periodMatch != null) {
                currentPeriod = periodMatch.groupValues[1]
                periods.getOrPut(currentPeriod) { mutableListOf() }
                return@forEach
            }

            // Parse lecture info
            val cells = row.select("td")
            if (cells.isNotEmpty()) {
                val firstCellText = cells.first()?.text()?.trim() ?: ""

                if (firstCellText.length == 7) {
                    // This is a new lecture
                    periods[currentPeriod]?.add(
                        LectureInfo(
                            code = firstCellText,
                            type = currentType
                        )
                    )
                } else if (cells.size >= 2) {
                    // This could be a requisite
                    val secondCellText = cells[1].text().trim()
                    val lastLecture = periods[currentPeriod]?.lastOrNull()

                    if (lastLecture != null) {
                        when (secondCellText) {
                            "Requisito fraco" -> {
                                val newReqWeak = lastLecture.reqWeak.toMutableList()
                                newReqWeak.add(firstCellText.substring(0, 7))
                                periods[currentPeriod]?.removeLast()
                                periods[currentPeriod]?.add(lastLecture.copy(reqWeak = newReqWeak))
                            }
                            "Requisito" -> {
                                val newReqStrong = lastLecture.reqStrong.toMutableList()
                                newReqStrong.add(firstCellText.substring(0, 7))
                                periods[currentPeriod]?.removeLast()
                                periods[currentPeriod]?.add(lastLecture.copy(reqStrong = newReqStrong))
                            }
                            "Indicação de Conjunto" -> {
                                val newIndConj = lastLecture.indConjunto.toMutableList()
                                newIndConj.add(firstCellText.substring(0, 7))
                                periods[currentPeriod]?.removeLast()
                                periods[currentPeriod]?.add(lastLecture.copy(indConjunto = newIndConj))
                            }
                        }
                    }
                }
            }
        }

        return periods
    }

    // LECTURE PARSERS
    suspend fun fetchLecture(lectureCode: String): Lecture? = withContext(Dispatchers.IO) {
        try {
            // First fetch basic info
            val infoRequest = Request.Builder()
                .url("https://uspdigital.usp.br/jupiterweb/obterDisciplina?print=true&sgldis=$lectureCode")
                .build()

            var lecture: Lecture? = null

            client.newCall(infoRequest).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Unexpected response $response")

                val bodyString = response.body?.string() ?: ""
                val document = Jsoup.parse(bodyString)
                lecture = parseLectureInfo(document, lectureCode)
            }

            if (lecture == null) return@withContext null

            // Then fetch classrooms
            val classroomsRequest = Request.Builder()
                .url("https://uspdigital.usp.br/jupiterweb/obterTurma?print=true&sgldis=$lectureCode")
                .build()

            client.newCall(classroomsRequest).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Unexpected response $response")

                val bodyString = response.body?.string() ?: ""
                val document = Jsoup.parse(bodyString)
                val classrooms = parseClassrooms(document)

                return@withContext lecture!!.copy(classrooms = classrooms)
            }
        } catch (e: Exception) {
            Log.e("UspParser", "Error fetching lecture $lectureCode", e)
            return@withContext null
        }
    }

    private fun parseLectureInfo(document: Document, lectureCode: String): Lecture? {
        val tables = document.select("table")

        var unit = ""
        var department = ""
        var campus = ""
        var name = ""
        var objectives = ""
        var summary = ""
        var lectureCredits = 0
        var workCredits = 0

        tables.forEach { table ->
            val headerText = table.text()

            // Parse header info
            if (headerText.contains("Disciplina:")) {
                val rows = table.select("tr")
                if (rows.size >= 3) {
                    unit = rows[0].text().trim()
                    department = rows[1].text().trim()

                    // Determine campus from unit code
                    val unitCodeInt = unitCodes[unit]?.toIntOrNull() ?: 0
                    campus = if (unitCodeInt > 0) {
                        campusByUnit.entries.find { (codes, _) ->
                            codes.contains(unitCodeInt)
                        }?.value ?: "Outro"
                    } else {
                        "Outro"
                    }

                    val disciplineMatch = Regex("Disciplina:\\s+([A-Z0-9\\s]{7})\\s-\\s(.+)").find(rows[2].text())
                    if (disciplineMatch != null) {
                        name = disciplineMatch.groupValues[2].trim()
                    }
                }
            }
            // Parse objectives
            else if (headerText.startsWith("Objetivos")) {
                val rows = table.select("tr")
                if (rows.size >= 2) {
                    objectives = rows[1].text().trim()
                }
            }
            // Parse program summary
            else if (headerText.startsWith("Programa Resumido")) {
                val rows = table.select("tr")
                if (rows.size >= 2) {
                    summary = rows[1].text().trim()
                }
            }
            // Parse credits
            else if (headerText.contains("Créditos Aula")) {
                val rows = table.select("tr")
                rows.forEach { row ->
                    val cells = row.select("td")
                    if (cells.size >= 2) {
                        val cellText = cells[0].text().trim()
                        if (cellText.contains("Créditos Aula")) {
                            lectureCredits = cells[1].text().trim().toIntOrNull() ?: 0
                        } else if (cellText.contains("Créditos Trabalho")) {
                            workCredits = cells[1].text().trim().toIntOrNull() ?: 0
                        }
                    }
                }
            }
        }

        return Lecture(
            code = lectureCode,
            name = name,
            unit = unit,
            department = department,
            campus = campus,
            objectives = objectives,
            summary = summary,
            lectureCredits = lectureCredits,
            workCredits = workCredits
        )
    }

    private fun parseClassrooms(document: Document): List<Classroom> {
        val tables = document.select("table")
        val classrooms = mutableListOf<Classroom>()

        var currentClassroom: Classroom? = null
        var currentSchedules: MutableList<Schedule>? = null

        tables.forEach { table ->
            val headerText = table.text()

            // Check if this is a classroom info table
            if (headerText.contains("Código da Turma")) {
                // Save previous classroom if exists
                if (currentClassroom != null && currentSchedules != null) {
                    classrooms.add(currentClassroom!!.copy(schedules = currentSchedules!!))
                }

                // Start parsing new classroom
                currentClassroom = parseClassroomInfo(table)
                currentSchedules = mutableListOf()
            }
            // Check if this is a schedule table
            else if (headerText.contains("Horário") && currentClassroom != null) {
                currentSchedules = parseClassroomSchedules(table)
            }
        }

        // Add the last classroom
        if (currentClassroom != null && currentSchedules != null) {
            classrooms.add(currentClassroom!!.copy(schedules = currentSchedules!!))
        }

        return classrooms
    }

    private fun parseClassroomInfo(table: Element): Classroom {
        var code = ""
        var startDate = ""
        var endDate = ""
        var observations = ""

        table.select("tr").forEach { row ->
            val cells = row.select("td")
            if (cells.size >= 2) {
                val label = cells[0].text().trim()
                val value = cells[1].text().trim()

                when {
                    label.contains("Código da Turma") && !label.contains("Teórica") -> {
                        val codeMatch = Regex("^(\\w+)").find(value)
                        if (codeMatch != null) {
                            code = codeMatch.groupValues[1]
                        }
                    }
                    label.contains("Início") -> startDate = value
                    label.contains("Fim") -> endDate = value
                    label.contains("Observações") -> observations = value
                }
            }
        }

        return Classroom(
            code = code,
            startDate = startDate,
            endDate = endDate,
            observations = observations
        )
    }

    private fun parseClassroomSchedules(table: Element): MutableList<Schedule> {
        val schedules = mutableListOf<Schedule>()
        var currentSchedule: Schedule? = null

        table.select("tr").forEach { row ->
            val cells = row.select("td")
            if (cells.size >= 4) {
                val day = cells[0].text().trim()
                val startTime = cells[1].text().trim()
                val endTime = cells[2].text().trim()
                val teacher = cells[3].text().trim()

                if (day.isNotEmpty() && startTime.isNotEmpty() && endTime.isNotEmpty()) {
                    if (currentSchedule != null) {
                        schedules.add(currentSchedule!!)
                    }
                    currentSchedule = Schedule(
                        day = day,
                        startTime = startTime,
                        endTime = endTime,
                        teachers = mutableListOf(teacher)
                    )
                } else if (day.isEmpty() && startTime.isEmpty() && teacher.isNotEmpty() && currentSchedule != null) {
                    val updatedTeachers = currentSchedule!!.teachers.toMutableList()
                    updatedTeachers.add(teacher)
                    val updatedEndTime = if (endTime.isNotEmpty() && endTime > currentSchedule!!.endTime)
                        endTime else currentSchedule!!.endTime
                    currentSchedule = currentSchedule!!.copy(
                        endTime = updatedEndTime,
                        teachers = updatedTeachers
                    )
                } else if (day.isEmpty() && startTime.isNotEmpty() && currentSchedule != null) {
                    schedules.add(currentSchedule!!)
                    currentSchedule = Schedule(
                        day = currentSchedule!!.day,
                        startTime = startTime,
                        endTime = endTime,
                        teachers = mutableListOf(teacher)
                    )
                }
            }
        }

        if (currentSchedule != null) {
            schedules.add(currentSchedule!!)
        }

        return schedules
    }

    fun getUnitCode(unitName: String): String? {
        return unitCodes[unitName]
    }

    // Get a sample lecture from a unit
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    suspend fun getSampleLecture(unitCode: String): Lecture? = withContext(Dispatchers.IO) {
        try {
            val courses = fetchCoursesForUnit(unitCode)
            val course = courses.firstOrNull() ?: return@withContext null
            val firstPeriod = course.periods.entries.firstOrNull() ?: return@withContext null
            val lectureInfo = firstPeriod.value.firstOrNull() ?: return@withContext null
            return@withContext fetchLecture(lectureInfo.code)
        } catch (e: Exception) {
            Log.e("UspParser", "Error getting sample lecture", e)
            return@withContext null
        }
    }
}
