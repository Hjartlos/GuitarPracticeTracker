package com.example.gpt.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

object ToneGenerator {
    fun playTone(frequency: Float, durationMs: Int = 2000) {
        CoroutineScope(Dispatchers.Default).launch {
            val sampleRate = 44100
            val numSamples = sampleRate * durationMs / 1000
            val samples = ShortArray(numSamples)

            val fadeOutStart = numSamples * 0.1

            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate

                val signal = (
                        1.0 * sin(2.0 * PI * frequency * t) +
                                0.5 * sin(2.0 * PI * (frequency * 2) * t) +
                                0.25 * sin(2.0 * PI * (frequency * 3) * t)
                        )

                val envelope = exp(-3.0 * i / numSamples)

                val value = (signal / 1.75 * envelope * Short.MAX_VALUE).toInt()
                samples[i] = value.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            val audioFormat = AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()

            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(audioAttributes)
                .setAudioFormat(audioFormat)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(numSamples * 2)
                .build()

            try {
                audioTrack.write(samples, 0, numSamples)
                audioTrack.play()
                Thread.sleep(durationMs.toLong())
                audioTrack.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}