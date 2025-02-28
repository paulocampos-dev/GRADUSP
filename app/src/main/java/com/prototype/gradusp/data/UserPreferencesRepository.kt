package com.prototype.gradusp.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import okio.IOException
import javax.inject.Inject

val Context.dataStore by preferencesDataStore(name = "user_preferences")

class UserPreferencesRepository @Inject constructor (
    private val context: Context
) {

    companion object PreferencesKeys {
        val ANIMATION_SPEED = intPreferencesKey("animation_speed")
        val INVERT_SWIPE_DIRECTION = booleanPreferencesKey("invert_swipe_direction")
    }

    val animationSpeedFlow: Flow<AnimationSpeed> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val speedOrdinal = preferences[PreferencesKeys.ANIMATION_SPEED] ?: AnimationSpeed.MEDIUM.ordinal
            AnimationSpeed.values()[speedOrdinal]
        }

    val invertSwipeDirectionFlow: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.INVERT_SWIPE_DIRECTION] ?: false
        }

    suspend fun updateAnimationSpeed(speed: AnimationSpeed) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ANIMATION_SPEED] = speed.ordinal
        }
    }

    suspend fun updateSwipeDirection(invert: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.INVERT_SWIPE_DIRECTION] = invert
        }
    }
}

enum class AnimationSpeed(val swipe: Int, val tab: Int, val transition: Int) {
    FAST(50, 40, 60),
    MEDIUM(100, 80, 120),
    SLOW(200, 160, 240)
}
