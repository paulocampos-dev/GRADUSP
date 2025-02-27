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
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val animationSpeed: Flow<AnimationSpeed> = userPreferencesRepository.animationSpeedFlow

    fun updateAnimationSpeed(speed: AnimationSpeed) {
        viewModelScope.launch {
            userPreferencesRepository.updateAnimationSpeed(speed)
        }
    }
}