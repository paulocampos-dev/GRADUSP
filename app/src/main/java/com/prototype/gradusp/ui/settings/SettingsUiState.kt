package com.prototype.gradusp.ui.settings

data class SettingsUiState(
    val campusMap: Map<String, List<String>> = emptyMap(),
    val isLoadingCampusData: Boolean = true,
    val lastUpdateTime: Long = 0L,
    val isUpdateInProgress: Boolean = false,
    val updateProgress: Float = 0f,
    val updateResult: String? = null
)
