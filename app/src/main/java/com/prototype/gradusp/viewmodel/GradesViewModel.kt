package com.prototype.gradusp.viewmodel

import androidx.lifecycle.ViewModel
import com.prototype.gradusp.data.model.CourseGrade
import com.prototype.gradusp.data.model.GradeEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class GradesViewModel @Inject constructor() : ViewModel() {

    // Store the list of courses with their grades
    private val _courses = MutableStateFlow<List<CourseGrade>>(emptyList())
    val courses: StateFlow<List<CourseGrade>> = _courses.asStateFlow()

    // Add a new course
    fun addCourse(name: String) {
        val newCourse = CourseGrade(name = name)
        _courses.update { currentCourses ->
            currentCourses + newCourse
        }
    }

    // Remove a course
    fun removeCourse(courseId: String) {
        _courses.update { currentCourses ->
            currentCourses.filter { it.id != courseId }
        }
    }

    // Update course name
    fun updateCourseName(courseId: String, newName: String) {
        _courses.update { currentCourses ->
            currentCourses.map {
                if (it.id == courseId) it.copy(name = newName) else it
            }
        }
    }

    // Add a grade entry to a course
    fun addGradeEntry(courseId: String, name: String, grade: Float, multiplier: Float) {
        val entry = GradeEntry(
            name = name,
            grade = grade,
            multiplier = multiplier
        )

        _courses.update { currentCourses ->
            currentCourses.map { course ->
                if (course.id == courseId) {
                    course.addGradeEntry(entry)
                } else {
                    course
                }
            }
        }
    }

    // Update an existing grade entry
    fun updateGradeEntry(courseId: String, entry: GradeEntry) {
        _courses.update { currentCourses ->
            currentCourses.map { course ->
                if (course.id == courseId) {
                    course.updateGradeEntry(entry)
                } else {
                    course
                }
            }
        }
    }

    // Remove a grade entry
    fun removeGradeEntry(courseId: String, entryId: String) {
        _courses.update { currentCourses ->
            currentCourses.map { course ->
                if (course.id == courseId) {
                    course.removeGradeEntry(entryId)
                } else {
                    course
                }
            }
        }
    }
}