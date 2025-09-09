package com.prototype.gradusp.data.parser

import android.util.Log
import com.prototype.gradusp.data.model.Classroom
import com.prototype.gradusp.data.model.Lecture
import com.prototype.gradusp.data.model.Schedule
import com.prototype.gradusp.data.model.VacancyInfo
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/**
 * Parses the HTML content of lecture and classroom pages from JupiterWeb.
 * Enhanced with better error handling and data validation.
 */
class LecturePageParser(
    private val campusByUnit: Map<List<Int>, String> = emptyMap(),
    private val unitCodes: Map<String, String> = emptyMap()
) {

    companion object {
        private const val TAG = "LecturePageParser"
    }

    /**
     * Main entry point to parse a lecture.
     * It takes the HTML documents for the lecture info page and the classrooms page.
     */
    fun parse(
        infoDocument: Document,
        classroomsDocument: Document,
        lectureCode: String
    ): Lecture {
        return try {
            val lecture = parseLectureInfo(infoDocument, lectureCode)
            val classrooms = parseClassrooms(classroomsDocument)
            val processedClassrooms = processLinkedClassrooms(classrooms)
            lecture.copy(classrooms = processedClassrooms)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing lecture $lectureCode", e)
            // Return a basic lecture with just the code if parsing fails
            Lecture(
                code = lectureCode,
                name = lectureCode,
                unit = "",
                department = "",
                campus = "",
                classrooms = emptyList()
            )
        }
    }

    private fun parseLectureInfo(document: Document, lectureCode: String): Lecture {
        return try {
            val tables = document.select("table")

            val lectureData = parseLectureTables(tables, lectureCode)
            val campus = determineCampus(lectureData.unit)

            Lecture(
                code = lectureCode,
                name = lectureData.name.ifBlank { lectureCode },
                unit = lectureData.unit,
                department = lectureData.department,
                campus = campus,
                objectives = lectureData.objectives,
                summary = lectureData.summary,
                lectureCredits = lectureData.lectureCredits,
                workCredits = lectureData.workCredits
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing lecture info for $lectureCode", e)
            Lecture(
                code = lectureCode,
                name = lectureCode,
                unit = "",
                department = "",
                campus = "",
                objectives = "",
                summary = "",
                lectureCredits = 0,
                workCredits = 0
            )
        }
    }

    private data class LectureData(
        var unit: String = "",
        var department: String = "",
        var name: String = "",
        var objectives: String = "",
        var summary: String = "",
        var lectureCredits: Int = 0,
        var workCredits: Int = 0
    )

    private fun parseLectureTables(tables: org.jsoup.select.Elements, lectureCode: String): LectureData {
        val data = LectureData()

        tables.forEach { table ->
            val headerText = table.text()

            when {
                headerText.contains("Disciplina:") -> parseDisciplineInfo(table, lectureCode, data)
                headerText.startsWith("Objetivos") -> data.objectives = parseTableContent(table)
                headerText.startsWith("Programa Resumido") -> data.summary = parseTableContent(table)
                headerText.contains("Créditos Aula") -> parseCredits(table, data)
            }
        }

        return data
    }

    private fun parseDisciplineInfo(table: Element, lectureCode: String, data: LectureData) {
        try {
            val rows = table.select("tr")
            if (rows.size >= 2) {
                data.unit = rows[0].text().trim()
                data.department = rows[1].text().trim()
                data.name = extractDisciplineName(rows, lectureCode)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error parsing discipline info", e)
        }
    }

    private fun extractDisciplineName(rows: org.jsoup.select.Elements, lectureCode: String): String {
        for (row in rows) {
            val rowText = row.text().trim()
            if (rowText.contains("Disciplina:")) {
                val disciplineMatch = Regex("Disciplina:\\s+([A-Z0-9]{7})\\s+-\\s+(.+)").find(rowText)
                if (disciplineMatch != null) {
                    return disciplineMatch.groupValues[2].trim()
                }

                val parts = rowText.split("-")
                if (parts.size > 1 && parts[0].contains(lectureCode)) {
                    return parts.drop(1).joinToString("-").trim()
                }

                var name = rowText.substringAfter("Disciplina:").trim()
                if (name.contains(lectureCode)) {
                    name = name.substringAfter(lectureCode).trim()
                    if (name.startsWith("-")) {
                        name = name.substring(1).trim()
                    }
                }
                return name
            }
        }
        return ""
    }

    private fun parseTableContent(table: Element): String {
        return try {
            table.select("tr").getOrNull(1)?.text()?.trim() ?: ""
        } catch (e: Exception) {
            Log.w(TAG, "Error parsing table content", e)
            ""
        }
    }

    private fun parseCredits(table: Element, data: LectureData) {
        try {
            table.select("tr").forEach { row ->
                val cells = row.select("td")
                if (cells.size >= 2) {
                    val cellText = cells[0].text().trim()
                    val value = cells[1].text().trim().toIntOrNull() ?: 0

                    when {
                        cellText.contains("Créditos Aula") -> data.lectureCredits = value
                        cellText.contains("Créditos Trabalho") -> data.workCredits = value
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error parsing credits", e)
        }
    }

    private fun determineCampus(unitName: String): String {
        return try {
            val unitCodeInt = unitCodes[unitName]?.toIntOrNull() ?: 0
            if (unitCodeInt > 0) {
                campusByUnit.entries.find { (codes, _) ->
                    codes.contains(unitCodeInt)
                }?.value ?: "Outro"
            } else {
                "Outro"
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error determining campus for unit $unitName", e)
            "Outro"
        }
    }

    private fun parseClassrooms(document: Document): List<Classroom> {
        val classrooms = mutableListOf<Classroom>()
        var currentClassroom: ClassroomBuilder? = null

        document.select("table").forEach { table ->
            val headerText = table.text()

            if (headerText.contains("Código da Turma")) {
                currentClassroom?.let { classrooms.add(it.build()) }
                currentClassroom = parseClassroomInfo(table)
            } else if (headerText.contains("Horário") && currentClassroom != null) {
                currentClassroom!!.schedules.addAll(parseClassroomSchedules(table))
            } else if (headerText.contains("Vagas") && currentClassroom != null) {
                currentClassroom!!.vacancies.putAll(parseVacancies(table))
            }
        }
        currentClassroom?.let { classrooms.add(it.build()) }
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
                            Regex("^(\\w+)").find(value)?.let {
                                builder.code = it.groupValues[1]
                            }
                        }
                    }
                    label.contains("Início") -> builder.startDate = value
                    label.contains("Fim") -> builder.endDate = value
                    label.contains("Observações") -> builder.observations = value
                    label.contains("Tipo da Turma") -> builder.type = ClassroomType.fromString(value)
                }
            }
        }
        return builder
    }

    private fun parseClassroomSchedules(table: Element): List<Schedule> {
        val schedules = mutableListOf<Schedule>()
        var currentScheduleData: Triple<String, String, String>? = null
        var currentTeachers = mutableListOf<String>()

        table.select("tr").forEach { row ->
            val cells = row.select("td")
            if (cells.size < 2) return@forEach

            val day = cells[0].text().trim()
            val startTime = cells[1].text().trim()
            val endTime = cells.getOrNull(2)?.text()?.trim() ?: ""
            val teacher = cells.getOrNull(3)?.text()?.trim() ?: ""

            if (day.isNotEmpty()) {
                currentScheduleData?.let { (d, s, e) ->
                    schedules.add(Schedule(d, s, e, currentTeachers.toList()))
                }
                currentScheduleData = Triple(day, startTime, endTime)
                currentTeachers = if (teacher.isNotEmpty()) mutableListOf(teacher) else mutableListOf()
            } else if (teacher.isNotEmpty()) {
                currentTeachers.add(teacher)
            }
        }
        currentScheduleData?.let { (d, s, e) ->
            schedules.add(Schedule(d, s, e, currentTeachers.toList()))
        }
        return schedules
    }

    private fun parseVacancies(table: Element): Map<String, VacancyInfo> {
        val vacancies = mutableMapOf<String, VacancyInfo>()
        var currentType: String? = null
        var currentVacancy: VacancyBuilder? = null

        table.select("tr").forEach { row ->
            val cells = row.select("td")
            if (cells.size < 2 || cells[0].text().trim().let { it == "Vagas" || it.isEmpty() }) return@forEach

            if (cells.size == 5 && cells[0].text().trim().isNotEmpty()) {
                currentType?.let { type -> currentVacancy?.let { vacancies[type] = it.build() } }
                currentType = cells[0].text().trim()
                currentVacancy = VacancyBuilder().apply {
                    total = cells[1].text().trim().toIntOrNull() ?: 0
                    subscribed = cells[2].text().trim().toIntOrNull() ?: 0
                    pending = cells[3].text().trim().toIntOrNull() ?: 0
                    enrolled = cells[4].text().trim().toIntOrNull() ?: 0
                }
            } else if (cells.size == 6 && currentType != null && currentVacancy != null) {
                currentVacancy!!.groups[cells[1].text().trim()] = VacancyInfo(
                    total = cells[2].text().trim().toIntOrNull() ?: 0,
                    subscribed = cells[3].text().trim().toIntOrNull() ?: 0,
                    pending = cells[4].text().trim().toIntOrNull() ?: 0,
                    enrolled = cells[5].text().trim().toIntOrNull() ?: 0
                )
            }
        }
        currentType?.let { type -> currentVacancy?.let { vacancies[type] = it.build() } }
        return vacancies
    }

    private fun processLinkedClassrooms(classrooms: List<Classroom>): List<Classroom> {
        val theoreticalClassrooms = classrooms.filter { it.type?.contains("Teórica") == true }
        val practicalClassrooms = classrooms.filter { it.type?.contains("Prática") == true }
        val standardClassrooms = classrooms.filter { it.type?.contains("Teórica") != true && it.type?.contains("Prática") != true }

        val result = mutableListOf<Classroom>()
        result.addAll(standardClassrooms)

        val linkedTheoreticalCodes = mutableSetOf<String>()
        practicalClassrooms.forEach { practical ->
            val theoretical = theoreticalClassrooms.find { it.code == practical.theoreticalCode }
            if (theoretical != null) {
                linkedTheoreticalCodes.add(theoretical.code)
                val combined = Classroom(
                    code = "${theoretical.code}+${practical.code.takeLast(2)}",
                    startDate = theoretical.startDate,
                    endDate = theoretical.endDate,
                    observations = listOfNotNull(theoretical.observations, practical.observations).filter { it.isNotEmpty() }.joinToString("\n"),
                    teachers = (theoretical.teachers + practical.teachers).distinct(),
                    schedules = theoretical.schedules + practical.schedules,
                    vacancies = practical.vacancies,
                    type = ClassroomType.TEORICA_PRATICA,
                    theoreticalCode = null
                )
                result.add(combined)
            } else {
                result.add(practical)
            }
        }

        result.addAll(theoreticalClassrooms.filter { it.code !in linkedTheoreticalCodes })
        return result
    }

    private data class ClassroomBuilder(
        var code: String = "",
        var startDate: String = "",
        var endDate: String = "",
        var observations: String = "",
        var type: ClassroomType? = null,
        var theoreticalCode: String? = null,
        val schedules: MutableList<Schedule> = mutableListOf(),
        val vacancies: MutableMap<String, VacancyInfo> = mutableMapOf()
    ) {
        fun build() = Classroom(code, startDate, endDate, observations, emptyList(), schedules.toList(), vacancies.toMap(), type, theoreticalCode)
    }

    private data class VacancyBuilder(
        var total: Int = 0,
        var subscribed: Int = 0,
        var pending: Int = 0,
        var enrolled: Int = 0,
        val groups: MutableMap<String, VacancyInfo> = mutableMapOf()
    ) {
        fun build() = VacancyInfo(total, subscribed, pending, enrolled, groups.toMap())
    }
}