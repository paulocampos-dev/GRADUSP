package com.prototype.gradusp.data.model

import java.math.BigDecimal
import java.math.RoundingMode

// Represents a single assessment (e.g., a test or assignment)
data class GradeEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "",
    val grade: Double = 0.0,
    val weight: Double = 1.0
)

// Represents a complete course with its grading scheme and entries
data class CourseGrade(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "",
    val gradeEntries: List<GradeEntry> = emptyList(),
    val notes: String = "",
    val targetGrade: Double = 5.0,
    val maxGrade: Double = 10.0
) {

    fun calculateFinalGrade(): Double {
        if (gradeEntries.isEmpty()) return 0.0

        val totalWeight = gradeEntries.sumOf { it.weight }
        if (totalWeight == 0.0) return 0.0

        val totalPoints = gradeEntries.sumOf { it.grade * it.weight }
        val finalGrade = totalPoints / totalWeight

        return BigDecimal(finalGrade).setScale(2, RoundingMode.HALF_DOWN).toDouble()
    }

    fun addGradeEntry(entry: GradeEntry): CourseGrade {
        return this.copy(gradeEntries = gradeEntries + entry)
    }

    fun updateGradeEntry(entry: GradeEntry): CourseGrade {
        return this.copy(
            gradeEntries = gradeEntries.map {
                if (it.id == entry.id) entry else it
            }
        )
    }

    fun removeGradeEntry(entryId: String): CourseGrade {
        return this.copy(
            gradeEntries = gradeEntries.filter { it.id != entryId }
        )
    }

    fun updateNotes(newNotes: String): CourseGrade {
        return this.copy(notes = newNotes)
    }
}