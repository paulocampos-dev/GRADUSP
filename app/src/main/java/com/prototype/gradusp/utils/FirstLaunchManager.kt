package com.prototype.gradusp.utils

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.prototype.gradusp.data.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Manages first launch related functionality
 */
object FirstLaunchManager {
    private val FIRST_LAUNCH_KEY = booleanPreferencesKey("is_first_launch")

    /**
     * Check if this is the first launch of the app
     */
    fun isFirstLaunch(context: Context): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            // Default to true if the key doesn't exist
            preferences[FIRST_LAUNCH_KEY] ?: true
        }
    }

    /**
     * Mark first launch as completed
     */
    suspend fun setFirstLaunchCompleted(context: Context) {
        context.dataStore.edit { preferences ->
            preferences[FIRST_LAUNCH_KEY] = false
        }
    }
}