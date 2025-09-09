package com.prototype.gradusp.data.parser

import android.util.Log
import com.prototype.gradusp.data.model.*

/**
 * Data transformation layer for ensuring consistency and data validation.
 * Handles normalization, validation, and enrichment of parsed data.
 */
class DataTransformer {

    companion object {
        private const val TAG = "DataTransformer"
    }

    /**
     * Validates and transforms a Lecture object
     */
    fun transformLecture(lecture: Lecture): Lecture {
        return try {
            lecture.copy(
                code = validateAndNormalizeCode(lecture.code),
                name = validateAndNormalizeName(lecture.name),
                unit = validateAndNormalizeText(lecture.unit),
                department = validateAndNormalizeText(lecture.department),
                campus = validateAndNormalizeCampus(lecture.campus),
                objectives = validateAndNormalizeText(lecture.objectives),
                summary = validateAndNormalizeText(lecture.summary),
                lectureCredits = validateCredits(lecture.lectureCredits),
                workCredits = validateCredits(lecture.workCredits),
                classrooms = lecture.classrooms.map { transformClassroom(it) }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error transforming lecture ${lecture.code}", e)
            lecture
        }
    }

    /**
     * Validates and transforms a Course object
     */
    fun transformCourse(course: Course): Course {
        return try {
            course.copy(
                code = validateAndNormalizeCode(course.code),
                name = validateAndNormalizeName(course.name),
                unit = validateAndNormalizeText(course.unit),
                period = validateAndNormalizeText(course.period),
                periods = transformPeriods(course.periods)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error transforming course ${course.code}", e)
            course
        }
    }

    /**
     * Validates and transforms a Classroom object
     */
    fun transformClassroom(classroom: Classroom): Classroom {
        return try {
            classroom.copy(
                code = validateAndNormalizeCode(classroom.code),
                startDate = validateAndNormalizeDate(classroom.startDate),
                endDate = validateAndNormalizeDate(classroom.endDate),
                observations = validateAndNormalizeText(classroom.observations),
                teachers = classroom.teachers.map { validateAndNormalizeName(it) },
                schedules = classroom.schedules.map { transformSchedule(it) },
                vacancies = transformVacancies(classroom.vacancies),
                type = classroom.type?.let { validateAndNormalizeText(it) },
                theoreticalCode = classroom.theoreticalCode?.let { validateAndNormalizeCode(it) }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error transforming classroom ${classroom.code}", e)
            classroom
        }
    }

    /**
     * Validates and transforms a Schedule object
     */
    fun transformSchedule(schedule: Schedule): Schedule {
        return try {
            schedule.copy(
                day = validateAndNormalizeDay(schedule.day),
                startTime = validateAndNormalizeTime(schedule.startTime),
                endTime = validateAndNormalizeTime(schedule.endTime),
                teachers = schedule.teachers.map { validateAndNormalizeName(it) }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error transforming schedule", e)
            schedule
        }
    }

    // Validation and normalization methods

    private fun validateAndNormalizeCode(code: String): String {
        return code.trim().uppercase().takeIf { it.isNotBlank() } ?: ""
    }

    private fun validateAndNormalizeName(name: String): String {
        return name.trim().takeIf { it.isNotBlank() } ?: ""
    }

    private fun validateAndNormalizeText(text: String): String {
        return text.trim().takeIf { it.isNotBlank() } ?: ""
    }

    private fun validateAndNormalizeCampus(campus: String): String {
        val normalized = campus.trim()
        return when (normalized.lowercase()) {
            "são paulo", "sp", "sao paulo" -> "São Paulo"
            "ribeirão preto", "rp", "ribeirao preto" -> "Ribeirão Preto"
            "são carlos", "sc", "sao carlos" -> "São Carlos"
            "piracicaba", "piraci" -> "Piracicaba"
            "bauru" -> "Bauru"
            "pirassununga" -> "Pirassununga"
            "são sebastião", "sao sebastiao" -> "São Sebastião"
            "lorena" -> "Lorena"
            else -> if (normalized.isNotBlank()) normalized else "Outro"
        }
    }

    private fun validateCredits(credits: Int): Int {
        return credits.coerceIn(0, 20) // Reasonable bounds for credits
    }

    private fun validateAndNormalizeDate(date: String): String {
        return date.trim().takeIf { it.isNotBlank() } ?: ""
    }

    private fun validateAndNormalizeDay(day: String): String {
        val normalized = day.trim().lowercase()
        return when (normalized) {
            "seg", "segunda", "segunda-feira" -> "seg"
            "ter", "terça", "terca", "terça-feira" -> "ter"
            "qua", "quarta", "quarta-feira" -> "qua"
            "qui", "quinta", "quinta-feira" -> "qui"
            "sex", "sexta", "sexta-feira" -> "sex"
            "sab", "sábado", "sabado" -> "sab"
            "dom", "domingo" -> "dom"
            else -> normalized
        }
    }

    private fun validateAndNormalizeTime(time: String): String {
        return time.trim().takeIf {
            it.isNotBlank() && Regex("\\d{1,2}:\\d{2}").matches(it)
        } ?: ""
    }

    private fun transformPeriods(periods: Map<String, List<LectureInfo>>): Map<String, List<LectureInfo>> {
        return periods.mapValues { (_, lectures) ->
            lectures.map { transformLectureInfo(it) }
        }
    }

    private fun transformLectureInfo(lectureInfo: LectureInfo): LectureInfo {
        return lectureInfo.copy(
            code = validateAndNormalizeCode(lectureInfo.code),
            type = validateLectureType(lectureInfo.type),
            reqWeak = lectureInfo.reqWeak.map { validateAndNormalizeCode(it) },
            reqStrong = lectureInfo.reqStrong.map { validateAndNormalizeCode(it) },
            indConjunto = lectureInfo.indConjunto.map { validateAndNormalizeCode(it) }
        )
    }

    private fun validateLectureType(type: String): String {
        return when (type.lowercase()) {
            "obrigatoria", "obrigatória" -> "obrigatoria"
            "optativa_eletiva", "optativa eletiva" -> "optativa_eletiva"
            "optativa_livre", "optativa livre" -> "optativa_livre"
            else -> type
        }
    }

    private fun transformVacancies(vacancies: Map<String, VacancyInfo>): Map<String, VacancyInfo> {
        return vacancies.mapValues { (_, vacancyInfo) ->
            vacancyInfo.copy(
                groups = vacancyInfo.groups.mapValues { (_, groupInfo) ->
                    groupInfo.copy(
                        total = validateVacancyCount(groupInfo.total),
                        subscribed = validateVacancyCount(groupInfo.subscribed),
                        pending = validateVacancyCount(groupInfo.pending),
                        enrolled = validateVacancyCount(groupInfo.enrolled)
                    )
                }
            )
        }
    }

    private fun validateVacancyCount(count: Int): Int {
        return count.coerceIn(0, 1000) // Reasonable bounds for class sizes
    }

    // Utility methods for data enrichment

    /**
     * Enriches lecture data with additional computed fields
     */
    fun enrichLecture(lecture: Lecture): Lecture {
        val totalCredits = lecture.lectureCredits + lecture.workCredits
        val hasPrerequisites = lecture.classrooms.any { classroom ->
            classroom.schedules.isNotEmpty()
        }

        // Could add more enrichment logic here
        return lecture
    }

    /**
     * Filters lectures based on search criteria
     */
    fun filterLectures(lectures: List<Lecture>, query: String): List<Lecture> {
        if (query.isBlank()) return lectures

        val normalizedQuery = query.trim().lowercase()
        return lectures.filter { lecture ->
            lecture.code.lowercase().contains(normalizedQuery) ||
            lecture.name.lowercase().contains(normalizedQuery) ||
            lecture.unit.lowercase().contains(normalizedQuery) ||
            lecture.department.lowercase().contains(normalizedQuery) ||
            lecture.campus.lowercase().contains(normalizedQuery) ||
            lecture.summary.lowercase().contains(normalizedQuery) ||
            lecture.teachers.any { it.lowercase().contains(normalizedQuery) }
        }
    }

    /**
     * Sorts lectures by relevance to search query
     */
    fun sortLecturesByRelevance(lectures: List<Lecture>, query: String): List<Lecture> {
        if (query.isBlank()) return lectures

        val normalizedQuery = query.trim().lowercase()
        return lectures.sortedByDescending { lecture ->
            calculateRelevanceScore(lecture, normalizedQuery)
        }
    }

    private fun calculateRelevanceScore(lecture: Lecture, query: String): Int {
        var score = 0

        // Exact code match gets highest score
        if (lecture.code.lowercase() == query) score += 100

        // Code prefix match
        if (lecture.code.lowercase().startsWith(query)) score += 50

        // Code contains query
        if (lecture.code.lowercase().contains(query)) score += 25

        // Name contains query
        if (lecture.name.lowercase().contains(query)) score += 15

        // Department contains query
        if (lecture.department.lowercase().contains(query)) score += 10

        // Unit contains query
        if (lecture.unit.lowercase().contains(query)) score += 5

        return score
    }
}
