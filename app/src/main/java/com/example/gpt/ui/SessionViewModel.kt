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

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val sessionDao: SessionDao,
    private val audioEngine: AudioEngine,
    application: Application
) : AndroidViewModel(application) {

    private val settingsRepo = SettingsRepository(application)
    private val rhythmAnalyzer = RhythmAnalyzer()
    private val metronome = Metronome()

    private var mediaPlayer: MediaPlayer? = null
    private val _currentlyPlayingSessionId = MutableStateFlow<Int?>(null)
    val currentlyPlayingSessionId = _currentlyPlayingSessionId.asStateFlow()

    private val _playbackProgress = MutableStateFlow(0f)
    val playbackProgress = _playbackProgress.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed = _playbackSpeed.asStateFlow()

    private var playbackJob: Job? = null

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
            startMonitoring()
        }
    }

    fun toggleAudioPlayback(session: PracticeSession) {
        if (_currentlyPlayingSessionId.value == session.id) {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
            } else {
                mediaPlayer?.start()
                startPlaybackTracker()
            }
        } else {
            playAudio(session)
        }
    }

    private fun playAudio(session: PracticeSession) {
        stopAudioPlayback()
        val path = session.audioPath ?: return
        val file = File(path)
        if (!file.exists()) return

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(getApplication<Application>(), Uri.fromFile(file))
                prepare()
                // Ustaw prędkość
                val params = PlaybackParams()
                params.speed = _playbackSpeed.value
                playbackParams = params

                start()
                setOnCompletionListener {
                    _playbackProgress.value = 1f
                    stopPlaybackTracker()
                    _currentlyPlayingSessionId.value = null
                }
            }
            _currentlyPlayingSessionId.value = session.id
            startPlaybackTracker()
        } catch (e: Exception) {
            e.printStackTrace()
            stopAudioPlayback()
        }
    }

    fun seekAudio(progress: Float) {
        mediaPlayer?.let { player ->
            val newPos = (player.duration * progress).toInt()
            player.seekTo(newPos)
            _playbackProgress.value = progress
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        mediaPlayer?.let { player ->
            if (player.isPlaying || _currentlyPlayingSessionId.value != null) {
                try {
                    val params = player.playbackParams
                    params.speed = speed
                    player.playbackParams = params
                } catch (e: Exception) { e.printStackTrace() }
            }
        }
    }

    private fun startPlaybackTracker() {
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            while (mediaPlayer?.isPlaying == true) {
                val current = mediaPlayer?.currentPosition ?: 0
                val total = mediaPlayer?.duration ?: 1
                if (total > 0) {
                    _playbackProgress.value = current.toFloat() / total.toFloat()
                }
                delay(50)
            }
        }
    }

    private fun stopPlaybackTracker() {
        playbackJob?.cancel()
    }

    private fun stopAudioPlayback() {
        playbackJob?.cancel()
        mediaPlayer?.release()
        mediaPlayer = null
        _currentlyPlayingSessionId.value = null
        _playbackProgress.value = 0f
    }

    fun setInputThreshold(value: Float) {
        _inputThreshold.value = value
        viewModelScope.launch { settingsRepo.setInputThreshold(value) }
    }

    fun setRhythmMargin(value: Float) {
        _rhythmMargin.value = value
        viewModelScope.launch { settingsRepo.setRhythmMargin(value) }
    }

    fun startMonitoring() { if (!_isSessionActive.value) audioEngine.start(null) }
    fun stopMonitoring() { if (!_isSessionActive.value) audioEngine.stop() }

    fun toggleMetronomeEnabled(enabled: Boolean) { _isMetronomeEnabled.value = enabled }
    fun setMetronomeBpm(bpm: Int) { _metronomeBpm.value = bpm; metronome.setBpm(bpm) }
    fun setMetronomeTimeSignature(ts: String) { _timeSignature.value = ts; val beats = ts.filter { it.isDigit() }.toIntOrNull() ?: 4; metronome.setTimeSignature(beats) }

    fun toggleSession() { if (_isSessionActive.value) stopSession() else startSession() }

    private fun startSession() {
        stopAudioPlayback()
        audioEngine.stop()
        _isSessionActive.value = true
        _elapsedSeconds.value = 0
        currentRecordingFile = File(getApplication<Application>().cacheDir, "temp_session.wav")

        viewModelScope.launch(Dispatchers.Default) {
            if (_isMetronomeEnabled.value) {
                metronome.setBpm(_metronomeBpm.value)
                val beats = _timeSignature.value.filter { it.isDigit() }.toIntOrNull() ?: 4
                metronome.setTimeSignature(beats)
                metronome.start()
            }
        }
        viewModelScope.launch(Dispatchers.IO) { audioEngine.start(currentRecordingFile!!) }
        timerJob = viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            while (_isSessionActive.value) {
                val now = System.currentTimeMillis()
                _elapsedSeconds.value = ((now - startTime) / 1000).toInt()
                delay(100)
            }
        }
    }

    private fun stopSession() {
        _isSessionActive.value = false
        timerJob?.cancel()
        audioEngine.stop()
        metronome.stop()
        saveCurrentSession()
        startMonitoring()
    }

    private fun saveCurrentSession() {
        viewModelScope.launch {
            val recordingFile = currentRecordingFile ?: return@launch
            val duration = _elapsedSeconds.value
            if (duration < 5) { recordingFile.delete(); _notes.value = ""; _elapsedSeconds.value = 0; return@launch }

            val wasMetronomeOn = _isMetronomeEnabled.value
            val targetBpm = if (wasMetronomeOn) _metronomeBpm.value else 0
            var bpmResult = 0
            var consistencyResult = 0

            if (wasMetronomeOn) {
                val analysis = withContext(Dispatchers.IO) { rhythmAnalyzer.analyze(recordingFile, targetBpm, _inputThreshold.value, _rhythmMargin.value) }
                bpmResult = analysis.bpm
                consistencyResult = analysis.consistency
            }

            val context = getApplication<Application>()
            val recordingsDir = File(context.filesDir, "recordings")
            if (!recordingsDir.exists()) recordingsDir.mkdirs()
            val fileName = "rec_${System.currentTimeMillis()}.wav"
            val finalFile = File(recordingsDir, fileName)

            withContext(Dispatchers.IO) {
                recordingFile.copyTo(finalFile, overwrite = true)
                recordingFile.delete()
            }

            val session = PracticeSession(
                timestamp = System.currentTimeMillis(),
                durationSeconds = duration.toLong(),
                exerciseType = _exerciseType.value,
                tuning = _tuning.value,
                notes = _notes.value,
                avgBpm = bpmResult,
                consistencyScore = consistencyResult,
                timeSignature = if(wasMetronomeOn) "${_timeSignature.value}/4" else "-",
                audioPath = finalFile.absolutePath
            )
            sessionDao.insertSession(session)
            currentRecordingFile = null
            _notes.value = ""
            _elapsedSeconds.value = 0
        }
    }

    // Helpery
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
            val bpmStr = if(s.avgBpm > 0) s.avgBpm.toString() else "Free"
            val accStr = if(s.avgBpm > 0) "${s.consistencyScore}%" else "-"
            csv.append("${dateFormat.format(date)},${timeFormat.format(date)},${s.durationSeconds/60},${s.exerciseType},${s.tuning},${bpmStr},${accStr},${s.timeSignature},\"${s.notes}\"\n")
        }
        return csv.toString()
    }
}