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
        private const val MIN_BPM = 20
        private const val MAX_BPM = 300
        private const val ACCENT_FREQUENCY = 1200.0
        private const val NORMAL_FREQUENCY = 800.0
        private const val TONE_DURATION_MS = 40
    }

    private var bpm = 120

    private var beatPattern: List<BeatType> = listOf(BeatType.ACCENT, BeatType.NORMAL, BeatType.NORMAL, BeatType.NORMAL)

    @Volatile
    private var isPlaying = false
    private var audioTrack: AudioTrack? = null
    private var metronomeJob: Job? = null
    private val metronomeScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    var onBeatTick: ((Int, Boolean) -> Unit)? = null

    private val strongBeat by lazy { generateTone(ACCENT_FREQUENCY, TONE_DURATION_MS) }
    private val weakBeat by lazy { generateTone(NORMAL_FREQUENCY, TONE_DURATION_MS) }

    private val silenceBuffer = ShortArray(SAMPLE_RATE / 2)

    fun setBpm(newBpm: Int) {
        bpm = newBpm.coerceIn(MIN_BPM, MAX_BPM)
    }

    fun setTimeSignature(numerator: Int) {
        val newPattern = mutableListOf<BeatType>()
        if (numerator > 0) {
            newPattern.add(BeatType.ACCENT)
            for (i in 1 until numerator) {
                newPattern.add(BeatType.NORMAL)
            }
        }
        beatPattern = newPattern
    }

    fun setPattern(pattern: List<BeatType>, denominator: Int) {
        this.beatPattern = pattern
    }

    fun start() {
        if (isPlaying) return
        isPlaying = true

        val minBufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        audioTrack = AudioTrack.Builder()
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
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(minBufferSize)
            .build()

        audioTrack?.play()

        metronomeJob = metronomeScope.launch(Dispatchers.Default) {
            var currentBeatIndex = 0

            while (isPlaying) {
                val beatType = beatPattern.getOrElse(currentBeatIndex) { BeatType.NORMAL }
                val isAccent = beatType == BeatType.ACCENT

                onBeatTick?.invoke(currentBeatIndex, isAccent)

                val sound = when (beatType) {
                    BeatType.ACCENT -> strongBeat
                    BeatType.NORMAL -> weakBeat
                    BeatType.MUTE -> null
                }
                val samplesPerBeat = (60.0 / bpm * SAMPLE_RATE).toInt()

                if (sound != null) {
                    writeBufferBlocking(sound)

                    var silenceSamplesNeeded = samplesPerBeat - sound.size
                    if (silenceSamplesNeeded < 0) silenceSamplesNeeded = 0
                    writeSilenceBlocking(silenceSamplesNeeded)
                } else {
                    writeSilenceBlocking(samplesPerBeat)
                }

                currentBeatIndex = (currentBeatIndex + 1) % beatPattern.size
            }
        }
    }

    private fun writeBufferBlocking(buffer: ShortArray) {
        if (!isPlaying || audioTrack == null) return
        try {
            audioTrack?.write(buffer, 0, buffer.size)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun writeSilenceBlocking(samples: Int) {
        var remaining = samples
        Arrays.fill(silenceBuffer, 0.toShort())

        while (remaining > 0 && isPlaying) {
            val chunk = minOf(remaining, silenceBuffer.size)
            try {
                audioTrack?.write(silenceBuffer, 0, chunk)
            } catch (e: Exception) {
                break
            }
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
        for (i in 0 until numSamples) {
            val envelope = 1.0 - (i.toDouble() / numSamples)
            val signal = kotlin.math.sin(2.0 * Math.PI * freqHz * i / SAMPLE_RATE) * envelope
            sample[i] = (signal * Short.MAX_VALUE * 0.8).toInt().toShort()
        }
        return sample
    }
}