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
import com.prototype.gradusp.data.model.VacancyInfo

class UspParser(private val context: Context) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    // Campus mapping
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
        private val DISCIPLINE_REGEX = Regex("sgldis=([A-Z0-9]+)")
        private val TAG = "UspParser"
    }

    /**
     * Fetches all campus units and their codes
     */
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
            Log.e(TAG, "Error fetching campus units", e)
            return@withContext emptyMap<String, List<String>>()
        }
    }

    /**
     * Fetches all disciplines directly for a unit (comprehensive approach)
     */
    suspend fun fetchAllDisciplinesForUnit(unitCode: String): List<String> = withContext(Dispatchers.IO) {
        try {
            val url = "https://uspdigital.usp.br/jupiterweb/jupDisciplinaLista?letra=A-Z&tipo=T&codcg=$unitCode"
            val request = Request.Builder().url(url).build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Unexpected response $response")

                val bodyString = response.body?.string() ?: ""
                val document = Jsoup.parse(bodyString)

                // Parse the document similar to Main.kt
                val disciplineLinks = document.select("a[href~=obterTurma]")
                val disciplineCodes = mutableSetOf<String>()

                disciplineLinks.forEach { element ->
                    val href = element.attr("href")
                    val matchResult = DISCIPLINE_REGEX.find(href)
                    if (matchResult != null) {
                        val code = matchResult.groupValues[1]
                        disciplineCodes.add(code)
                    }
                }

                return@withContext disciplineCodes.toList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching all disciplines for unit $unitCode", e)
            return@withContext emptyList<String>()
        }
    }

    /**
     * Fetches courses for a unit (curriculum-based approach)
     */
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
            Log.e(TAG, "Error fetching courses for unit $unitCode", e)
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
            Log.e(TAG, "Error fetching course details: $courseLink", e)
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

    /**
     * Fetches lecture details given its code
     */
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

                // Process linked classrooms (theoretical and practical)
                val processedClassrooms = processLinkedClassrooms(classrooms)

                return@withContext lecture!!.copy(classrooms = processedClassrooms)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching lecture $lectureCode", e)
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
                    val unitCode = unitCodes[unit]?.toIntOrNull() ?: 0
                    campus = if (unitCode > 0) {
                        campusByUnit.entries.find { (codes, _) ->
                            codes.contains(unitCode)
                        }?.value ?: "Outro"
                    } else {
                        "Outro"
                    }

                    // More robust approach to extract the discipline name
                    for (i in 0 until rows.size) {
                        val row = rows[i]
                        val rowText = row.text().trim()
                        Log.d("UspParser", "Row $i text: '${rowText}'")

                        if (rowText.contains("Disciplina:")) {
                            // Try the specific pattern first
                            val disciplineMatch = Regex("Disciplina:\\s+([A-Z0-9]{7})\\s+-\\s+(.+)").find(rowText)
                            if (disciplineMatch != null) {
                                name = disciplineMatch.groupValues[2].trim()
                                Log.d("UspParser", "Pattern match extracted: '$name'")
                            } else {
                                // Fallback to a more generic approach
                                val parts = rowText.split("-")
                                if (parts.size > 1 && parts[0].contains(lectureCode)) {
                                    name = parts.drop(1).joinToString("-").trim()
                                    Log.d("UspParser", "Split approach extracted: '$name'")
                                } else {
                                    // Just get everything after "Disciplina:" as a last resort
                                    name = rowText.substringAfter("Disciplina:").trim()

                                    // Remove the code if present
                                    if (name.contains(lectureCode)) {
                                        name = name.substringAfter(lectureCode).trim()
                                        if (name.startsWith("-")) {
                                            name = name.substring(1).trim()
                                        }
                                    }
                                    Log.d("UspParser", "Fallback approach extracted: '$name'")
                                }
                            }
                            break
                        }
                    }

                    // Final fallback if name is still empty
                    if (name.isBlank()) {
                        Log.d("UspParser", "Unable to extract name, using code as name for $lectureCode")
                        name = lectureCode
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

        Log.d("UspParser", "Final parsed lecture info - Code: $lectureCode, Name: '$name', Unit: '$unit', Department: '$department'")

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
        val classrooms = mutableListOf<Classroom>()
        var currentClassroom: ClassroomBuilder? = null

        document.select("table").forEach { table ->
            val headerText = table.text()

            // Check if this is a classroom info table
            if (headerText.contains("Código da Turma")) {
                // Save previous classroom if exists
                currentClassroom?.let { builder ->
                    classrooms.add(builder.build())
                }

                // Start parsing new classroom
                currentClassroom = parseClassroomInfo(table)
            }
            // Check if this is a schedule table
            else if (headerText.contains("Horário") && currentClassroom != null) {
                val schedules = parseClassroomSchedules(table)
                currentClassroom!!.schedules.addAll(schedules)
            }
            // Check if this is a vacancy table
            else if (headerText.contains("Vagas") && currentClassroom != null) {
                val vacancies = parseVacancies(table)
                currentClassroom!!.vacancies.putAll(vacancies)
            }
        }

        // Add the last classroom
        currentClassroom?.let {
            classrooms.add(it.build())
        }

        return classrooms
    }

    private fun parseClassroomInfo(table: Element): ClassroomBuilder {
        val builder = ClassroomBuilder()

        table.select("tr").forEach { row ->
            val cells = row.select("td")
            if (cells.size >= 2) {
                val label = cells[0].text().trim()
                val value = cells[1].text().trim()

                when {
                    label.contains("Código da Turma") -> {
                        if (label.contains("Teórica")) {
                            builder.theoreticalCode = value.trim()
                        } else {
                            val codeMatch = Regex("^(\\w+)").find(value)
                            if (codeMatch != null) {
                                builder.code = codeMatch.groupValues[1]
                            }
                        }
                    }
                    label.contains("Início") -> builder.startDate = value
                    label.contains("Fim") -> builder.endDate = value
                    label.contains("Observações") -> builder.observations = value
                    label.contains("Tipo da Turma") -> builder.type = value
                }
            }
        }

        return builder
    }

    private fun parseClassroomSchedules(table: Element): List<Schedule> {
        val schedules = mutableListOf<Schedule>()
        var currentDay: String? = null
        var currentStartTime: String? = null
        var currentEndTime: String? = null
        var currentTeachers = mutableListOf<String>()

        table.select("tr").forEach { row ->
            val cells = row.select("td")

            if (cells.size < 2) return@forEach

            val day = cells[0].text().trim()
            val startTime = cells[1].text().trim()
            val endTime = if (cells.size > 2) cells[2].text().trim() else ""
            val teacher = if (cells.size > 3) cells[3].text().trim() else ""

            if (day.isNotEmpty()) {
                // A new schedule is starting
                // Save the previous one if it exists
                if (currentDay != null && currentStartTime != null && currentEndTime != null) {
                    schedules.add(Schedule(
                        day = currentDay!!,
                        startTime = currentStartTime!!,
                        endTime = currentEndTime!!,
                        teachers = currentTeachers.toList()
                    ))
                }

                // Start a new schedule
                currentDay = day
                currentStartTime = startTime
                currentEndTime = endTime
                currentTeachers = mutableListOf()
                if (teacher.isNotEmpty()) {
                    currentTeachers.add(teacher)
                }
            } else if (day.isEmpty() && startTime.isEmpty() && teacher.isNotEmpty()) {
                // This is a continuation of teachers for the current schedule
                currentTeachers.add(teacher)
                // If there's a new end time, update it
                if (endTime.isNotEmpty() && endTime > (currentEndTime ?: "")) {
                    currentEndTime = endTime
                }
            } else if (day.isEmpty() && startTime.isNotEmpty()) {
                // This is a new time slot on the same day
                if (currentDay != null && currentStartTime != null && currentEndTime != null) {
                    schedules.add(Schedule(
                        day = currentDay!!,
                        startTime = currentStartTime!!,
                        endTime = currentEndTime!!,
                        teachers = currentTeachers.toList()
                    ))
                }

                // Keep the same day, but update times
                currentStartTime = startTime
                currentEndTime = endTime
                currentTeachers = mutableListOf()
                if (teacher.isNotEmpty()) {
                    currentTeachers.add(teacher)
                }
            }
        }

        // Don't forget to add the last schedule
        if (currentDay != null && currentStartTime != null && currentEndTime != null) {
            schedules.add(Schedule(
                day = currentDay!!,
                startTime = currentStartTime!!,
                endTime = currentEndTime!!,
                teachers = currentTeachers.toList()
            ))
        }

        return schedules
    }

    private fun parseVacancies(table: Element): Map<String, VacancyInfo> {
        val vacancies = mutableMapOf<String, VacancyInfo>()
        var currentType: String? = null
        var currentVacancy: VacancyBuilder? = null

        table.select("tr").forEach { row ->
            val cells = row.select("td")
            if (cells.size < 2) return@forEach

            val firstCell = cells[0].text().trim()

            if (firstCell == "Vagas" || firstCell.isEmpty()) {
                // Header row - skip
                return@forEach
            }

            if (cells.size == 5 && !firstCell.isEmpty()) {
                // This is a new vacancy type (Obrigatória, Optativa, etc.)
                // Save previous vacancy if it exists
                if (currentType != null && currentVacancy != null) {
                    vacancies[currentType!!] = currentVacancy!!.build()
                }

                // Start a new vacancy type
                currentType = firstCell
                currentVacancy = VacancyBuilder()

                // Parse the vacancy numbers
                if (cells.size >= 5) {
                    currentVacancy!!.total = cells[1].text().trim().toIntOrNull() ?: 0
                    currentVacancy!!.subscribed = cells[2].text().trim().toIntOrNull() ?: 0
                    currentVacancy!!.pending = cells[3].text().trim().toIntOrNull() ?: 0
                    currentVacancy!!.enrolled = cells[4].text().trim().toIntOrNull() ?: 0
                }
            } else if (cells.size == 6 && currentType != null && currentVacancy != null) {
                // This is a vacancy group (IME - Matemática Bacharelado, etc.)
                val groupName = cells[1].text().trim()
                val groupTotal = cells[2].text().trim().toIntOrNull() ?: 0
                val groupSubscribed = cells[3].text().trim().toIntOrNull() ?: 0
                val groupPending = cells[4].text().trim().toIntOrNull() ?: 0
                val groupEnrolled = cells[5].text().trim().toIntOrNull() ?: 0

                // Add to current vacancy's groups
                currentVacancy!!.groups[groupName] = VacancyInfo(
                    total = groupTotal,
                    subscribed = groupSubscribed,
                    pending = groupPending,
                    enrolled = groupEnrolled
                )
            }
        }

        // Add the last vacancy
        if (currentType != null && currentVacancy != null) {
            vacancies[currentType!!] = currentVacancy!!.build()
        }

        return vacancies
    }

    private fun processLinkedClassrooms(classrooms: List<Classroom>): List<Classroom> {
        val result = mutableListOf<Classroom>()
        val theoreticalClassrooms = classrooms.filter { it.type?.contains("Teórica") == true }
        val practicalClassrooms = classrooms.filter { it.type?.contains("Prática") == true }
        val standardClassrooms = classrooms.filter {
            it.type?.contains("Teórica") != true && it.type?.contains("Prática") != true
        }

        // Add all standard classrooms
        result.addAll(standardClassrooms)

        // Process linked classrooms
        practicalClassrooms.forEach { practical ->
            val theoretical = theoreticalClassrooms.find { it.code == practical.theoreticalCode }

            if (theoretical != null) {
                // Create a combined classroom
                val combined = Classroom(
                    code = "${theoretical.code}+${practical.code.takeLast(2)}",
                    startDate = theoretical.startDate,
                    endDate = theoretical.endDate,
                    observations = listOfNotNull(theoretical.observations, practical.observations)
                        .filter { it.isNotEmpty() }
                        .joinToString("\n"),
                    teachers = (theoretical.teachers + practical.teachers).distinct(),
                    schedules = theoretical.schedules + practical.schedules,
                    vacancies = practical.vacancies, // Use practical's vacancies as they're usually more restrictive
                    type = "Teórica+Prática",
                    theoreticalCode = null
                )
                result.add(combined)
            } else {
                // Add the practical classroom as is
                result.add(practical)
            }
        }

        // Add any theoretical classrooms that don't have linked practicals
        val linkedTheoretical = practicalClassrooms.mapNotNull { it.theoreticalCode }.toSet()
        theoreticalClassrooms
            .filter { !linkedTheoretical.contains(it.code) }
            .forEach { result.add(it) }

        return result
    }

    /**
     * Get the unit code from its name
     */
    fun getUnitCode(unitName: String): String? {
        return unitCodes[unitName]
    }

    // Helper classes for building objects
    private class ClassroomBuilder {
        var code: String = ""
        var startDate: String = ""
        var endDate: String = ""
        var observations: String = ""
        var type: String? = null
        var theoreticalCode: String? = null
        val teachers = mutableListOf<String>()
        val schedules = mutableListOf<Schedule>()
        val vacancies = mutableMapOf<String, VacancyInfo>()

        fun build(): Classroom {
            return Classroom(
                code = code,
                startDate = startDate,
                endDate = endDate,
                observations = observations,
                teachers = teachers.toList(),
                schedules = schedules.toList(),
                vacancies = vacancies.toMap(),
                type = type,
                theoreticalCode = theoreticalCode
            )
        }
    }

    private class VacancyBuilder {
        var total: Int = 0
        var subscribed: Int = 0
        var pending: Int = 0
        var enrolled: Int = 0
        val groups = mutableMapOf<String, VacancyInfo>()

        fun build(): VacancyInfo {
            return VacancyInfo(
                total = total,
                subscribed = subscribed,
                pending = pending,
                enrolled = enrolled,
                groups = groups.toMap()
            )
        }
    }
}