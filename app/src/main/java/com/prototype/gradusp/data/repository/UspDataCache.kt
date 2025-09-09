package com.prototype.gradusp.data.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.prototype.gradusp.data.model.*
import com.prototype.gradusp.data.parser.SearchService
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Comprehensive caching layer for USP data with automatic invalidation and cleanup
 */
@Singleton
class UspDataCache(private val context: Context) {

    private val gson = Gson()
    private val cacheMutex = Mutex()

    companion object {
        private const val TAG = "UspDataCache"
        private const val CACHE_VERSION = 1
        private const val MAX_CACHE_SIZE_MB = 100L // 100MB limit
        private const val CLEANUP_THRESHOLD_MB = 80L // Cleanup when >80MB
        private const val LECTURES_DIR = "cached_lectures"
        private const val COURSES_DIR = "cached_courses"
        private const val SEARCH_DIR = "cached_search"
        private const val METADATA_FILE = "cache_metadata.json"
    }

    private val cacheDir: File = File(context.cacheDir, "usp_data_cache")
    private val lecturesDir: File = File(cacheDir, LECTURES_DIR)
    private val coursesDir: File = File(cacheDir, COURSES_DIR)
    private val searchDir: File = File(cacheDir, SEARCH_DIR)
    private val metadataFile: File = File(cacheDir, METADATA_FILE)

    init {
        ensureDirectoriesExist()
        cleanupIfNeeded()
    }

    // Cache metadata management

    private data class CacheMetadata(
        val version: Int = CACHE_VERSION,
        val lastCleanup: Long = System.currentTimeMillis(),
        val totalSize: Long = 0L,
        val entries: MutableMap<String, CacheEntryInfo> = mutableMapOf()
    )

    private data class CacheEntryInfo(
        val key: String,
        val type: CacheType,
        val timestamp: Long,
        val size: Long,
        val accessCount: Int = 0,
        val lastAccess: Long = System.currentTimeMillis()
    )

    private enum class CacheType {
        LECTURE, COURSES, CAMPUS_UNITS, CURRICULUM, SEARCH_RESULTS
    }

