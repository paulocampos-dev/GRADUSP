package com.prototype.gradusp.data.model

// Data models
data class Course(
    val code: String,
    val name: String,
    val unit: String,
    val period: String,
    val periods: Map<String, List<LectureInfo>> = mapOf()
)

data class LectureInfo(
    val code: String,
    val type: String, // "obrigatoria", "optativa_eletiva", "optativa_livre"
    val reqWeak: List<String> = listOf(),
    val reqStrong: List<String> = listOf(),
    val indConjunto: List<String> = listOf()
)

data class Lecture(
    val code: String,
    val name: String,
    val unit: String,
    val department: String,
    val campus: String,
    val objectives: String = "",
    val summary: String = "",
    val lectureCredits: Int = 0,
    val workCredits: Int = 0,
    val classrooms: List<Classroom> = listOf()
)

data class Classroom(
    val code: String,
    val startDate: String,
    val endDate: String,
    val observations: String = "",
    val teachers: List<String> = listOf(),
    val schedules: List<Schedule> = listOf()
)

data class Schedule(
    val day: String, // "seg", "ter", "qua", "qui", "sex", "sab", "dom"
    val startTime: String, // "08:00"
    val endTime: String,   // "09:40"
    val teachers: List<String> = listOf()
)
