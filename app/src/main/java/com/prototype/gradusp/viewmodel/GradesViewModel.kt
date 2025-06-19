package com.prototype.gradusp.viewmodel

import androidx.lifecycle.SavedStateHandle
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
    private val gradesRepository: GradesRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    val isAddCourseDialogVisible = savedStateHandle.getStateFlow("show_add_course_dialog", false)

    val courses: StateFlow<List<CourseGrade>> = gradesRepository.coursesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onAddCourseDialogDismissed() {
        savedStateHandle["show_add_course_dialog"] = false
    }

    private fun saveCourses(updatedCourses: List<CourseGrade>) {
        viewModelScope.launch {
            gradesRepository.saveCourses(updatedCourses)
        }
    }

    fun addCourse(name: String) {
        val newCourse = CourseGrade(name = name)
        val updatedCourses = courses.value + newCourse
        saveCourses(updatedCourses)
    }

    fun removeCourse(courseId: String) {
        val updatedCourses = courses.value.filter { it.id != courseId }
        saveCourses(updatedCourses)
    }

    fun updateCourseName(courseId: String, newName: String) {
        val updatedCourses = courses.value.map {
            if (it.id == courseId) it.copy(name = newName) else it
        }
        saveCourses(updatedCourses)
    }

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

    // Updated to use Double for grade and weight
    fun addGradeEntry(courseId: String, name: String, grade: Double, weight: Double) {
        val entry = GradeEntry(
            name = name,
            grade = grade,
            weight = weight
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