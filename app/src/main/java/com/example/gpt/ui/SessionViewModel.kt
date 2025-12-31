package com.example.gpt.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gpt.audio.AudioEngine
import com.example.gpt.audio.RhythmAnalyzer
import com.example.gpt.audio.TunerResult
import com.example.gpt.data.PracticeSession
import com.example.gpt.data.SessionDao
import com.example.gpt.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val sessionDao: SessionDao,
    private val audioEngine: AudioEngine,
    application: Application
) : AndroidViewModel(application) {

    private val settingsRepo = SettingsRepository(application)
    private val rhythmAnalyzer = RhythmAnalyzer()

    // USTAWIENIA
    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _weeklyGoalHours = MutableStateFlow(5)
    val weeklyGoalHours: StateFlow<Int> = _weeklyGoalHours.asStateFlow()

    // SESJA
    private val _isSessionActive = MutableStateFlow(false)
    val isSessionActive: StateFlow<Boolean> = _isSessionActive.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0)
    val elapsedSeconds: StateFlow<Int> = _elapsedSeconds.asStateFlow()

    private val _exerciseType = MutableStateFlow("Scales")
    val exerciseType: StateFlow<String> = _exerciseType.asStateFlow()

    private val _tuning = MutableStateFlow("E Standard")
    val tuning: StateFlow<String> = _tuning.asStateFlow()

    private val _notes = MutableStateFlow("")
    val notes: StateFlow<String> = _notes.asStateFlow()

    // TUNER (z AudioEngine)
    val tunerResult: StateFlow<TunerResult> = audioEngine.tunerResult

    // DANE
    val allSessions: StateFlow<List<PracticeSession>> = sessionDao.getAllSessions()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // POSTĘP WOBEC CELU
    val weeklyProgress: StateFlow<Float> = allSessions.map { sessions ->
        val weekAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000
        val weekSessions = sessions.filter { it.timestamp >= weekAgo }
        val totalHours = weekSessions.sumOf { it.durationSeconds } / 3600f
        (totalHours / _weeklyGoalHours.value).coerceIn(0f, 1f)
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0f)

    private var timerJob: Job? = null
    private var currentRecordingFile: File? = null

    init {
        viewModelScope.launch {
            _isDarkMode.value = settingsRepo.isDarkMode()
            _weeklyGoalHours.value = settingsRepo.getWeeklyGoal()
        }
    }

    fun toggleSession() {
        if (_isSessionActive.value) {
            stopSession()
        } else {
            startSession()
        }
    }

    private fun startSession() {
        _isSessionActive.value = true
        _elapsedSeconds.value = 0

        // Tworzymy plik do nagrywania
        currentRecordingFile = File(getApplication<Application>().cacheDir, "session_${System.currentTimeMillis()}.wav")

        // Uruchamiamy AudioEngine z zapisem
        audioEngine.start(currentRecordingFile!!)

        timerJob = viewModelScope.launch {
            while (_isSessionActive.value) {
                delay(1000)
                _elapsedSeconds.value++
            }
        }
    }

    private fun stopSession() {
        _isSessionActive.value = false
        timerJob?.cancel()
        audioEngine.stop()
        saveCurrentSession()
    }

    private fun saveCurrentSession() {
        viewModelScope.launch {
            val recordingFile = currentRecordingFile ?: return@launch

            // Analiza BPM z nagranego pliku
            val analysis = rhythmAnalyzer.analyze(recordingFile)

            val session = PracticeSession(
                timestamp = System.currentTimeMillis(),
                durationSeconds = _elapsedSeconds.value.toLong(),
                exerciseType = _exerciseType.value,
                tuning = _tuning.value,
                notes = _notes.value,
                avgBpm = analysis.bpm,
                consistencyScore = analysis.consistency
            )

            sessionDao.insertSession(session)

            // Usuń plik tymczasowy po analizie (opcjonalnie)
            recordingFile.delete()
            currentRecordingFile = null

            _notes.value = ""
            _elapsedSeconds.value = 0
        }
    }

    fun updateExerciseType(type: String) { _exerciseType.value = type }
    fun updateTuning(newTuning: String) { _tuning.value = newTuning }
    fun updateNotes(newNotes: String) { _notes.value = newNotes }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            _isDarkMode.value = enabled
            settingsRepo.setDarkMode(enabled)
        }
    }

    fun setWeeklyGoal(hours: Int) {
        viewModelScope.launch {
            _weeklyGoalHours.value = hours
            settingsRepo.setWeeklyGoal(hours)
        }
    }

    fun exportToCSV(context: Context): String {
        val sessions = allSessions.value
        val csv = StringBuilder("Timestamp,Duration (min),Exercise,Tuning,BPM,Accuracy,Notes\n")

        sessions.forEach { s ->
            csv.append("${s.timestamp},${s.durationSeconds/60},${s.exerciseType},")
            csv.append("${s.tuning},${s.avgBpm},${s.consistencyScore},\"${s.notes}\"\n")
        }

        return csv.toString()
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.stop()
    }
}
