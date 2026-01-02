package com.example.gpt.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.dataStore by preferencesDataStore("settings")

class SettingsRepository(private val context: Context) {

    private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
    private val WEEKLY_GOAL_KEY = intPreferencesKey("weekly_goal")
    private val INPUT_THRESHOLD_KEY = floatPreferencesKey("input_threshold")
    private val RHYTHM_MARGIN_KEY = floatPreferencesKey("rhythm_margin")
    private val LATENCY_OFFSET_KEY = intPreferencesKey("latency_offset")

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[DARK_MODE_KEY] = enabled }
    }

    suspend fun isDarkMode(): Boolean {
        return context.dataStore.data.first()[DARK_MODE_KEY] ?: true
    }

    suspend fun setWeeklyGoal(hours: Int) {
        context.dataStore.edit { prefs -> prefs[WEEKLY_GOAL_KEY] = hours }
    }

    suspend fun getWeeklyGoal(): Int {
        return context.dataStore.data.first()[WEEKLY_GOAL_KEY] ?: 5
    }

    suspend fun setInputThreshold(value: Float) {
        context.dataStore.edit { prefs -> prefs[INPUT_THRESHOLD_KEY] = value }
    }

    suspend fun getInputThreshold(): Float {
        return context.dataStore.data.first()[INPUT_THRESHOLD_KEY] ?: 0.15f
    }

    suspend fun setRhythmMargin(value: Float) {
        context.dataStore.edit { prefs -> prefs[RHYTHM_MARGIN_KEY] = value }
    }

    suspend fun getRhythmMargin(): Float {
        return context.dataStore.data.first()[RHYTHM_MARGIN_KEY] ?: 0.30f
    }

    suspend fun setLatencyOffset(ms: Int) {
        context.dataStore.edit { prefs -> prefs[LATENCY_OFFSET_KEY] = ms }
    }

    suspend fun getLatencyOffset(): Int {
        return context.dataStore.data.first()[LATENCY_OFFSET_KEY] ?: 0
    }
}