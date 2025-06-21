package com.prototype.gradusp.data.parser

import com.prototype.gradusp.data.model.Course
import com.prototype.gradusp.data.model.LectureInfo
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/**
 * Parses the HTML content of a course curriculum page from JupiterWeb.
 */
class CoursePageParser(private val unitCodes: Map<String, String>) {

    companion object {
        private val CODE_REGEX = Regex("codcur=(.+?)&codhab=(.+?)(&|$)")
        private val COURSE_NAME_REGEX = Regex("Curso:\\s*(.+)\\s*")
        private val UNIT_CODE_REGEX = Regex("codcg=([1-9]+)")
        private val PERIOD_REGEX = Regex("([0-9]+)º Período Ideal")
    }

    fun parse(document: Document, courseLink: String, period: String): Course {
        val codeMatch = CODE_REGEX.find(courseLink)
        val code = codeMatch?.let { "${it.groupValues[1]}-${it.groupValues[2]}" } ?: ""

        val courseText = document.text()
        val nameMatches = COURSE_NAME_REGEX.findAll(courseText)
        val name = nameMatches.map { it.groupValues[1] }.joinToString(" - ")

        val unitCodeMatch = UNIT_CODE_REGEX.find(courseLink)
        val unitCodeVal = unitCodeMatch?.groupValues?.get(1) ?: ""
        val unit = unitCodes.entries.find { it.value == unitCodeVal }?.key ?: ""

        val periods = mutableMapOf<String, List<LectureInfo>>()
        document.select("table")
            .firstOrNull { it.text().contains("Disciplinas Obrigatórias") }
            ?.let { periods.putAll(parsePeriods(it)) }

        return Course(code, name, unit, period, periods)
    }

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

            typeMap[rowText]?.let {
                currentType = it
                return@forEach
            }

            PERIOD_REGEX.find(rowText)?.let {
                currentPeriod = it.groupValues[1]
                periods.getOrPut(currentPeriod) { mutableListOf() }
                return@forEach
            }

            val cells = row.select("td")
            if (cells.isNotEmpty()) {
                val firstCellText = cells.first()?.text()?.trim() ?: ""
                if (firstCellText.length == 7) {
                    periods[currentPeriod]?.add(LectureInfo(code = firstCellText, type = currentType))
                } else if (cells.size >= 2) {
                    val secondCellText = cells[1].text().trim()
                    periods[currentPeriod]?.lastOrNull()?.let { lastLecture ->
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
        return periods
    }
}