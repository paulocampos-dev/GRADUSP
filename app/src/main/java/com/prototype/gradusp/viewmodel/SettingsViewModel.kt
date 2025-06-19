package com.prototype.gradusp.viewmodel

import androidx.compose.runtime.collectAsState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prototype.gradusp.data.AnimationSpeed
import com.prototype.gradusp.data.UserPreferencesRepository
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
    private val userPreferencesRepository: UserPreferencesRepository,
    private val uspDataRepository: UspDataRepository
) : ViewModel() {

    private val _UiState = MutableStateFlow(SettingsUiState())
    val uiState : Flow<SettingsUiState> = _UiState.asStateFlow()

    init {
        loadCampusData()

        viewModelScope.launch {
            uspDataRepository.lastUpdateTime.collect { time ->
                _UiState.update { it.copy(lastUpdateTime = time) }
            }
        }
        viewModelScope.launch {
            uspDataRepository.isUpdateInProgress.collect { inProgress ->
                _UiState.update { it.copy(isUpdateInProgress = inProgress) }
            }
        }
        viewModelScope.launch {
            uspDataRepository.updateProgress.collect { updateProgress ->
                _UiState.update { it.copy(updateProgress = updateProgress) }
            }
        }
    }

    private fun loadCampusData() {
        viewModelScope.launch {
            _UiState.update { it.copy(isUpdateInProgress = true) }
            val campusMap = uspDataRepository.getCampusUnits()
            _UiState.update { it.copy(campusMap = campusMap, isUpdateInProgress = false) }
        }
    }

    fun triggerUspDataUpdate() {
        viewModelScope.launch {
            _UiState.update { it.copy(updateResult = null) }
            val success = uspDataRepository.updateUspData()
            val resultMessage = if (success) "Dados atualizados com sucesso!" else "Erro ao atualizar dados."
            _UiState.update { it.copy(updateResult = resultMessage) }
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