    private suspend fun loadMetadata(): CacheMetadata = cacheMutex.withLock {
        try {
            if (metadataFile.exists()) {
                metadataFile.reader().use {
                    gson.fromJson(it, CacheMetadata::class.java)
                } ?: CacheMetadata()
            } else {
                CacheMetadata()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading cache metadata", e)
            CacheMetadata()
        }
    }

    private suspend fun saveMetadata(metadata: CacheMetadata) = cacheMutex.withLock {
        try {
            metadataFile.writer().use {
                gson.toJson(metadata, it)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving cache metadata", e)
        }
    }

    private suspend fun updateEntryAccess(key: String, type: CacheType) {
        val metadata = loadMetadata()
        metadata.entries[key]?.let { entry ->
            entry.accessCount++
            entry.lastAccess = System.currentTimeMillis()
            saveMetadata(metadata)
        } ?: run {
            // New entry
            metadata.entries[key] = CacheEntryInfo(
                key = key,
                type = type,
                timestamp = System.currentTimeMillis(),
                size = 0L
            )
            saveMetadata(metadata)
        }
    }

    // Lecture caching

    suspend fun cacheLecture(code: String, lecture: Lecture) = cacheMutex.withLock {
        try {
            val file = File(lecturesDir, "$code.json")
            val cachedLecture = CachedLecture(lecture, System.currentTimeMillis())

            file.writer().use {
                gson.toJson(cachedLecture, it)
            }

            updateCacheMetadata(code, CacheType.LECTURE, file.length())
            Log.d(TAG, "Cached lecture: $code")
        } catch (e: Exception) {
            Log.e(TAG, "Error caching lecture $code", e)
        }
    }

    suspend fun getCachedLecture(code: String): CachedLecture? {
        return try {
            val file = File(lecturesDir, "$code.json")
            if (!file.exists()) return null

            updateEntryAccess(code, CacheType.LECTURE)

            file.reader().use {
                gson.fromJson(it, CachedLecture::class.java)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading cached lecture $code", e)
            null
        }
    }

    // Courses caching

    suspend fun cacheCourses(unitCode: String, cachedCourses: CachedCourses) = cacheMutex.withLock {
        try {
            val file = File(coursesDir, "$unitCode.json")

            file.writer().use {
                gson.toJson(cachedCourses, it)
            }

            updateCacheMetadata(unitCode, CacheType.COURSES, file.length())
            Log.d(TAG, "Cached courses for unit: $unitCode")
        } catch (e: Exception) {
            Log.e(TAG, "Error caching courses for unit $unitCode", e)
        }
    }

    suspend fun getCachedCourses(unitCode: String): CachedCourses? {
        return try {
            val file = File(coursesDir, "$unitCode.json")
            if (!file.exists()) return null

            updateEntryAccess(unitCode, CacheType.COURSES)

            file.reader().use {
                gson.fromJson(it, CachedCourses::class.java)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading cached courses for unit $unitCode", e)
            null
        }
    }

    // Campus units caching

    suspend fun cacheCampusUnits(cachedUnits: CachedCampusUnits) = cacheMutex.withLock {
        try {
            val file = File(cacheDir, "campus_units.json")

            file.writer().use {
                gson.toJson(cachedUnits, it)
            }

            updateCacheMetadata("campus_units", CacheType.CAMPUS_UNITS, file.length())
            Log.d(TAG, "Cached campus units")
        } catch (e: Exception) {
            Log.e(TAG, "Error caching campus units", e)
        }
    }

    suspend fun getCachedCampusUnits(): CachedCampusUnits? {
        return try {
            val file = File(cacheDir, "campus_units.json")
            if (!file.exists()) return null

            updateEntryAccess("campus_units", CacheType.CAMPUS_UNITS)

            file.reader().use {
                gson.fromJson(it, CachedCampusUnits::class.java)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading cached campus units", e)
            null
        }
    }

    // Curriculum caching

    suspend fun cacheCurriculum(cachedCurriculum: CachedCurriculum) = cacheMutex.withLock {
        try {
            val key = "${cachedCurriculum.courseCode}_${cachedCurriculum.unitCode}"
            val file = File(cacheDir, "curriculum_$key.json")

            file.writer().use {
                gson.toJson(cachedCurriculum, it)
            }

            updateCacheMetadata(key, CacheType.CURRICULUM, file.length())
            Log.d(TAG, "Cached curriculum: ${cachedCurriculum.courseCode}")
        } catch (e: Exception) {
            Log.e(TAG, "Error caching curriculum ${cachedCurriculum.courseCode}", e)
        }
    }

    suspend fun getCachedCurriculum(courseCode: String, unitCode: String): CachedCurriculum? {
        return try {
            val key = "${courseCode}_$unitCode"
            val file = File(cacheDir, "curriculum_$key.json")
            if (!file.exists()) return null

            updateEntryAccess(key, CacheType.CURRICULUM)

            file.reader().use {
                gson.fromJson(it, CachedCurriculum::class.java)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading cached curriculum $courseCode", e)
            null
        }
    }

    // Search results caching

    suspend fun cacheSearchResults(key: String, cachedResults: CachedSearchResults) = cacheMutex.withLock {
        try {
            val file = File(searchDir, "$key.json")

            file.writer().use {
                gson.toJson(cachedResults, it)
            }

            updateCacheMetadata(key, CacheType.SEARCH_RESULTS, file.length())
            Log.d(TAG, "Cached search results for key: $key")
        } catch (e: Exception) {
            Log.e(TAG, "Error caching search results for key $key", e)
        }
    }

    suspend fun getCachedSearchResults(key: String): CachedSearchResults? {
        return try {
            val file = File(searchDir, "$key.json")
            if (!file.exists()) return null

            updateEntryAccess(key, CacheType.SEARCH_RESULTS)

            file.reader().use {
                gson.fromJson(it, CachedSearchResults::class.java)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading cached search results for key $key", e)
            null
        }
    }

    // Utility methods

    private suspend fun updateCacheMetadata(key: String, type: CacheType, size: Long) {
        val metadata = loadMetadata()
        metadata.entries[key] = CacheEntryInfo(
            key = key,
            type = type,
            timestamp = System.currentTimeMillis(),
            size = size,
            accessCount = metadata.entries[key]?.accessCount ?: 0,
            lastAccess = System.currentTimeMillis()
        )

        metadata.totalSize = metadata.entries.values.sumOf { it.size }
        saveMetadata(metadata)
    }

    suspend fun getCachedLectureCount(): Int {
        return lecturesDir.listFiles()?.size ?: 0
    }

    suspend fun getCachedCoursesCount(): Int {
        return coursesDir.listFiles()?.size ?: 0
    }

    suspend fun getCachedUnitsCount(): Int {
        return if (File(cacheDir, "campus_units.json").exists()) 1 else 0
    }

    suspend fun getCacheSize(): Long {
        return loadMetadata().totalSize
    }

    suspend fun getLastSyncTime(): Long? {
        val metadata = loadMetadata()
        return metadata.entries.values.maxOfOrNull { it.timestamp }
    }

    // Cache cleanup and maintenance

    suspend fun cleanup() = cacheMutex.withLock {
        try {
            val metadata = loadMetadata()
            val currentTime = System.currentTimeMillis()

            // Remove expired entries (older than 24 hours)
            val expiryTime = TimeUnit.HOURS.toMillis(24)
            val expiredKeys = metadata.entries.filter { (_, entry) ->
                currentTime - entry.timestamp > expiryTime
            }.keys

            expiredKeys.forEach { key ->
                removeEntry(key, metadata.entries[key]?.type)
                metadata.entries.remove(key)
            }

            // If still over size limit, remove least recently accessed entries
            if (metadata.totalSize > bytesFromMB(CLEANUP_THRESHOLD_MB)) {
                val sortedEntries = metadata.entries.values.sortedBy { it.lastAccess }
                var removedSize = 0L

                for (entry in sortedEntries) {
                    if (metadata.totalSize - removedSize <= bytesFromMB(MAX_CACHE_SIZE_MB)) break

                    removeEntry(entry.key, entry.type)
                    removedSize += entry.size
                    metadata.entries.remove(entry.key)
                }

                metadata.totalSize -= removedSize
            }

            metadata.lastCleanup = currentTime
            saveMetadata(metadata)

            Log.d(TAG, "Cache cleanup completed. Removed ${expiredKeys.size} expired entries")

        } catch (e: Exception) {
            Log.e(TAG, "Error during cache cleanup", e)
        }
    }

    private fun removeEntry(key: String, type: CacheType?) {
        try {
            when (type) {
                CacheType.LECTURE -> File(lecturesDir, "$key.json").delete()
                CacheType.COURSES -> File(coursesDir, "$key.json").delete()
                CacheType.CAMPUS_UNITS -> File(cacheDir, "campus_units.json").delete()
                CacheType.CURRICULUM -> {
                    // Find curriculum files with this key
                    cacheDir.listFiles { file ->
                        file.name.startsWith("curriculum_$key")
                    }?.forEach { it.delete() }
                }
                CacheType.SEARCH_RESULTS -> File(searchDir, "$key.json").delete()
                null -> Log.w(TAG, "Unknown cache type for key: $key")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing cache entry $key", e)
        }
    }

    suspend fun clearAll() = cacheMutex.withLock {
        try {
            cacheDir.deleteRecursively()
            ensureDirectoriesExist()
            saveMetadata(CacheMetadata())
            Log.d(TAG, "All cache cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing cache", e)
        }
    }

    private fun ensureDirectoriesExist() {
        listOf(cacheDir, lecturesDir, coursesDir, searchDir).forEach { dir ->
            if (!dir.exists()) dir.mkdirs()
        }
    }

    private fun cleanupIfNeeded() {
        CoroutineScope(Dispatchers.IO).launch {
            val metadata = loadMetadata()
            val currentTime = System.currentTimeMillis()
            val cleanupInterval = TimeUnit.HOURS.toMillis(6) // Cleanup every 6 hours

            if (currentTime - metadata.lastCleanup > cleanupInterval) {
                cleanup()
            }
        }
    }

    private fun bytesFromMB(mb: Long): Long = mb * 1024 * 1024
}
