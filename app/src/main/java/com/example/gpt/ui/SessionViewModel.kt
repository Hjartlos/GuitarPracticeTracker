package com.example.gpt.ui

import android.app.Application
import android.content.Context
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gpt.audio.AudioEngine
import com.example.gpt.audio.Metronome
import com.example.gpt.audio.RhythmAnalyzer
import com.example.gpt.audio.ToneGenerator
import com.example.gpt.data.PracticeSession
import com.example.gpt.data.SessionDao
import com.example.gpt.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.compareTo
import kotlin.div
import kotlin.toString

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val sessionDao: SessionDao,
    private val audioEngine: AudioEngine,
    application: Application
) : AndroidViewModel(application) {

    private val settingsRepo = SettingsRepository(application)
    private val rhythmAnalyzer = RhythmAnalyzer()
    private val metronome = Metronome()

    // Odtwarzacz
    private var mediaPlayer: MediaPlayer? = null
    private val _activePlayerSessionId = MutableStateFlow<Int?>(null)
    val activePlayerSessionId = _activePlayerSessionId.asStateFlow()
    private val _isPlayerPlaying = MutableStateFlow(false)
    val isPlayerPlaying = _isPlayerPlaying.asStateFlow()
    private val _playbackProgress = MutableStateFlow(0f)
    val playbackProgress = _playbackProgress.asStateFlow()
    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed = _playbackSpeed.asStateFlow()
    private var playbackJob: Job? = null

    // Audio / Stany
    val tunerResult = audioEngine.tunerResult
    val amplitude = audioEngine.amplitude

    private val _isMetronomeEnabled = MutableStateFlow(false)
    val isMetronomeEnabled = _isMetronomeEnabled.asStateFlow()
    private val _metronomeBpm = MutableStateFlow(100)
    val metronomeBpm = _metronomeBpm.asStateFlow()
    private val _timeSignature = MutableStateFlow("4")
    val timeSignature = _timeSignature.asStateFlow()

    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode = _isDarkMode.asStateFlow()
    private val _weeklyGoalHours = MutableStateFlow(5)
    val weeklyGoalHours = _weeklyGoalHours.asStateFlow()

    private val _inputThreshold = MutableStateFlow(0.15f)
    val inputThreshold = _inputThreshold.asStateFlow()
    private val _rhythmMargin = MutableStateFlow(0.30f)
    val rhythmMargin = _rhythmMargin.asStateFlow()
    // NOWE: Latencja
    private val _latencyOffset = MutableStateFlow(0)
    val latencyOffset = _latencyOffset.asStateFlow()

    private var testMetronomeJob: Job? = null
    private val _isTestMetronomeRunning = MutableStateFlow(false)
    val isTestMetronomeRunning = _isTestMetronomeRunning.asStateFlow()

    private val _isSessionActive = MutableStateFlow(false)
    val isSessionActive = _isSessionActive.asStateFlow()
    private val _elapsedSeconds = MutableStateFlow(0)
    val elapsedSeconds = _elapsedSeconds.asStateFlow()
    private val _exerciseType = MutableStateFlow("Scales")
    val exerciseType = _exerciseType.asStateFlow()
    private val _tuning = MutableStateFlow("E Standard")
    val tuning = _tuning.asStateFlow()
    private val _notes = MutableStateFlow("")
    val notes = _notes.asStateFlow()

    val allSessions = sessionDao.getAllSessions().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val weeklyProgress = allSessions.map { sessions ->
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
            _inputThreshold.value = settingsRepo.getInputThreshold()
            _rhythmMargin.value = settingsRepo.getRhythmMargin()
            _latencyOffset.value = settingsRepo.getLatencyOffset()
            startMonitoring()
        }
    }

    fun toggleTestMetronome() {
        if (_isTestMetronomeRunning.value) {
            stopTestMetronome()
        } else {
            startTestMetronome()
        }
    }

    private fun startTestMetronome() {
        _isTestMetronomeRunning.value = true
        testMetronomeJob = viewModelScope.launch(Dispatchers.Default) {
            while (_isTestMetronomeRunning.value) {
                ToneGenerator.playTone(880f, 100)
                delay(1000)
            }
        }
    }

    fun stopTestMetronome() {
        _isTestMetronomeRunning.value = false
        testMetronomeJob?.cancel()
    }

    fun setInputThreshold(value: Float) { _inputThreshold.value = value; viewModelScope.launch { settingsRepo.setInputThreshold(value) } }
    fun setRhythmMargin(value: Float) { _rhythmMargin.value = value; viewModelScope.launch { settingsRepo.setRhythmMargin(value) } }
    fun setLatencyOffset(ms: Int) { _latencyOffset.value = ms; viewModelScope.launch { settingsRepo.setLatencyOffset(ms) } }

    fun toggleHistoryPlayback(session: PracticeSession) {
        if (_activePlayerSessionId.value == session.id) togglePauseResume() else startNewPlayback(session.audioPath, session.id)
    }
    private fun togglePauseResume() {
        mediaPlayer?.let { if (it.isPlaying) { it.pause(); _isPlayerPlaying.value = false; stopPlaybackTracker() } else { it.start(); _isPlayerPlaying.value = true; startPlaybackTracker() } }
    }
    private fun startNewPlayback(path: String?, sessionId: Int) {
        closePlayer()
        if (path == null) return
        val file = File(path); if (!file.exists()) return
        try {
            mediaPlayer = MediaPlayer().apply { setDataSource(getApplication<Application>(), Uri.fromFile(file)); prepare(); start(); setOnCompletionListener { _isPlayerPlaying.value = false; _playbackProgress.value = 0f; seekTo(0); stopPlaybackTracker() } }
            _activePlayerSessionId.value = sessionId; _isPlayerPlaying.value = true; startPlaybackTracker()
        } catch (e: Exception) { e.printStackTrace(); closePlayer() }
    }
    fun seekAudio(progress: Float) { mediaPlayer?.let { it.seekTo((it.duration * progress).toInt()); _playbackProgress.value = progress } }
    fun setPlaybackSpeed(speed: Float) { _playbackSpeed.value = speed; mediaPlayer?.let { try { val was = it.isPlaying; it.playbackParams = it.playbackParams.apply { this.speed = speed }; if (was && !it.isPlaying) it.start() } catch (e: Exception) {} } }
    private fun startPlaybackTracker() { playbackJob?.cancel(); playbackJob = viewModelScope.launch { while (mediaPlayer?.isPlaying == true) { mediaPlayer?.let { _playbackProgress.value = it.currentPosition.toFloat() / it.duration.toFloat() }; delay(50) } } }
    private fun stopPlaybackTracker() { playbackJob?.cancel() }
    private fun closePlayer() { playbackJob?.cancel(); mediaPlayer?.release(); mediaPlayer = null; _activePlayerSessionId.value = null; _isPlayerPlaying.value = false; _playbackProgress.value = 0f }

    fun startMonitoring() { if (!_isSessionActive.value) audioEngine.start(null) }
    fun stopMonitoring() { if (!_isSessionActive.value) audioEngine.stop() }
    fun toggleMetronomeEnabled(enabled: Boolean) { _isMetronomeEnabled.value = enabled }
    fun setMetronomeBpm(bpm: Int) { _metronomeBpm.value = bpm; metronome.setBpm(bpm) }
    fun setMetronomeTimeSignature(ts: String) { _timeSignature.value = ts; val beats = ts.filter { it.isDigit() }.toIntOrNull() ?: 4; metronome.setTimeSignature(beats) }

    fun toggleSession() { if (_isSessionActive.value) stopSession() else startSession() }
    private fun startSession() {
        closePlayer(); stopTestMetronome(); audioEngine.stop() // Zatrzymaj testy przed sesją
        _isSessionActive.value = true; _elapsedSeconds.value = 0; currentRecordingFile = File(getApplication<Application>().cacheDir, "temp_session.wav")
        viewModelScope.launch(Dispatchers.Default) { if (_isMetronomeEnabled.value) { metronome.setBpm(_metronomeBpm.value); val beats = _timeSignature.value.filter { it.isDigit() }.toIntOrNull() ?: 4; metronome.setTimeSignature(beats); metronome.start() } }
        viewModelScope.launch(Dispatchers.IO) { audioEngine.start(currentRecordingFile!!) }
        timerJob = viewModelScope.launch { val start = System.currentTimeMillis(); while (_isSessionActive.value) { _elapsedSeconds.value = ((System.currentTimeMillis() - start) / 1000).toInt(); delay(100) } }
    }
    private fun stopSession() { _isSessionActive.value = false; timerJob?.cancel(); audioEngine.stop(); metronome.stop(); saveCurrentSession(); startMonitoring() }

    private fun saveCurrentSession() {
        viewModelScope.launch {
            val file = currentRecordingFile ?: return@launch
            if (_elapsedSeconds.value < 5) { file.delete(); return@launch }
            val wasMeta = _isMetronomeEnabled.value
            val result = if (wasMeta) withContext(Dispatchers.IO) {
                rhythmAnalyzer.analyze(file, _metronomeBpm.value, _inputThreshold.value, _rhythmMargin.value, _latencyOffset.value)
            } else com.example.gpt.audio.AnalysisResult(0,0)

            val context = getApplication<Application>(); val dir = File(context.filesDir, "recordings"); if (!dir.exists()) dir.mkdirs()
            val finalFile = File(dir, "rec_${System.currentTimeMillis()}.wav"); withContext(Dispatchers.IO) { file.copyTo(finalFile, true); file.delete() }

            sessionDao.insertSession(PracticeSession(timestamp = System.currentTimeMillis(), durationSeconds = _elapsedSeconds.value.toLong(), exerciseType = _exerciseType.value, tuning = _tuning.value, notes = _notes.value, avgBpm = result.bpm, consistencyScore = result.consistency, timeSignature = if(wasMeta) "${_timeSignature.value}/4" else "-", audioPath = finalFile.absolutePath))
            currentRecordingFile = null; _notes.value = ""; _elapsedSeconds.value = 0
        }
    }

    fun updateExerciseType(type: String) { _exerciseType.value = type }
    fun updateTuning(newTuning: String) { _tuning.value = newTuning }
    fun updateNotes(newNotes: String) { _notes.value = newNotes }
    fun setDarkMode(enabled: Boolean) { viewModelScope.launch { _isDarkMode.value = enabled; settingsRepo.setDarkMode(enabled) } }
    fun setWeeklyGoal(hours: Int) { viewModelScope.launch { _weeklyGoalHours.value = hours; settingsRepo.setWeeklyGoal(hours) } }
    fun exportToCSV(context: Context): String {
        val sessions = allSessions.value
        val csv = StringBuilder("Date,Time,Duration (min),Exercise,Tuning,BPM,Accuracy,TimeSig,Notes\n")

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        sessions.forEach { s ->
            val date = Date(s.timestamp)
            val bpmStr = if (s.avgBpm > 0) s.avgBpm.toString() else "Free"
            val accStr = if (s.avgBpm > 0) "${s.consistencyScore}%" else "-"
            val safeNotes = s.notes.replace("\"", "\"\"")

            csv.append("${dateFormat.format(date)},${timeFormat.format(date)},${s.durationSeconds / 60},${s.exerciseType},${s.tuning},${bpmStr},${accStr},${s.timeSignature},\"${safeNotes}\"\n")
        }
        return csv.toString()
    }
}