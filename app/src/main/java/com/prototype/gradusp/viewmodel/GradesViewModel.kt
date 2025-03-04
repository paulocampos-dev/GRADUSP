package com.prototype.gradusp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prototype.gradusp.data.model.CourseGrade
import com.prototype.gradusp.data.model.GradeEntry
import com.prototype.gradusp.data.repository.GradesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GradesViewModel @Inject constructor(
    private val gradesRepository: GradesRepository
) : ViewModel() {

    // Get courses from repository and convert it to StateFlow
    val courses: StateFlow<List<CourseGrade>> = gradesRepository.coursesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Helper method to save courses
    private fun saveCourses(updatedCourses: List<CourseGrade>) {
        viewModelScope.launch {
            gradesRepository.saveCourses(updatedCourses)
        }
    }

    // Add a new course
    fun addCourse(name: String) {
        val newCourse = CourseGrade(name = name)
        val updatedCourses = courses.value + newCourse
        saveCourses(updatedCourses)
    }

    // Remove a course
    fun removeCourse(courseId: String) {
        val updatedCourses = courses.value.filter { it.id != courseId }
        saveCourses(updatedCourses)
    }

    // Update course name
    fun updateCourseName(courseId: String, newName: String) {
        val updatedCourses = courses.value.map {
            if (it.id == courseId) it.copy(name = newName) else it
        }
        saveCourses(updatedCourses)
    }

    // Update course notes
    fun updateCourseNotes(courseId: String, notes: String) {
        val updatedCourses = courses.value.map { course ->
            if (course.id == courseId) {
                course.updateNotes(notes)
            } else {
                course
            }
        }
        saveCourses(updatedCourses)
    }

    // Add a grade entry to a course
    fun addGradeEntry(courseId: String, name: String, grade: Float, multiplier: Float) {
        val entry = GradeEntry(
            name = name,
            grade = grade,
            multiplier = multiplier
        )

        val updatedCourses = courses.value.map { course ->
            if (course.id == courseId) {
                course.addGradeEntry(entry)
            } else {
                course
            }
        }
        saveCourses(updatedCourses)
    }

    // Update an existing grade entry
    fun updateGradeEntry(courseId: String, entry: GradeEntry) {
        val updatedCourses = courses.value.map { course ->
            if (course.id == courseId) {
                course.updateGradeEntry(entry)
            } else {
                course
            }
        }
        saveCourses(updatedCourses)
    }

    // Remove a grade entry
    fun removeGradeEntry(courseId: String, entryId: String) {
        val updatedCourses = courses.value.map { course ->
            if (course.id == courseId) {
                course.removeGradeEntry(entryId)
            } else {
                course
            }
        }
        saveCourses(updatedCourses)
    }
}