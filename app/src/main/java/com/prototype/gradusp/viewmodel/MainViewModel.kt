package com.prototype.gradusp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prototype.gradusp.ui.MainUiState
import com.prototype.gradusp.utils.FirstLaunchManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val firstLaunchManager: FirstLaunchManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState = _uiState.asStateFlow()

    init {
        checkFirstLaunch()
    }

    private fun checkFirstLaunch() {
        viewModelScope.launch {
            val isFirstLaunch = firstLaunchManager.isFirstLaunch().first()
            _uiState.update { it.copy(showWelcomeDialog = isFirstLaunch) }
        }
    }

    fun onWelcomeDialogDismissed() {
        viewModelScope.launch {
            firstLaunchManager.setFirstLaunchCompleted()
            _uiState.update { it.copy(showWelcomeDialog = false) }
        }
    }
}