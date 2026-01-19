package com.blockdude2.game.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "game_progress")

class ProgressDataStore(private val context: Context) {

    companion object {
        private val COMPLETED_LEVELS_KEY = stringSetPreferencesKey("completed_levels")
    }

    val completedLevels: Flow<Set<Int>> = context.dataStore.data.map { preferences ->
        preferences[COMPLETED_LEVELS_KEY]?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()
    }

    suspend fun markLevelCompleted(levelId: Int) {
        context.dataStore.edit { preferences ->
            val currentSet = preferences[COMPLETED_LEVELS_KEY] ?: emptySet()
            preferences[COMPLETED_LEVELS_KEY] = currentSet + levelId.toString()
        }
    }

    suspend fun resetProgress() {
        context.dataStore.edit { preferences ->
            preferences.remove(COMPLETED_LEVELS_KEY)
        }
    }
}
