package com.example.gpt.core.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.*
import java.util.Arrays

enum class BeatType {
    ACCENT,
    NORMAL,
    MUTE
}

class Metronome {

    companion object {
        private const val SAMPLE_RATE = 44100
        private const val MIN_BPM = 30
        private const val MAX_BPM = 300
        private const val ACCENT_FREQUENCY = 1200.0
        private const val NORMAL_FREQUENCY = 800.0
        private const val TONE_DURATION_MS = 50
    }

    private var bpm = 120
    private var noteValue = 4

    private var beatPattern: List<BeatType> = listOf(BeatType.ACCENT, BeatType.NORMAL, BeatType.NORMAL, BeatType.NORMAL)

    @Volatile
    private var isPlaying = false
    private var audioTrack: AudioTrack? = null
    private var metronomeJob: Job? = null
    private val metronomeScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    var onBeatTick: ((Int) -> Unit)? = null

    private val strongBeat by lazy { generateTone(ACCENT_FREQUENCY, TONE_DURATION_MS) }
    private val weakBeat by lazy { generateTone(NORMAL_FREQUENCY, TONE_DURATION_MS) }

    private val silenceBuffer = ShortArray(SAMPLE_RATE)

    fun setBpm(newBpm: Int) {
        bpm = newBpm.coerceIn(MIN_BPM, MAX_BPM)
    }

    fun setPattern(pattern: List<BeatType>, value: Int) {
        beatPattern = pattern
        noteValue = value
    }

    fun start() {
        if (isPlaying) return
        isPlaying = true

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        val audioFormat = AudioFormat.Builder()
            .setSampleRate(SAMPLE_RATE)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()

        val minBufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val bufferSize = maxOf(minBufferSize, SAMPLE_RATE)

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(audioAttributes)
            .setAudioFormat(audioFormat)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(bufferSize)
            .build()

        audioTrack?.play()

        metronomeJob = metronomeScope.launch {
            var globalBeatCounter = 0
            val startTime = System.nanoTime()

            while (isActive && isPlaying) {
                val currentStepIndex = globalBeatCounter % beatPattern.size

                launch(Dispatchers.Main.immediate) {
                    onBeatTick?.invoke(currentStepIndex + 1)
                }

                val beatMultiplier = 4.0 / noteValue
                val beatDurationNanos = (60_000_000_000L / bpm * beatMultiplier).toLong()

                globalBeatCounter++
                val nextBeatTime = startTime + (globalBeatCounter * beatDurationNanos)

                val now = System.nanoTime()
                val waitNanos = nextBeatTime - now
                if (waitNanos > 0) {
                    delay(waitNanos / 1_000_000)
                }
            }
        }

        Thread {
            var audioGlobalCounter = 0
            while (isPlaying) {
                val currentStepIndex = audioGlobalCounter % beatPattern.size
                val currentBeatType = beatPattern[currentStepIndex]

                val sound = when (currentBeatType) {
                    BeatType.ACCENT -> strongBeat
                    BeatType.NORMAL -> weakBeat
                    BeatType.MUTE -> null
                }

                val beatMultiplier = 4.0 / noteValue
                val samplesPerBeat = ((SAMPLE_RATE * 60) / bpm * beatMultiplier).toInt()

                if (sound != null) {
                    audioTrack?.write(sound, 0, sound.size)
                    var silenceSamplesNeeded = samplesPerBeat - sound.size
                    if (silenceSamplesNeeded < 0) silenceSamplesNeeded = 0
                    writeSilenceBlocking(silenceSamplesNeeded)
                } else {
                    writeSilenceBlocking(samplesPerBeat)
                }

                audioGlobalCounter++
            }
        }.start()
    }

    private fun writeSilenceBlocking(samples: Int) {
        var remaining = samples
        Arrays.fill(silenceBuffer, 0.toShort())

        while (remaining > 0 && isPlaying) {
            val chunk = minOf(remaining, silenceBuffer.size)
            audioTrack?.write(silenceBuffer, 0, chunk)
            remaining -= chunk
        }
    }

    fun stop() {
        isPlaying = false
        metronomeJob?.cancel()
        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.release()
        } catch (e: Exception) { e.printStackTrace() }
        audioTrack = null
    }

    private fun generateTone(freqHz: Double, durationMs: Int): ShortArray {
        val numSamples = (SAMPLE_RATE * durationMs / 1000)
        val sample = ShortArray(numSamples)
        val phaseStep = 2.0 * Math.PI * freqHz / SAMPLE_RATE
        for (i in 0 until numSamples) {
            val envelope = 1.0 - (i.toDouble() / numSamples)
            sample[i] = (Math.sin(i * phaseStep) * Short.MAX_VALUE * envelope).toInt().toShort()
        }
        return sample
    }
}