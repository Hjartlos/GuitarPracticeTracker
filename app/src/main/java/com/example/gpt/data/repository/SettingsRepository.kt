package com.example.gpt.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore("settings")

@Singleton
class SettingsRepository @Inject constructor(private val context: Context) {

    companion object {
        private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
        private val WEEKLY_GOAL_KEY = intPreferencesKey("weekly_goal")
        private val INPUT_THRESHOLD_KEY = floatPreferencesKey("input_threshold")
        private val RHYTHM_MARGIN_KEY = floatPreferencesKey("rhythm_margin")
        private val LATENCY_OFFSET_KEY = intPreferencesKey("latency_offset")
        private val LANGUAGE_KEY = stringPreferencesKey("app_language")
        private val HAPTIC_ENABLED_KEY = booleanPreferencesKey("haptic_enabled")
    }

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

    suspend fun setLanguage(langCode: String) {
        context.dataStore.edit { prefs -> prefs[LANGUAGE_KEY] = langCode }
    }

    suspend fun getLanguage(): String {
        val savedLang = context.dataStore.data.first()[LANGUAGE_KEY]
        if (savedLang != null) {
            return savedLang
        }

        val systemLang = Locale.getDefault().language
        return if (systemLang == "pl") "pl" else "en"
    }

    suspend fun setHapticEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[HAPTIC_ENABLED_KEY] = enabled }
    }

    suspend fun isHapticEnabled(): Boolean {
        return context.dataStore.data.first()[HAPTIC_ENABLED_KEY] ?: true
    }
}