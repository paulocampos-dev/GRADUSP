package com.prototype.gradusp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prototype.gradusp.data.parser.SearchService
import com.prototype.gradusp.data.model.*
import com.prototype.gradusp.data.repository.EnhancedUspDataRepository
import com.prototype.gradusp.utils.OfflineSupport
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val enhancedRepository: EnhancedUspDataRepository,
    private val searchService: SearchService,
    private val offlineSupport: OfflineSupport
) : ViewModel() {

    // Search state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching

    private val _searchResults = MutableStateFlow<List<SearchService.SearchResult<Lecture>>>(emptyList())
    val searchResults: StateFlow<List<SearchService.SearchResult<Lecture>>> = _searchResults

    private val _searchError = MutableStateFlow<String?>(null)
    val searchError: StateFlow<String?> = _searchError

    private val _searchSuggestions = MutableStateFlow<List<String>>(emptyList())
    val searchSuggestions: StateFlow<List<String>> = _searchSuggestions

    private val _selectedFilters = MutableStateFlow<SearchService.SearchFilters>(SearchService.SearchFilters())
    val selectedFilters: StateFlow<SearchService.SearchFilters> = _selectedFilters

    // Campus units for filtering
    private val _campusUnits = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val campusUnits: StateFlow<Map<String, List<String>>> = _campusUnits

    // Offline support
    val isOnline: StateFlow<Boolean> = offlineSupport.isOnline
    val connectionType: StateFlow<OfflineSupport.ConnectionType> = offlineSupport.connectionType

    private val _isOfflineMode = MutableStateFlow(false)
    val isOfflineMode: StateFlow<Boolean> = _isOfflineMode

    private val _cachedDataAvailable = MutableStateFlow(false)
    val cachedDataAvailable: StateFlow<Boolean> = _cachedDataAvailable

    // Progressive search job for cancellation
    private var progressiveSearchJob: Job? = null
    private var suggestionsJob: Job? = null

    init {
        // Check connectivity and cached data on initialization
        checkConnectivityAndCache()

        // Load campus units on initialization
        loadCampusUnits()

        // Monitor connectivity changes
        viewModelScope.launch {
            offlineSupport.isOnline.collect { online ->
                _isOfflineMode.value = !online
                if (online && _searchQuery.value.isNotBlank()) {
                    // Retry search when coming back online
                    performSearch(_searchQuery.value, _selectedFilters.value)
                }
            }
        }

        // Set up automatic search when query or filters change
        combine(_searchQuery, _selectedFilters) { query, filters ->
            Pair(query, filters)
        }.debounce(300) // Debounce to avoid excessive searches
            .distinctUntilChanged()
            .onEach { (query, filters) ->
                if (query.isNotBlank()) {
                    performSearch(query, filters)
                } else {
                    _searchResults.value = emptyList()
                }
            }
            .launchIn(viewModelScope)
    }

    private fun checkConnectivityAndCache() {
        viewModelScope.launch {
            try {
                // Check connectivity
                offlineSupport.refreshConnectivity()

                // Check if cached data is available
                val stats = enhancedRepository.getOfflineStats()
                _cachedDataAvailable.value = stats.cachedLectures > 0
            } catch (e: Exception) {
                _cachedDataAvailable.value = false
            }
        }
    }

    private fun loadCampusUnits() {
        viewModelScope.launch {
            try {
                val units = enhancedRepository.getCampusUnits()
                _campusUnits.value = units
            } catch (e: Exception) {
                _searchError.value = "Failed to load campus units: ${e.message}"
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        _searchError.value = null

        // Generate suggestions for partial queries
        if (query.length >= 2) {
            generateSuggestions(query)
        } else {
            _searchSuggestions.value = emptyList()
        }
    }

    private fun generateSuggestions(query: String) {
        suggestionsJob?.cancel()
        suggestionsJob = viewModelScope.launch {
            try {
                val suggestions = enhancedRepository.getSearchSuggestions(query)
                _searchSuggestions.value = suggestions
            } catch (e: Exception) {
                // Silently fail for suggestions
            }
        }
    }

    fun updateFilters(filters: SearchService.SearchFilters) {
        _selectedFilters.value = filters
    }

    fun clearFilters() {
        _selectedFilters.value = SearchService.SearchFilters()
    }

    private fun performSearch(query: String, filters: SearchService.SearchFilters) {
        // Cancel any existing search
        progressiveSearchJob?.cancel()

        progressiveSearchJob = viewModelScope.launch {
            try {
                _isSearching.value = true
                _searchError.value = null

                // Check if we're offline
                if (!offlineSupport.isOnline.value) {
                    if (_cachedDataAvailable.value) {
                        _searchError.value = "Modo offline - Usando dados em cache"
                        // Try to search in cached data (this would need enhancement in the repository)
                        performOfflineSearch(query, filters)
                    } else {
                        _searchError.value = "Sem conexão e nenhum dado em cache disponível"
                        _searchResults.value = emptyList()
                    }
                    return@launch
                }

                // Perform online search
                val results = enhancedRepository.searchLecturesAdvanced(
                    query = query,
                    filters = filters,
                    maxResults = 100
                )

                _searchResults.value = results

                // Update cached data availability
                checkConnectivityAndCache()

            } catch (e: Exception) {
                _searchError.value = "Busca falhou: ${e.message}"

                // If online search fails, try offline search as fallback
                if (_cachedDataAvailable.value && e.message?.contains("network") == true) {
                    _searchError.value = "Busca online falhou - Tentando modo offline"
                    performOfflineSearch(query, filters)
                } else {
                    _searchResults.value = emptyList()
                }
            } finally {
                _isSearching.value = false
            }
        }
    }

    private suspend fun performOfflineSearch(query: String, filters: SearchService.SearchFilters) {
        try {
            // This is a simplified offline search - in a real implementation,
            // you might want to enhance the repository to support offline queries
            _searchResults.value = emptyList()
            _searchError.value = "Busca offline limitada - Sincronize dados quando online"
        } catch (e: Exception) {
            _searchError.value = "Busca offline falhou: ${e.message}"
            _searchResults.value = emptyList()
        }
    }

    fun performProgressiveSearch(query: String, filters: SearchService.SearchFilters) {
        progressiveSearchJob?.cancel()
        progressiveSearchJob = viewModelScope.launch {
            try {
                _isSearching.value = true
                _searchError.value = null

                enhancedRepository.searchLecturesProgressive(query, filters)
                    .collect { partialResults ->
                        _searchResults.value = partialResults.take(50) // Limit for performance
                    }
            } catch (e: Exception) {
                _searchError.value = "Progressive search failed: ${e.message}"
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun findNonConflictingLectures(
        existingLectures: List<Lecture>,
        query: String,
        filters: SearchService.SearchFilters = SearchService.SearchFilters()
    ) {
        viewModelScope.launch {
            try {
                _isSearching.value = true
                _searchError.value = null

                val results = enhancedRepository.findNonConflictingLectures(
                    existingLectures = existingLectures,
                    query = query,
                    filters = filters
                )

                _searchResults.value = results
            } catch (e: Exception) {
                _searchError.value = "Failed to find non-conflicting lectures: ${e.message}"
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun selectSuggestion(suggestion: String) {
        _searchQuery.value = suggestion
        _searchSuggestions.value = emptyList()
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _searchResults.value = emptyList()
        _searchSuggestions.value = emptyList()
        _searchError.value = null
        progressiveSearchJob?.cancel()
        suggestionsJob?.cancel()
    }

    fun retrySearch() {
        val query = _searchQuery.value
        val filters = _selectedFilters.value
        if (query.isNotBlank()) {
            performSearch(query, filters)
        }
    }

    // Get lecture details
    suspend fun getLectureDetails(code: String): Lecture? {
        return try {
            enhancedRepository.getLecture(code)
        } catch (e: Exception) {
            _searchError.value = "Failed to get lecture details: ${e.message}"
            null
        }
    }

    // Get course details
    suspend fun getCourseDetails(unitCode: String): List<Course> {
        return try {
            enhancedRepository.getCoursesForUnit(unitCode)
        } catch (e: Exception) {
            _searchError.value = "Failed to get courses: ${e.message}"
            emptyList()
        }
    }

    // Validate codes
    fun isValidLectureCode(code: String): Boolean {
        return enhancedRepository.isValidLectureCode(code)
    }

    fun isValidCourseCode(code: String): Boolean {
        return enhancedRepository.isValidCourseCode(code)
    }

    /**
     * Get user-friendly connection status message
     */
    fun getConnectionStatusMessage(): String {
        return offlineSupport.getConnectionStatusMessage()
    }

    /**
     * Check if connection is suitable for large data transfers
     */
    fun isConnectionSuitableForLargeData(): Boolean {
        return offlineSupport.isConnectionSuitableForLargeData()
    }

    /**
     * Refresh connectivity status
     */
    fun refreshConnectivity() {
        offlineSupport.refreshConnectivity()
        checkConnectivityAndCache()
    }

    /**
     * Get offline statistics
     */
    suspend fun getOfflineStats() = enhancedRepository.getOfflineStats()

    /**
     * Clear all cached data
     */
    suspend fun clearCache(): Boolean {
        return try {
            val success = enhancedRepository.clearCache()
            if (success) {
                _cachedDataAvailable.value = false
                _searchResults.value = emptyList()
            }
            success
        } catch (e: Exception) {
            false
        }
    }

    override fun onCleared() {
        super.onCleared()
        progressiveSearchJob?.cancel()
        suggestionsJob?.cancel()
    }
}
