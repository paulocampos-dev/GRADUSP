package com.prototype.gradusp.data.parser

import android.util.Log
import com.prototype.gradusp.data.model.Course
import com.prototype.gradusp.data.model.LectureInfo
import com.prototype.gradusp.data.model.LectureType
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/**
 * Parses the HTML content of a course curriculum page from JupiterWeb.
 * Enhanced with better error handling and data validation.
 */
class CoursePageParser {

    companion object {
        private const val TAG = "CoursePageParser"
        private val CODE_REGEX = Regex("codcur=(.+?)&codhab=(.+?)(&|$)")
        private val COURSE_NAME_REGEX = Regex("Curso:\\s*(.+)\\s*")
        private val UNIT_CODE_REGEX = Regex("codcg=([1-9]+)")
        private val PERIOD_REGEX = Regex("([0-9]+)º Período Ideal")
    }

    fun parse(document: Document, courseLink: String, period: String): Course? {
        return try {
            val code = extractCourseCode(courseLink)
            val name = extractCourseName(document)
            val unit = extractUnit(courseLink)
            val periods = extractPeriods(document)

            if (code.isBlank() || name.isBlank()) {
                Log.w(TAG, "Failed to extract essential course information from $courseLink")
                return null
            }

            Course(code, name, unit, period, periods)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing course from $courseLink", e)
            null
        }
    }

    private fun extractCourseCode(courseLink: String): String {
        return CODE_REGEX.find(courseLink)?.let { match ->
            "${match.groupValues[1]}-${match.groupValues[2]}"
        } ?: run {
            Log.w(TAG, "Could not extract course code from link: $courseLink")
            ""
        }
    }

    private fun extractCourseName(document: Document): String {
        val courseText = document.text()
        val nameMatches = COURSE_NAME_REGEX.findAll(courseText)
        return nameMatches.map { it.groupValues[1].trim() }.joinToString(" - ").ifBlank {
            Log.w(TAG, "Could not extract course name from document")
            ""
        }
    }

    private fun extractUnit(courseLink: String): String {
        return UNIT_CODE_REGEX.find(courseLink)?.groupValues?.get(1) ?: run {
            Log.w(TAG, "Could not extract unit code from link: $courseLink")
            ""
        }
    }

    private fun extractPeriods(document: Document): Map<String, List<LectureInfo>> {
        return try {
            val table = document.select("table")
                .firstOrNull { it.text().contains("Disciplinas Obrigatórias") }

            table?.let { parsePeriods(it) } ?: emptyMap()
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting periods from document", e)
            emptyMap()
        }
    }

    private fun parsePeriods(table: Element): Map<String, List<LectureInfo>> {
        return try {
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

                // Check for discipline type headers
                typeMap[rowText]?.let {
                    currentType = it
                    return@forEach
                }

                // Check for period headers
                PERIOD_REGEX.find(rowText)?.let {
                    currentPeriod = it.groupValues[1]
                    periods.getOrPut(currentPeriod) { mutableListOf() }
                    return@forEach
                }

                // Parse lecture rows
                parseLectureRow(row, periods, currentPeriod, currentType)
            }

            return periods.mapValues { it.value.toList() }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing periods from table", e)
            emptyMap()
        }
    }

    private fun parseLectureRow(
        row: Element,
        periods: MutableMap<String, MutableList<LectureInfo>>,
        currentPeriod: String,
        currentType: String
    ) {
        try {
            val cells = row.select("td")
            if (cells.isEmpty() || currentPeriod.isBlank()) return

            val firstCellText = cells.first()?.text()?.trim() ?: ""
            if (firstCellText.isBlank()) return

            when {
                // New lecture entry (7-character code)
                firstCellText.length == 7 && firstCellText.matches(Regex("[A-Z0-9]{7}")) -> {
                    val lectureType = when (currentType) {
                        "obrigatoria" -> LectureType.OBRIGATORIA
                        "optativa_eletiva" -> LectureType.OPTATIVA_ELETIVA
                        "optativa_livre" -> LectureType.OPTATIVA_LIVRE
                        else -> LectureType.OPTATIVA_LIVRE
                    }
                    periods[currentPeriod]?.add(LectureInfo(code = firstCellText, type = lectureType))
                }

                // Prerequisite information
                cells.size >= 2 -> {
                    val secondCellText = cells[1].text().trim()
                    periods[currentPeriod]?.lastOrNull()?.let { lastLecture ->
                        if (firstCellText.length >= 7) {
                            val reqCode = firstCellText.substring(0, 7)
                            val updatedLecture = when (secondCellText) {
                                "Requisito fraco" -> lastLecture.copy(reqWeak = lastLecture.reqWeak + reqCode)
                                "Requisito" -> lastLecture.copy(reqStrong = lastLecture.reqStrong + reqCode)
                                "Indicação de Conjunto" -> lastLecture.copy(indConjunto = lastLecture.indConjunto + reqCode)
                                else -> null
                            }
                            if (updatedLecture != null) {
                                periods[currentPeriod]?.removeLast()
                                periods[currentPeriod]?.add(updatedLecture)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error parsing lecture row", e)
        }
    }
}