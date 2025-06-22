package com.prototype.gradusp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prototype.gradusp.data.AnimationSpeed
import com.prototype.gradusp.data.UserPreferencesRepository
import com.prototype.gradusp.data.repository.SyncResult
import com.prototype.gradusp.data.repository.UspDataRepository
import com.prototype.gradusp.ui.settings.SettingsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    internal val userPreferencesRepository: UserPreferencesRepository,
    private val uspDataRepository: UspDataRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState : Flow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadCampusData()

        viewModelScope.launch {
            uspDataRepository.lastUpdateTime.collect { time ->
                _uiState.update { it.copy(lastUpdateTime = time) }
            }
        }
        viewModelScope.launch {
            uspDataRepository.isUpdateInProgress.collect { inProgress ->
                _uiState.update { it.copy(isUpdateInProgress = inProgress) }
            }
        }
        viewModelScope.launch {
            uspDataRepository.updateProgress.collect { updateProgress ->
                _uiState.update { it.copy(updateProgress = updateProgress) }
            }
        }
    }

    private fun loadCampusData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingCampusData = true) }
            val campusMap = uspDataRepository.getCampusUnits()
            _uiState.update { it.copy(campusMap = campusMap, isLoadingCampusData = false) }
        }
    }

    fun triggerUspDataUpdate() {
        viewModelScope.launch {
            _uiState.update { it.copy(updateResult = null) }
            when (val result = uspDataRepository.updateUspData()) {
                is SyncResult.Success -> {
                    _uiState.update { it.copy(updateResult = "Dados atualizados com sucesso!") }
                }
                is SyncResult.Error -> {
                    _uiState.update { it.copy(updateResult = "Erro: ${result.message}") }
                }
            }
        }
    }

    val animationSpeed: Flow<AnimationSpeed> = userPreferencesRepository.animationSpeedFlow
    val invertSwipeDirection: Flow<Boolean> = userPreferencesRepository.invertSwipeDirectionFlow
    val selectedSchools: Flow<Set<String>> = userPreferencesRepository.selectedSchoolsFlow

    fun updateAnimationSpeed(speed: AnimationSpeed) {
        viewModelScope.launch {
            userPreferencesRepository.updateAnimationSpeed(speed)
        }
    }

    fun updateInvertSwipeDirection(invert: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateSwipeDirection(invert)
        }
    }

    fun updateSelectedSchools(schools: Set<String>) {
        viewModelScope.launch {
            userPreferencesRepository.updateSelectedSchools(schools)
        }
    }
}