package com.example.gpt.core.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin

object ToneGenerator {

    private const val SAMPLE_RATE = 44100

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private var currentAudioTrack: AudioTrack? = null

    private val GUITAR_HARMONICS = listOf(
        1.0 to 1.0,
        2.0 to 0.5,
        3.0 to 0.33,
        4.0 to 0.25,
        5.0 to 0.2,
        6.0 to 0.16,
    )

    fun stopCurrentTone() {
        try {
            currentAudioTrack?.stop()
            currentAudioTrack?.release()
        } catch (e: Exception) {
        }
        currentAudioTrack = null
        _isPlaying.value = false
    }

    fun playGuitarTone(
        frequency: Float,
        durationMs: Int = 1200,
        onPlaybackStarted: (() -> Unit)? = null,
        onPlaybackFinished: (() -> Unit)? = null
    ) {
        if (_isPlaying.value) {
            stopCurrentTone()
        }

        CoroutineScope(Dispatchers.Default).launch {
            _isPlaying.value = true
            onPlaybackStarted?.invoke()

            try {
                val numSamples = SAMPLE_RATE * durationMs / 1000
                val samples = ShortArray(numSamples)

                val attackSamples = (SAMPLE_RATE * 0.005).toInt()
                val decaySamples = (SAMPLE_RATE * 0.1).toInt()
                val sustainLevel = 0.7
                val releaseStart = numSamples - (SAMPLE_RATE * 0.3).toInt()

                for (i in 0 until numSamples) {
                    val t = i.toDouble() / SAMPLE_RATE

                    var signal = 0.0
                    for ((harmonic, amplitude) in GUITAR_HARMONICS) {
                        val harmonicFreq = frequency * harmonic
                        val harmonicDecay = exp(-harmonic * 0.5 * t)
                        signal += amplitude * harmonicDecay * sin(2.0 * PI * harmonicFreq * t)
                    }

                    if (i < attackSamples * 3) {
                        val pluckNoise = (Math.random() - 0.5) * 0.15 * exp(-10.0 * t)
                        signal += pluckNoise
                    }

                    val envelope = when {
                        i < attackSamples -> i.toDouble() / attackSamples
                        i < attackSamples + decaySamples -> {
                            val decayProgress = (i - attackSamples).toDouble() / decaySamples
                            1.0 - (1.0 - sustainLevel) * decayProgress
                        }
                        i > releaseStart -> {
                            val releaseProgress = (i - releaseStart).toDouble() / (numSamples - releaseStart)
                            sustainLevel * (1.0 - releaseProgress).pow(2)
                        }
                        else -> sustainLevel * exp(-1.5 * (i - attackSamples - decaySamples).toDouble() / numSamples)
                    }

                    val normalizedSignal = signal / 2.5
                    val value = (normalizedSignal * envelope * Short.MAX_VALUE * 0.8).toInt()
                    samples[i] = value.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }

                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()

                val audioFormat = AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(audioAttributes)
                    .setAudioFormat(audioFormat)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .setBufferSizeInBytes(numSamples * 2)
                    .build()

                currentAudioTrack = audioTrack

                audioTrack.write(samples, 0, numSamples)
                audioTrack.play()

                var elapsed = 0
                val sleepChunk = 50
                while (elapsed < durationMs && _isPlaying.value) {
                    Thread.sleep(sleepChunk.toLong())
                    elapsed += sleepChunk
                }

                audioTrack.stop()
                audioTrack.release()
                currentAudioTrack = null

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isPlaying.value = false
                onPlaybackFinished?.invoke()
            }
        }
    }

    fun playCalibrationTone(frequency: Float = 1000f, durationMs: Int = 80) {
        CoroutineScope(Dispatchers.Default).launch {
            val numSamples = SAMPLE_RATE * durationMs / 1000
            val samples = ShortArray(numSamples)

            for (i in 0 until numSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                val envelope = if (i < numSamples / 10) i.toDouble() / (numSamples / 10)
                              else if (i > numSamples * 9 / 10) (numSamples - i).toDouble() / (numSamples / 10)
                              else 1.0
                val signal = sin(2.0 * PI * frequency * t) * envelope
                samples[i] = (signal * Short.MAX_VALUE * 0.9).toInt().toShort()
            }

            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(SAMPLE_RATE)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(numSamples * 2)
                .build()

            try {
                audioTrack.write(samples, 0, numSamples)
                audioTrack.play()
                Thread.sleep(durationMs.toLong() + 50)
                audioTrack.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun playTone(frequency: Float, durationMs: Int = 2000) {
        playGuitarTone(frequency, durationMs)
    }
}