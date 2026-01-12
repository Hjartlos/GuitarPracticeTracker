package com.example.gpt.core.audio

import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.io.TarsosDSPAudioFormat
import be.tarsos.dsp.io.UniversalAudioInputStream
import be.tarsos.dsp.onsets.ComplexOnsetDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.round

data class RhythmHit(
    val timeSeconds: Double,
    val beatNumber: Int,
    val deviationMs: Int,
    val isOnBeat: Boolean,
    val noteType: String = "Quarter"
)

data class AnalysisResult(
    val bpm: Int,
    val consistency: Int,
    val hits: List<Float> = emptyList(),
    val detailedHits: List<RhythmHit> = emptyList(),
    val totalBeats: Int = 0,
    val hitsOnBeat: Int = 0,
    val sessionDurationSeconds: Double = 0.0
)

class RhythmAnalyzer {

    companion object {
        private const val SAMPLE_RATE = 44100f
        private const val BUFFER_SIZE = 1024
        private const val HOP_SIZE = 512
        private const val METRONOME_BLIND_WINDOW = 0.05
    }

    suspend fun analyze(
        audioFile: File,
        targetBpm: Int,
        threshold: Float,
        errorMargin: Float,
        latencyMs: Int,
        metronomeClicksSeconds: List<Double> = emptyList()
    ): AnalysisResult = withContext(Dispatchers.Default) {
        if (!audioFile.exists() || audioFile.length() < 1000) return@withContext AnalysisResult(0, 0)

        val rawOnsets = mutableListOf<Pair<Double, Double>>()
        val latencySec = latencyMs / 1000.0

        try {
            val format = TarsosDSPAudioFormat(SAMPLE_RATE, 16, 1, true, false)
            val stream = UniversalAudioInputStream(FileInputStream(audioFile), format)
            val dispatcher = AudioDispatcher(stream, BUFFER_SIZE, HOP_SIZE)

            val onsetDetector = ComplexOnsetDetector(BUFFER_SIZE, threshold.toDouble())

            onsetDetector.setHandler { time, salience ->
                val adjustedTime = time - latencySec
                if (adjustedTime > 0) {
                    rawOnsets.add(adjustedTime to salience)
                }
            }

            dispatcher.addAudioProcessor(onsetDetector)
            dispatcher.run()
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext AnalysisResult(0, 0)
        }

        val averageSalience = if (rawOnsets.isNotEmpty()) {
            rawOnsets.map { it.second }.average()
        } else 0.0

        val filteredOnsets = if (metronomeClicksSeconds.isNotEmpty()) {
            rawOnsets.filter { (onsetTime, onsetSalience) ->
                val isInsideMetronomeWindow = metronomeClicksSeconds.any { clickTime ->
                    abs(onsetTime - clickTime) < METRONOME_BLIND_WINDOW
                }

                if (isInsideMetronomeWindow) {
                    onsetSalience > (averageSalience * 2.0)
                } else {
                    true
                }
            }.map { it.first }
        } else {
            rawOnsets.map { it.first }
        }

        if (filteredOnsets.isEmpty()) return@withContext AnalysisResult(0, 0)

        val detailedHits = mutableListOf<RhythmHit>()
        var hitsOnBeat = 0
        var totalDeviationMs = 0L
        val sessionDuration = filteredOnsets.lastOrNull() ?: 0.0

        if (targetBpm > 0) {
            val quarterDuration = 60.0 / targetBpm

            val strictWindowMs = 25.0
            val looseWindowMs = 150.0
            val allowedWindowSeconds = (strictWindowMs + (looseWindowMs - strictWindowMs) * errorMargin) / 1000.0

            for (onset in filteredOnsets) {
                val rawBeatIndex = onset / quarterDuration
                val nearestQuarter = round(rawBeatIndex).toInt()
                val positionInBeat = rawBeatIndex % 1.0

                val diffQuarter = abs(onset - (nearestQuarter * quarterDuration))
                val diffEighth = abs(positionInBeat - 0.5) * quarterDuration

                val diffSixteenthA = abs(positionInBeat - 0.25) * quarterDuration
                val diffSixteenthB = abs(positionInBeat - 0.75) * quarterDuration
                val diffSixteenth = min(diffSixteenthA, diffSixteenthB)

                val minDiff = min(diffQuarter, min(diffEighth, diffSixteenth))

                var deviationSeconds = diffQuarter
                var detectedNoteType = "Miss"
                var isOnBeat = false

                if (minDiff <= allowedWindowSeconds) {
                    isOnBeat = true
                    when (minDiff) {
                        diffQuarter -> {
                            deviationSeconds = diffQuarter
                            detectedNoteType = "Quarter"
                        }
                        diffEighth -> {
                            deviationSeconds = diffEighth
                            detectedNoteType = "Eighth"
                        }
                        else -> {
                            deviationSeconds = diffSixteenth
                            detectedNoteType = "Sixteenth"
                        }
                    }
                    hitsOnBeat++
                    totalDeviationMs += (minDiff * 1000).toLong()
                } else {
                    detectedNoteType = "Miss"
                    isOnBeat = false
                    deviationSeconds = minDiff
                    totalDeviationMs += (allowedWindowSeconds * 1000).toLong() * 2
                }

                detailedHits.add(
                    RhythmHit(
                        timeSeconds = onset,
                        beatNumber = nearestQuarter,
                        deviationMs = (deviationSeconds * 1000).toInt(),
                        isOnBeat = isOnBeat,
                        noteType = detectedNoteType
                    )
                )
            }

            val totalBeats = if (sessionDuration > 0) (sessionDuration / quarterDuration).toInt() else 0
            val avgDeviation = if (detailedHits.isNotEmpty()) {
                totalDeviationMs.toDouble() / detailedHits.size
            } else 0.0

            val maxPenalty = allowedWindowSeconds * 1000.0 * 2.0
            val scoreRaw = 100.0 * (1.0 - (avgDeviation / maxPenalty))
            val accuracy = scoreRaw.coerceIn(0.0, 100.0).toInt()

            return@withContext AnalysisResult(
                bpm = targetBpm,
                consistency = accuracy,
                hits = detailedHits.map { (it.timeSeconds / sessionDuration).toFloat() },
                detailedHits = detailedHits,
                totalBeats = totalBeats,
                hitsOnBeat = hitsOnBeat,
                sessionDurationSeconds = sessionDuration
            )
        } else {
            return@withContext AnalysisResult(0, 0, sessionDurationSeconds = sessionDuration)
        }
    }
}