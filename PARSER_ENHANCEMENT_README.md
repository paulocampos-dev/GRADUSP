# GRADUSP Parser Enhancement

This document describes the comprehensive enhancement of the GRADUSP parser system, inspired by the USPolis project structure and best practices.

## Overview

The enhanced parser system provides:

1. **Modular Architecture** - Separate crawlers for different data sources (JupiterWeb, JanusWeb)
2. **Robust Error Handling** - Comprehensive error handling and data validation
3. **Advanced Search** - Multi-criteria search with relevance ranking
4. **Data Transformation** - Consistent data normalization and enrichment
5. **Type Safety** - Enhanced models with enums and validation
6. **Performance** - Optimized search and progressive loading

## Architecture

### Core Components

#### 1. Base Infrastructure
- **`BaseCrawler`** - Abstract base class providing common HTTP functionality
- **`CrawlerResult<T>`** - Result wrapper for operations with error handling
- **`DataTransformer`** - Data normalization and validation layer

#### 2. Specialized Crawlers
- **`JupiterCrawler`** - Handles JupiterWeb academic data (courses, lectures, classrooms)
- **`JanusCrawler`** - Handles JanusWeb student data (enrollments, grades, history)

#### 3. Enhanced Parsers
- **`CoursePageParser`** - Robust course curriculum parsing
- **`LecturePageParser`** - Enhanced lecture and classroom parsing
- **`EnhancedUspParser`** - Main entry point combining all parsers

#### 4. Search System
- **`SearchService`** - Advanced search with filtering and ranking
- **`SearchFilters`** - Comprehensive filtering options
- **Progressive Search** - Real-time result streaming

## Key Features

### Enhanced Data Models

#### Lecture Enhancements
```kotlin
data class Lecture(
    // ... existing fields
    val prerequisites: List<String> = listOf(),
    val bibliography: String = "",
    val lastUpdated: LocalDate? = null,
    val isActive: Boolean = true
) {
    val totalCredits: Int get() = lectureCredits + workCredits
    val teachers: List<String> get() = classrooms.flatMap { it.teachers }.distinct()
    val schedules: List<Schedule> get() = classrooms.flatMap { it.schedules }

    // Advanced search
    fun matchesSearch(searchQuery: String): Boolean
    fun getAvailableClassrooms(): List<Classroom>
    fun conflictsWith(other: Lecture, day: DayOfWeek, time: LocalTime): Boolean
}
```

#### Type Safety Improvements
```kotlin
enum class LectureType {
    OBRIGATORIA, OPTATIVA_ELETIVA, OPTATIVA_LIVRE
}

enum class ClassroomType {
    TEORICA, PRATICA, TEORICA_PRATICA
}
```

### Advanced Search Capabilities

#### Multi-Criteria Filtering
```kotlin
val filters = SearchFilters(
    campus = "São Paulo",
    unit = "IME",
    hasVacancies = true,
    dayOfWeek = DayOfWeek.MONDAY,
    timeRange = LocalTime.of(8, 0)..LocalTime.of(12, 0),
    minCredits = 2,
    maxCredits = 6,
    lectureType = LectureType.OBRIGATORIA
)

val results = searchService.searchLecturesAdvanced("Cálculo", filters)
```

#### Relevance Ranking
- Exact code matches (100 points)
- Code prefix matches (50 points)
- Name matches (20 points)
- Teacher matches (15 points)
- Department matches (10 points)
- Availability bonuses

#### Progressive Search
```kotlin
searchService.searchLecturesProgressive("matemática")
    .collect { partialResults ->
        // Handle partial results as they arrive
        displayResults(partialResults)
    }
```

### Error Handling & Resilience

#### Comprehensive Error Handling
```kotlin
sealed class CrawlerResult<out T> {
    data class Success<T>(val data: T) : CrawlerResult<T>()
    data class Error(val message: String, val cause: Exception? = null) : CrawlerResult<T>()

    fun onSuccess(action: (T) -> Unit): CrawlerResult<T>
    fun onError(action: (String, Exception?) -> Unit): CrawlerResult<T>
}
```

#### Graceful Degradation
- Network failures return partial data when possible
- Invalid data is filtered out with logging
- Fallback values for missing information
- Timeout handling for long-running operations

