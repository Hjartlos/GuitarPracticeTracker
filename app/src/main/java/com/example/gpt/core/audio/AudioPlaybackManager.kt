package com.example.gpt.core.audio

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
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
        if (!file.exists()) return

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, Uri.fromFile(file))
                prepare()
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
            player.seekTo((player.duration * progressPercent).toInt())
            _progress.value = progressPercent
        }
    }

    fun setSpeed(newSpeed: Float) {
        _speed.value = newSpeed
        mediaPlayer?.let { player ->
            try {
                val wasPlaying = player.isPlaying
                player.playbackParams = player.playbackParams.apply { speed = newSpeed }
                if (wasPlaying && !player.isPlaying) {
                    player.start()
                }
            } catch (e: Exception) {
            }
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.let { player ->
                    _progress.value = player.currentPosition.toFloat() / player.duration.toFloat()
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
        mediaPlayer?.release()
        mediaPlayer = null
        _activeSessionId.value = null
        _isPlaying.value = false
        _progress.value = 0f
    }
}

