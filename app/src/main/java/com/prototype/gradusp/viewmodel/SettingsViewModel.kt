package com.prototype.gradusp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prototype.gradusp.data.AnimationSpeed
import com.prototype.gradusp.data.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

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