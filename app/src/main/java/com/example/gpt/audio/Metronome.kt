package com.example.gpt.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.*
import java.util.Arrays

class Metronome {
    private var bpm = 120
    private var timeSignature = 4
    private var isPlaying = false
    private var audioTrack: AudioTrack? = null
    private var metronomeJob: Job? = null

    private val strongBeat by lazy { generateTone(1200.0, 50) }
    private val weakBeat by lazy { generateTone(800.0, 50) }

    private val silenceBuffer = ShortArray(44100)

    fun setBpm(newBpm: Int) {
        bpm = newBpm.coerceIn(30, 300)
    }

    fun setTimeSignature(ts: Int) {
        timeSignature = ts
    }

    fun start() {
        if (isPlaying) return
        isPlaying = true

        val sampleRate = 44100
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        val audioFormat = AudioFormat.Builder()
            .setSampleRate(sampleRate)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()

        val minBufferSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val bufferSize = maxOf(minBufferSize, sampleRate)

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(audioAttributes)
            .setAudioFormat(audioFormat)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(bufferSize)
            .build()

        audioTrack?.play()

        metronomeJob = CoroutineScope(Dispatchers.Default).launch {
            var beatCounter = 0

            while (isActive && isPlaying) {
                val sound = if (beatCounter % timeSignature == 0) strongBeat else weakBeat

                val samplesPerBeat = (sampleRate * 60) / bpm

                var silenceSamplesNeeded = samplesPerBeat - sound.size
                if (silenceSamplesNeeded < 0) silenceSamplesNeeded = 0

                audioTrack?.write(sound, 0, sound.size)

                writeSilence(silenceSamplesNeeded)

                beatCounter++
            }
        }
    }

    private fun writeSilence(samples: Int) {
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

    private fun generateTone(freqHz: Double, durationMs: Int, sampleRate: Int = 44100): ShortArray {
        val numSamples = (sampleRate * durationMs / 1000)
        val sample = ShortArray(numSamples)
        val phaseStep = 2.0 * Math.PI * freqHz / sampleRate
        for (i in 0 until numSamples) {
            val envelope = 1.0 - (i.toDouble() / numSamples)
            sample[i] = (Math.sin(i * phaseStep) * Short.MAX_VALUE * envelope).toInt().toShort()
        }
        return sample
    }
}