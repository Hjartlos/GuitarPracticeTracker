package com.example.gpt.core.audio

import android.content.Context
import android.media.MediaPlayer
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class AudioPlaybackManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private var mediaPlayer: MediaPlayer? = null

    private val _activeSessionId = MutableStateFlow<Int?>(null)
    val activeSessionId = _activeSessionId.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress = _progress.asStateFlow()

    private val _speed = MutableStateFlow(1.0f)
    val speed = _speed.asStateFlow()

    private var progressJob: Job? = null

    fun togglePlayback(audioPath: String?, sessionId: Int) {
        if (_activeSessionId.value == sessionId) {
            togglePauseResume()
        } else {
            startNewPlayback(audioPath, sessionId)
        }
    }

    private fun togglePauseResume() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
                _isPlaying.value = false
                stopProgressTracker()
            } else {
                player.start()
                _isPlaying.value = true
                startProgressTracker()
            }
        }
    }

    private fun startNewPlayback(path: String?, sessionId: Int) {
        release()
        if (path == null) return

        val file = File(path)
        if (!file.exists() || !file.canRead()) {
            android.util.Log.e("AudioPlaybackManager", "File issue: $path")
            return
        }

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    try {
                        playbackParams = playbackParams.apply { speed = _speed.value }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                start()
                setOnCompletionListener {
                    _isPlaying.value = false
                    _progress.value = 0f
                    seekTo(0)
                    stopProgressTracker()
                }
            }
            _activeSessionId.value = sessionId
            _isPlaying.value = true
            startProgressTracker()
        } catch (e: Exception) {
            e.printStackTrace()
            release()
        }
    }

    fun seek(progressPercent: Float) {
        mediaPlayer?.let { player ->
            if (player.duration > 0) {
                player.seekTo((player.duration * progressPercent).toInt())
                _progress.value = progressPercent
            }
        }
    }

    fun setSpeed(newSpeed: Float) {
        _speed.value = newSpeed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mediaPlayer?.let { player ->
                try {
                    val wasPlaying = player.isPlaying
                    player.playbackParams = player.playbackParams.apply { speed = newSpeed }
                    if (wasPlaying && !player.isPlaying) {
                        player.start()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.let { player ->
                    if (player.duration > 0) {
                        _progress.value = player.currentPosition.toFloat() / player.duration.toFloat()
                    }
                }
                delay(50)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
    }

    fun release() {
        progressJob?.cancel()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mediaPlayer = null
        _activeSessionId.value = null
        _isPlaying.value = false
        _progress.value = 0f
    }
}