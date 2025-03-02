package com.prototype.gradusp.data.model

// Represents a grade entry (single assessment) with a multiplier
data class GradeEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "",
    val grade: Float = 0f,
    val multiplier: Float = 1f
) {
    fun calculateContribution(): Float {
        return grade * multiplier
    }
}

// Represents a complete course with multiple grade entries
data class CourseGrade(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "",
    val gradeEntries: List<GradeEntry> = emptyList()
) {
    fun calculateFinalGrade(): Float {
        if (gradeEntries.isEmpty()) return 0f

        val totalContribution = gradeEntries.sumOf { it.calculateContribution().toDouble() }
        val totalMultiplier = gradeEntries.sumOf { it.multiplier.toDouble() }

        return if (totalMultiplier > 0) {
            (totalContribution / totalMultiplier).toFloat()
        } else {
            0f
        }
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
}