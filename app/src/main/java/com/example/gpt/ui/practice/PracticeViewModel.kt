package com.example.gpt.ui.practice

import android.app.Application
import android.content.Context
import android.os.PowerManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import com.example.gpt.R
import com.example.gpt.core.audio.AnalysisResult
import com.example.gpt.core.audio.AudioEngine
import com.example.gpt.core.audio.AudioPlaybackManager
import com.example.gpt.core.audio.AudioUtils
import com.example.gpt.core.audio.BeatType
import com.example.gpt.core.audio.Metronome
import com.example.gpt.core.audio.RhythmAnalyzer
import com.example.gpt.core.audio.ToneGenerator
import com.example.gpt.core.haptic.HapticManager
import com.example.gpt.core.notifications.NotificationHelper
import com.example.gpt.data.local.entity.AchievementType
import com.example.gpt.data.local.entity.PracticeSession
import com.example.gpt.data.local.dao.SessionDao
import com.example.gpt.data.repository.AchievementRepository
import com.example.gpt.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class PracticeViewModel @Inject constructor(
    private val sessionDao: SessionDao,
    private val audioEngine: AudioEngine,
    private val settingsRepo: SettingsRepository,
    private val hapticManager: HapticManager,
    private val achievementRepository: AchievementRepository,
    application: Application
) : AndroidViewModel(application), LifecycleEventObserver {

    private val rhythmAnalyzer = RhythmAnalyzer()
    private val metronome = Metronome()
    private val playbackManager = AudioPlaybackManager(application, viewModelScope)
    val tunerResult = audioEngine.tunerResult
    val amplitude = audioEngine.amplitude
    val activePlayerSessionId = playbackManager.activeSessionId
    val isPlayerPlaying = playbackManager.isPlaying
    val playbackProgress = playbackManager.progress
    val playbackSpeed = playbackManager.speed
    private val _isHapticEnabled = MutableStateFlow(true)
    val isHapticEnabled = _isHapticEnabled.asStateFlow()
    val allAchievements = achievementRepository.allAchievements
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val unlockedAchievementsCount = achievementRepository.unlockedCount
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)
    val totalAchievementsCount = achievementRepository.totalCount
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)
    private val _newlyUnlockedAchievement = MutableStateFlow<AchievementType?>(null)
    val newlyUnlockedAchievement = _newlyUnlockedAchievement.asStateFlow()
    private val _isMetronomeEnabled = MutableStateFlow(false)
    val isMetronomeEnabled = _isMetronomeEnabled.asStateFlow()

    private val _metronomeBpm = MutableStateFlow(100)
    val metronomeBpm = _metronomeBpm.asStateFlow()

    private val _timeSignature = MutableStateFlow("4/4")
    val timeSignature = _timeSignature.asStateFlow()

    private val _beatPattern = MutableStateFlow<List<BeatType>>(
        listOf(BeatType.ACCENT, BeatType.NORMAL, BeatType.NORMAL, BeatType.NORMAL)
    )
    val beatPattern = _beatPattern.asStateFlow()

    private val _currentPlayingBeat = MutableStateFlow(0)
    val currentPlayingBeat = _currentPlayingBeat.asStateFlow()

    private val _bpm = MutableStateFlow(120)
    val bpm = _bpm.asStateFlow()

    private val _isMetronomePlaying = MutableStateFlow(false)
    val isMetronomePlaying = _isMetronomePlaying.asStateFlow()
    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode = _isDarkMode.asStateFlow()

    private val _weeklyGoalHours = MutableStateFlow(5)
    val weeklyGoalHours = _weeklyGoalHours.asStateFlow()

    private val _inputThreshold = MutableStateFlow(0.02f)
    val inputThreshold = _inputThreshold.asStateFlow()

    private val _rhythmMargin = MutableStateFlow(0.30f)
    val rhythmMargin = _rhythmMargin.asStateFlow()

    private val _latencyOffset = MutableStateFlow(0)
    val latencyOffset = _latencyOffset.asStateFlow()

    private val _metronomeOffset = MutableStateFlow(0)
    val metronomeOffset = _metronomeOffset.asStateFlow()

    private val _isRhythmAnalysisEnabled = MutableStateFlow(true)

    val isRhythmAnalysisEnabled = _isRhythmAnalysisEnabled.asStateFlow()

    private val _baseFrequency = MutableStateFlow(440)
    val baseFrequency = _baseFrequency.asStateFlow()

    private val _useFlats = MutableStateFlow(false)
    val useFlats = _useFlats.asStateFlow()

    private val _currentLanguage = MutableStateFlow("en")
    val currentLanguage = _currentLanguage.asStateFlow()
    private val _detailedAnalysisResults = MutableStateFlow<Map<Int, AnalysisResult>>(emptyMap())
    val detailedAnalysisResults = _detailedAnalysisResults.asStateFlow()

    private val _isAnalyzingSession = MutableStateFlow<Map<Int, Boolean>>(emptyMap())
    val isAnalyzingSession = _isAnalyzingSession.asStateFlow()
    private var testMetronomeJob: Job? = null
    private val _isTestMetronomeRunning = MutableStateFlow(false)
    val isTestMetronomeRunning = _isTestMetronomeRunning.asStateFlow()

    private var syncTestJob: Job? = null
    private val _isSyncTestRunning = MutableStateFlow(false)

    private val _isSessionActive = MutableStateFlow(false)
    val isSessionActive = _isSessionActive.asStateFlow()

    private val _isCountingIn = MutableStateFlow(false)
    val isCountingIn = _isCountingIn.asStateFlow()

    private var calibrationJob: Job? = null

    private val _elapsedSeconds = MutableStateFlow(0)
    val elapsedSeconds = _elapsedSeconds.asStateFlow()

    private val _exerciseType = MutableStateFlow("")
    val exerciseType = _exerciseType.asStateFlow()

    private val _tuning = MutableStateFlow("E Standard")
    val tuning = _tuning.asStateFlow()

    private val _notes = MutableStateFlow("")
    val notes = _notes.asStateFlow()

    private val _exerciseTypeError = MutableStateFlow(false)
    val exerciseTypeError = _exerciseTypeError.asStateFlow()

    private val _tuningError = MutableStateFlow(false)
    val tuningError = _tuningError.asStateFlow()
    private val _latencyTestResult = MutableStateFlow<String?>(null)
    val latencyTestResult = _latencyTestResult.asStateFlow()

    private val _isLatencyTesting = MutableStateFlow(false)
    val isLatencyTesting = _isLatencyTesting.asStateFlow()
    private val _isTapCalibrating = MutableStateFlow(false)
    val isTapCalibrating = _isTapCalibrating.asStateFlow()

    private val _tapCalibrationBeat = MutableStateFlow(0)
    val tapCalibrationBeat = _tapCalibrationBeat.asStateFlow()

    private val _tapCalibrationProgress = MutableStateFlow("")
    val tapCalibrationProgress = _tapCalibrationProgress.asStateFlow()

    private val _isMonitoringEnabled = MutableStateFlow(false)
    val isMonitoringEnabled = _isMonitoringEnabled.asStateFlow()

    private var tapTimestamps = mutableListOf<Long>()
    private var clickTimestamps = mutableListOf<Long>()
    val allSessions = sessionDao.getAllSessions()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val weeklyProgress = allSessions.map { sessions ->
        val weekAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000
        val weekSessions = sessions.filter { it.timestamp >= weekAgo }
        val totalHours = weekSessions.sumOf { it.durationSeconds } / 3600f
        (totalHours / _weeklyGoalHours.value).coerceIn(0f, 1f)
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0f)

    val streakDays: StateFlow<Int> = achievementRepository.observeCurrentStreak()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )
    private var timerJob: Job? = null
    private var sessionStartupJob: Job? = null
    private var currentRecordingFile: File? = null

    private var effectiveBpmValue = 100

    private val recordedMetronomeClicks = mutableListOf<Double>()
    private var sessionStartTime: Long = 0
    private var wakeLock: PowerManager.WakeLock? = null

    init {
        initializeSettings()
        setupMetronomeCallback()
        initializeAchievements()

        val powerManager = application.getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "GuitarTracker:RecordingWakeLock")
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        if (event == Lifecycle.Event.ON_STOP) {
            onAppBackgrounded()
        }
    }

    private fun onAppBackgrounded() {
        if (_isSessionActive.value) {
        } else {
            stopAudioPlayback()
            stopTestMetronome()
            stopSyncTest()
            audioEngine.stop()
        }
    }

    private fun initializeAchievements() {
        viewModelScope.launch {
            achievementRepository.initializeAchievements()
        }
    }

    private fun initializeSettings() {
        viewModelScope.launch {
            _isDarkMode.value = settingsRepo.isDarkMode()
            _weeklyGoalHours.value = settingsRepo.getWeeklyGoal()
            _currentLanguage.value = settingsRepo.getLanguage()
            _inputThreshold.value = settingsRepo.getInputThreshold()
            _rhythmMargin.value = settingsRepo.getRhythmMargin()
            _latencyOffset.value = settingsRepo.getLatencyOffset()
            _metronomeOffset.value = settingsRepo.getMetronomeOffset()
            _isHapticEnabled.value = settingsRepo.isHapticEnabled()
            audioEngine.currentThreshold = _inputThreshold.value
            hapticManager.setEnabled(_isHapticEnabled.value)
            if (_exerciseType.value.isEmpty()) {
                _exerciseType.value = getApplication<Application>().getString(R.string.ex_scales)
            }
        }
        viewModelScope.launch {
            _currentLanguage
                .drop(1)
                .collect { _ ->
                    _exerciseType.value = ""
                }
        }
    }

    fun triggerTestVibration() {
        hapticManager.performTestVibration()
    }

    private fun setupMetronomeCallback() {
        metronome.onBeatTick = { beatIndex, isAccent ->
            val delayMs = _metronomeOffset.value.toLong()

            if (_isSessionActive.value && sessionStartTime > 0) {
                val clickTimeSeconds = (System.currentTimeMillis() - sessionStartTime) / 1000.0
                recordedMetronomeClicks.add(clickTimeSeconds)
            }

            viewModelScope.launch(Dispatchers.Default) {
                if (delayMs > 0) delay(delayMs)

                withContext(Dispatchers.Main.immediate) {
                    _currentPlayingBeat.value = beatIndex + 1

                    val shouldVibrate = _isHapticEnabled.value || _isSyncTestRunning.value

                    if (shouldVibrate) {
                        if (_isSyncTestRunning.value) {
                            if (isAccent) hapticManager.syncBeat()
                        } else {
                            if (effectiveBpmValue > 220) {
                                if (isAccent) hapticManager.accentBeat()
                            } else {
                                if (isAccent) hapticManager.accentBeat() else hapticManager.mediumTap()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun updateMetronomeSpeed() {
        val ts = _timeSignature.value
        val denominator = ts.split("/").getOrNull(1)?.toIntOrNull() ?: 4
        val multiplier = when (denominator) {
            8 -> 2
            16 -> 4
            else -> 1
        }

        val baseBpm = _metronomeBpm.value
        effectiveBpmValue = baseBpm * multiplier

        metronome.setBpm(effectiveBpmValue)
    }

    fun toggleRhythmAnalysis(enabled: Boolean) {
        _isRhythmAnalysisEnabled.value = enabled
    }

    fun analyzeSessionRhythm(session: PracticeSession) {
        if (session.audioPath == null || _detailedAnalysisResults.value.containsKey(session.id)) return

        val currentLoading = _isAnalyzingSession.value.toMutableMap()
        currentLoading[session.id] = true
        _isAnalyzingSession.value = currentLoading

        viewModelScope.launch(Dispatchers.IO) {
            val file = File(session.audioPath)

            val ts = session.timeSignature
            val denominator = ts.split("/").getOrNull(1)?.toIntOrNull() ?: 4

            val result = rhythmAnalyzer.analyze(
                audioFile = file,
                targetBpm = session.avgBpm,
                timeSignatureDenominator = denominator,
                threshold = _inputThreshold.value,
                errorMargin = _rhythmMargin.value,
                latencyMs = _latencyOffset.value
            )

            withContext(Dispatchers.Main) {
                val currentResults = _detailedAnalysisResults.value.toMutableMap()
                currentResults[session.id] = result
                _detailedAnalysisResults.value = currentResults

                val loading = _isAnalyzingSession.value.toMutableMap()
                loading.remove(session.id)
                _isAnalyzingSession.value = loading
            }
        }
    }

    fun toggleTestMetronome() {
        if (_isTestMetronomeRunning.value) stopTestMetronome() else startTestMetronome()
    }

    private fun startTestMetronome() {
        _isTestMetronomeRunning.value = true
        testMetronomeJob = viewModelScope.launch(Dispatchers.Default) {
            while (_isTestMetronomeRunning.value) {
                ToneGenerator.playMetronomeClick()
                delay(1000)
            }
        }
    }

    fun stopTestMetronome() {
        _isTestMetronomeRunning.value = false
        testMetronomeJob?.cancel()
    }

    fun startSyncTest() {
        if (_isSyncTestRunning.value) return
        _isSyncTestRunning.value = true
        metronome.setBpm(100)
        metronome.setTimeSignature(4)
        effectiveBpmValue = 100

        metronome.start()
    }

    fun stopSyncTest() {
        _isSyncTestRunning.value = false
        metronome.stop()
    }

    fun runLatencyAutoCalibration() {
        startAcousticCalibration()
    }

    private fun startAcousticCalibration() {
        if (_isTapCalibrating.value) return

        val context = getApplication<Application>()
        _isTapCalibrating.value = true
        _isLatencyTesting.value = true
        _tapCalibrationBeat.value = 0
        _tapCalibrationProgress.value = context.getString(R.string.tap_calibration_ready)

        startMonitoring()

        calibrationJob = viewModelScope.launch(Dispatchers.Default) {
            delay(1000)

            val detectedLatencies = mutableListOf<Long>()
            val iterations = 5

            val triggerThreshold = 10f

            for (i in 1..iterations) {
                withContext(Dispatchers.Main) {
                    _tapCalibrationProgress.value = context.getString(R.string.calibration_step_fmt, i, iterations)
                    _tapCalibrationBeat.value = i
                }

                delay(500)

                val startTime = System.nanoTime()
                ToneGenerator.playCalibrationTone(1000f, 100)

                var beepDetected = false
                val timeoutMs = 800L
                val loopStart = System.currentTimeMillis()

                while (System.currentTimeMillis() - loopStart < timeoutMs) {
                    if (amplitude.value > triggerThreshold) {
                        val endTime = System.nanoTime()
                        val latencyNs = endTime - startTime
                        val latencyMs = latencyNs / 1_000_000

                        if (latencyMs > 10) {
                            detectedLatencies.add(latencyMs)
                            beepDetected = true
                            break
                        }
                    }
                    delay(5)
                }

                if (!beepDetected) {
                    android.util.Log.w("Calibration", "Beep not detected in iteration $i")
                }

                delay(400)
            }

            withContext(Dispatchers.Main) {
                if (detectedLatencies.isNotEmpty()) {
                    val sorted = detectedLatencies.sorted()
                    val validData = if (sorted.size >= 3) {
                        sorted.subList(1, sorted.size - 1)
                    } else sorted

                    val avgLatency = validData.average().toInt()
                    val finalLatency = (avgLatency - 20).coerceIn(0, 300)

                    setLatencyOffset(finalLatency)

                    _latencyTestResult.value = context.getString(R.string.latency_measured, finalLatency)
                    _tapCalibrationProgress.value = context.getString(R.string.tap_calibration_done)
                } else {
                    _latencyTestResult.value = context.getString(R.string.tap_calibration_failed)
                    _tapCalibrationProgress.value = context.getString(R.string.latency_failed)
                }

                finishTapCalibration()
            }
        }
    }

    private fun finishTapCalibration() {
        viewModelScope.launch {
            delay(2000)
            _isTapCalibrating.value = false
            _isLatencyTesting.value = false
            _latencyTestResult.value = null
            _tapCalibrationProgress.value = ""
        }
    }

    fun cancelTapCalibration() {
        calibrationJob?.cancel()
        _isTapCalibrating.value = false
        _isLatencyTesting.value = false
        _tapCalibrationProgress.value = ""
    }

    fun stopAudioPlayback() {
        playbackManager.release()
    }

    fun toggleAudioMonitoring() {
        val newState = !_isMonitoringEnabled.value
        _isMonitoringEnabled.value = newState
        audioEngine.toggleMonitoring(newState)
    }

    fun stopAudioMonitoring() {
        _isMonitoringEnabled.value = false
        audioEngine.toggleMonitoring(false)
    }

    fun setInputThreshold(value: Float) {
        _inputThreshold.value = value
        audioEngine.currentThreshold = value
        viewModelScope.launch { settingsRepo.setInputThreshold(value) }
    }

    fun setRhythmMargin(value: Float) {
        _rhythmMargin.value = value
        viewModelScope.launch { settingsRepo.setRhythmMargin(value) }
    }

    fun setLatencyOffset(ms: Int) {
        _latencyOffset.value = ms
        viewModelScope.launch { settingsRepo.setLatencyOffset(ms) }
    }

    fun setMetronomeOffset(ms: Int) {
        _metronomeOffset.value = ms
        viewModelScope.launch { settingsRepo.setMetronomeOffset(ms) }
    }

    fun setHapticEnabled(enabled: Boolean) {
        _isHapticEnabled.value = enabled
        hapticManager.setEnabled(enabled)
        viewModelScope.launch { settingsRepo.setHapticEnabled(enabled) }
    }

    fun dismissAchievementToast() {
        _newlyUnlockedAchievement.value = null
    }

    fun setBaseFrequency(freq: Int) {
        _baseFrequency.value = freq
        AudioUtils.referenceFrequency = freq.toFloat()
    }

    fun setUseFlats(enabled: Boolean) {
        _useFlats.value = enabled
    }

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

    fun setAppLanguage(code: String) {
        viewModelScope.launch {
            settingsRepo.setLanguage(code)
            _currentLanguage.value = code
        }
    }
    fun toggleHistoryPlayback(session: PracticeSession) {
        playbackManager.togglePlayback(session.audioPath, session.id)
    }

    fun seekAudio(progress: Float) {
        playbackManager.seek(progress)
    }

    fun setPlaybackSpeed(speed: Float) {
        playbackManager.setSpeed(speed)
    }
    fun startMonitoring() {
        if (!_isSessionActive.value) {
            audioEngine.start(null)
        }
    }

    fun stopMonitoring() {
        if (!_isSessionActive.value) {
            audioEngine.stop()
        }
    }
    fun toggleMetronomeEnabled(enabled: Boolean) {
        _isMetronomeEnabled.value = enabled
    }

    fun setMetronomeBpm(bpm: Int) {
        val ts = _timeSignature.value
        val denominator = ts.split("/").getOrNull(1)?.toIntOrNull() ?: 4

        val maxAllowed = when(denominator) {
            8 -> 150
            16 -> 75
            else -> 300
        }

        val safeBpm = bpm.coerceIn(20, maxAllowed)
        _metronomeBpm.value = safeBpm
        updateMetronomeSpeed()
    }

    fun setMetronomeTimeSignature(ts: String) {
        val parts = ts.split("/")
        val beats = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(1, 27) ?: 4
        val value = parts.getOrNull(1)?.toIntOrNull() ?: 4

        _timeSignature.value = "$beats/$value"

        val currentBpm = _metronomeBpm.value
        if (value == 8 && currentBpm > 150) {
            _metronomeBpm.value = 150
        } else if (value == 16 && currentBpm > 75) {
            _metronomeBpm.value = 75
        }

        val newPattern = List(beats) { index ->
            if (index == 0) BeatType.ACCENT else BeatType.NORMAL
        }
        _beatPattern.value = newPattern
        metronome.setPattern(newPattern, value)
        updateMetronomeSpeed()
    }

    fun toggleBeatType(index: Int) {
        val currentPattern = _beatPattern.value.toMutableList()
        if (index in currentPattern.indices) {
            val nextType = when (currentPattern[index]) {
                BeatType.ACCENT -> BeatType.NORMAL
                BeatType.NORMAL -> BeatType.MUTE
                BeatType.MUTE -> BeatType.ACCENT
            }
            currentPattern[index] = nextType
            _beatPattern.value = currentPattern

            val parts = _timeSignature.value.split("/")
            val value = parts.getOrNull(1)?.toIntOrNull() ?: 4
            metronome.setPattern(currentPattern, value)
        }
    }

    fun toggleSession() {
        if (_isSessionActive.value) {
            stopSession()
        } else {
            var hasError = false
            if (_exerciseType.value.isBlank()) {
                _exerciseTypeError.value = true
                hasError = true
            }
            if (_tuning.value.isBlank()) {
                _tuningError.value = true
                hasError = true
            }
            if (!hasError) startSession()
        }
    }

    private fun startSession() {
        playbackManager.release()
        stopTestMetronome()
        audioEngine.stop()

        _isSessionActive.value = true
        _elapsedSeconds.value = 0
        currentRecordingFile = File(getApplication<Application>().cacheDir, "temp_session.wav")

        recordedMetronomeClicks.clear()

        try {
            if (wakeLock?.isHeld == false) {
                wakeLock?.acquire(10 * 60 * 1000L)
            }
        } catch (e: Exception) { e.printStackTrace() }

        sessionStartupJob = viewModelScope.launch(Dispatchers.Main) {
            if (_isMetronomeEnabled.value) {
                updateMetronomeSpeed()

                val parts = _timeSignature.value.split("/")
                val value = parts.getOrNull(1)?.toIntOrNull() ?: 4
                metronome.setPattern(_beatPattern.value, value)

                _isCountingIn.value = true
                metronome.start()

                val beatDurationMs = (60_000.0 / effectiveBpmValue)
                val barDurationMs = (beatDurationMs * _beatPattern.value.size).toLong()

                delay(barDurationMs)
                _isCountingIn.value = false
            }

            withContext(Dispatchers.IO) {
                audioEngine.start(currentRecordingFile!!)
            }
            sessionStartTime = System.currentTimeMillis()

            timerJob = launch {
                val start = System.currentTimeMillis()
                while (_isSessionActive.value) {
                    _elapsedSeconds.value = ((System.currentTimeMillis() - start) / 1000).toInt()
                    delay(100)
                }
            }
        }
    }

    private fun stopSession() {
        sessionStartupJob?.cancel()
        _isCountingIn.value = false
        _isSessionActive.value = false
        timerJob?.cancel()
        metronome.stop()
        _currentPlayingBeat.value = 1

        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) { e.printStackTrace() }

        saveCurrentSession()

        startMonitoring()
    }

    private fun saveCurrentSession() {
        val currentBpm = _metronomeBpm.value
        val currentTs = _timeSignature.value
        val wasMetronomeOn = _isMetronomeEnabled.value
        val isAnalysisRequested = _isRhythmAnalysisEnabled.value

        val file = currentRecordingFile
        val duration = _elapsedSeconds.value
        val isCountingInState = _isCountingIn.value

        viewModelScope.launch(Dispatchers.IO) {
            audioEngine.stop()

            if (duration < 5 && !isCountingInState) {
                withContext(Dispatchers.Main) {
                    currentRecordingFile = null
                    _notes.value = ""
                    _elapsedSeconds.value = 0
                }
                return@launch
            }

            if (file == null || !file.exists()) return@launch

            val shouldAnalyze = wasMetronomeOn && isAnalysisRequested

            val denominator = currentTs.split("/").getOrNull(1)?.toIntOrNull() ?: 4

            val multiplier = when (denominator) {
                8 -> 2
                16 -> 4
                else -> 1
            }

            val analysisTargetBpm = if (shouldAnalyze) currentBpm * multiplier else 0

            val clicksToFilter = if (shouldAnalyze) recordedMetronomeClicks else emptyList()

            val result = rhythmAnalyzer.analyze(
                audioFile = file,
                targetBpm = analysisTargetBpm,
                timeSignatureDenominator = denominator,
                threshold = _inputThreshold.value,
                errorMargin = _rhythmMargin.value,
                latencyMs = _latencyOffset.value,
                metronomeClicksSeconds = clicksToFilter
            )

            val context = getApplication<Application>()
            val dir = File(context.filesDir, "recordings")
            if (!dir.exists()) dir.mkdirs()

            val finalFile = File(dir, "rec_${System.currentTimeMillis()}.wav")

            try {
                file.copyTo(finalFile, true)
                file.delete()
            } catch (e: Exception) {
                e.printStackTrace()
                return@launch
            }

            val dbBpm = if (wasMetronomeOn) currentBpm else 0

            val savedConsistency = if (shouldAnalyze) result.consistency else 0

            sessionDao.insertSession(
                PracticeSession(
                    timestamp = System.currentTimeMillis(),
                    durationSeconds = duration.toLong(),
                    exerciseType = _exerciseType.value,
                    tuning = _tuning.value,
                    notes = _notes.value,
                    avgBpm = dbBpm,
                    consistencyScore = savedConsistency,
                    timeSignature = if (wasMetronomeOn) currentTs else "-",
                    audioPath = finalFile.absolutePath
                )
            )

            val newlyUnlocked = achievementRepository.checkAchievementsAfterSession(
                sessionDurationMinutes = duration.toLong() / 60,
                bpm = _metronomeBpm.value,
                exerciseType = _exerciseType.value,
                isMetronomeSession = shouldAnalyze,
                consistencyScore = savedConsistency
            )

            withContext(Dispatchers.Main) {
                if (newlyUnlocked.isNotEmpty()) {
                    _newlyUnlockedAchievement.value = newlyUnlocked.first()
                    hapticManager.successPattern()
                }

                checkDailyGoalAchievement(context)

                currentRecordingFile = null
                _notes.value = ""
                _elapsedSeconds.value = 0
            }
        }
    }

    private suspend fun checkDailyGoalAchievement(context: android.content.Context) {
        if (!NotificationHelper.areGoalNotificationsEnabled(context)) return

        val weeklyGoal = _weeklyGoalHours.value
        val dailyGoalMinutes = NotificationHelper.getDailyGoalMinutes(weeklyGoal)
        val todayStart = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis

        val todaySessions = allSessions.value.filter { it.timestamp >= todayStart }
        val totalMinutesToday = todaySessions.sumOf { it.durationSeconds } / 60

        if (totalMinutesToday >= dailyGoalMinutes) {
            val goalText = context.getString(R.string.daily_goal_type_fmt, dailyGoalMinutes)
            NotificationHelper.showGoalAchievedNotification(context, goalText)
        }
    }

    fun updateExerciseType(type: String) {
        _exerciseType.value = type
        if (type.isNotBlank()) _exerciseTypeError.value = false
    }

    fun updateTuning(newTuning: String) {
        _tuning.value = newTuning
        if (newTuning.isNotBlank()) _tuningError.value = false
    }

    fun updateNotes(newNotes: String) {
        _notes.value = newNotes
    }
    fun exportToCSV(): String {
        val sessions = allSessions.value
        val csv = StringBuilder("Date,Time,Duration (sec),Exercise,Tuning,BPM,Accuracy,TimeSig,Notes,RawTimestamp\n")

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        sessions.forEach { s ->
            val date = Date(s.timestamp)
            val bpmStr = if (s.avgBpm > 0) s.avgBpm.toString() else "Free"
            val accStr = if (s.avgBpm > 0) "${s.consistencyScore}%" else "-"

            val safeNotes = s.notes.replace("\"", "\"\"")

            csv.append(
                "${dateFormat.format(date)},${timeFormat.format(date)},${s.durationSeconds}," +
                        "${s.exerciseType},${s.tuning},${bpmStr},${accStr},${s.timeSignature},\"${safeNotes}\",${s.timestamp}\n"
            )
        }
        return csv.toString()
    }

    fun importFromCSV(csvContent: String): Result<Int> {
        return try {
            val lines = csvContent.lines().filter { it.isNotBlank() }
            if (lines.size < 2) return Result.failure(Exception("No data to import"))

            val header = lines.first().lowercase()
            val isSecondsFormat = header.contains("duration (sec)")
            val hasRawTimestamp = header.contains("rawtimestamp")

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            var importedCount = 0

            viewModelScope.launch(Dispatchers.IO) {
                lines.drop(1).forEach { line ->
                    try {
                        val parts = parseCSVLine(line)
                        if (parts.size >= 5) {
                            val dateStr = parts[0]
                            val timeStr = parts.getOrNull(1) ?: "12:00"

                            val durationRaw = parts[2].toLongOrNull() ?: 0L
                            val durationSeconds = if (isSecondsFormat) durationRaw else durationRaw * 60L

                            val exerciseType = parts[3]
                            val tuning = parts[4]
                            val bpmStr = parts.getOrNull(5) ?: "0"
                            val accuracyStr = parts.getOrNull(6) ?: "0"
                            val timeSig = parts.getOrNull(7) ?: "4/4"
                            val notes = parts.getOrNull(8) ?: ""

                            val rawTimestampStr = parts.getOrNull(9)

                            val finalTimestamp = if (hasRawTimestamp && rawTimestampStr != null) {
                                rawTimestampStr.toLongOrNull() ?: System.currentTimeMillis()
                            } else {
                                val date = dateFormat.parse(dateStr) ?: Date()
                                val time = try { timeFormat.parse(timeStr) } catch (_: Exception) { null }
                                val calendar = java.util.Calendar.getInstance().apply {
                                    setTime(date)
                                    if (time != null) {
                                        val timeCal = java.util.Calendar.getInstance().apply { setTime(time) }
                                        set(java.util.Calendar.HOUR_OF_DAY, timeCal.get(java.util.Calendar.HOUR_OF_DAY))
                                        set(java.util.Calendar.MINUTE, timeCal.get(java.util.Calendar.MINUTE))
                                        set(java.util.Calendar.SECOND, 0)
                                        set(java.util.Calendar.MILLISECOND, 0)
                                    }
                                }
                                calendar.timeInMillis
                            }

                            val exists = sessionDao.sessionExists(finalTimestamp, durationSeconds, exerciseType)

                            if (!exists) {
                                val bpm = bpmStr.replace("Free", "0").toIntOrNull() ?: 0
                                val accuracy = accuracyStr.replace("%", "").replace("-", "0").toIntOrNull() ?: 0

                                val session = PracticeSession(
                                    timestamp = finalTimestamp,
                                    durationSeconds = durationSeconds,
                                    exerciseType = exerciseType,
                                    tuning = tuning,
                                    notes = notes,
                                    avgBpm = bpm,
                                    consistencyScore = accuracy,
                                    timeSignature = timeSig,
                                    audioPath = null
                                )
                                sessionDao.insertSession(session)
                                importedCount++
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            Result.success(lines.size - 1)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseCSVLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false

        for (char in line) {
            when {
                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    result.add(current.toString().trim())
                    current = StringBuilder()
                }
                else -> current.append(char)
            }
        }
        result.add(current.toString().trim())
        return result
    }

    override fun onCleared() {
        super.onCleared()
        playbackManager.release()
        stopTestMetronome()
        metronome.stop()
        audioEngine.stop()
        timerJob?.cancel()
        sessionStartupJob?.cancel()

        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
        } catch(e: Exception) { }
    }
}