### Data Transformation & Validation

#### Automatic Data Normalization
```kotlin
val transformer = DataTransformer()

val normalizedLecture = transformer.transformLecture(rawLecture)
// - Normalizes campus names
// - Validates credit ranges
// - Standardizes date formats
// - Cleans text fields
```

#### Validation Rules
- Campus names standardized (e.g., "são paulo" → "São Paulo")
- Credit values constrained to reasonable ranges
- Time formats validated and parsed
- Code formats verified (7-character USP codes)

## Usage Examples

### Basic Lecture Search
```kotlin
val parser = EnhancedUspParser(context, okHttpClient)

// Simple search
val lectures = parser.searchLectures("Cálculo")

// Advanced search with filters
val searchService = SearchService(parser)
val results = searchService.searchLecturesAdvanced(
    query = "álgebra",
    filters = SearchFilters(
        campus = "São Paulo",
        hasVacancies = true,
        minCredits = 4
    )
)
```

### Finding Non-Conflicting Classes
```kotlin
val existingClasses = listOf(myCurrentClass1, myCurrentClass2)
val availableClasses = searchService.findNonConflictingLectures(
    existingLectures = existingClasses,
    query = "física",
    filters = SearchFilters(dayOfWeek = DayOfWeek.MONDAY)
)
```

### Course Curriculum Search
```kotlin
val courses = parser.searchCourses(
    query = "computação",
    includeCurriculum = true
)

courses.forEach { course ->
    println("${course.name} - ${course.getAllLectures().size} lectures")
}
```

### Student Data (Janus)
```kotlin
// Authenticate student
val session = parser.authenticateStudent(username, password)

// Get enrollments
val enrollments = parser.fetchStudentEnrollments(session.getOrNull()!!)

// Get grades
val grades = parser.fetchStudentGrades(session.getOrNull()!!)
```

## Performance Optimizations

### 1. Progressive Loading
- Results returned as soon as available
- No waiting for complete dataset
- Suitable for large searches

### 2. Caching Strategy
- Campus/unit mappings cached
- Search results cached with TTL
- Avoid redundant network requests

### 3. Concurrent Processing
- Multiple units searched in parallel
- Coroutine-based implementation
- Efficient resource utilization

### 4. Smart Filtering
- Filters applied early in pipeline
- Reduces processing overhead
- Relevance scoring optimizes result ordering

## Migration Guide

### From Old Parser
```kotlin
// Old way
val oldParser = UspParser(context, client)
val lectures = oldParser.fetchLecture("MAC0110")

// New way
val newParser = EnhancedUspParser(context, client)
val lectures = newParser.fetchLecture("MAC0110")
```

### Enhanced Features
- Better error handling
- Automatic data validation
- Search relevance ranking
- Progressive result loading
- Advanced filtering options

## Testing

### Unit Tests
```kotlin
@Test
fun testLectureSearch() {
    val parser = EnhancedUspParser(context, mockClient)
    val results = parser.searchLectures("test")
    assertTrue(results.isNotEmpty())
}

@Test
fun testSearchFilters() {
    val filters = SearchFilters(campus = "São Paulo", hasVacancies = true)
    val results = searchService.searchLecturesAdvanced("matemática", filters)
    // Verify all results match filters
}
```

## Future Enhancements

1. **Caching Layer** - Redis/memcached integration
2. **Rate Limiting** - Respect USP server limits
3. **Offline Mode** - Cached data for offline access
4. **Analytics** - Search pattern analysis
5. **Machine Learning** - Improved relevance ranking
6. **Real-time Updates** - WebSocket integration for live data

## Contributing

When adding new features:
1. Follow the modular architecture pattern
2. Add comprehensive error handling
3. Include data validation
4. Write unit tests
5. Update this documentation

## Compatibility

- **Minimum Android API**: 21 (Android 5.0)
- **Kotlin Version**: 1.8+
- **Dependencies**: OkHttp, JSoup, Kotlin Coroutines

---

This enhancement maintains backward compatibility while providing significant improvements in reliability, performance, and functionality. The modular design makes it easy to extend and maintain.
