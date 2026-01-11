package com.example.gpt.core.audio

import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.io.TarsosDSPAudioFormat
import be.tarsos.dsp.io.UniversalAudioInputStream
import be.tarsos.dsp.onsets.ComplexOnsetDetector
import java.io.File
import java.io.FileInputStream
import kotlin.math.abs
import kotlin.math.round

data class RhythmHit(
    val timeSeconds: Double,
    val beatNumber: Int,
    val deviationMs: Int,
    val isOnBeat: Boolean
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
        private const val MIN_ONSET_INTERVAL = 0.08
        private const val MIN_FILE_SIZE = 1000L
        private const val MIN_ONSETS_REQUIRED = 4
    }

    fun analyze(audioFile: File, targetBpm: Int = 0, threshold: Float = 0.15f, errorMargin: Float = 0.3f, latencyMs: Int = 0): AnalysisResult {
        if (!audioFile.exists() || audioFile.length() < MIN_FILE_SIZE) {
            return if (targetBpm > 0) AnalysisResult(targetBpm, 0) else AnalysisResult(0, 0)
        }

        val onsets = mutableListOf<Double>()

        try {
            val fileStream = FileInputStream(audioFile)
            val dispatcher = AudioDispatcher(UniversalAudioInputStream(fileStream, run {
                TarsosDSPAudioFormat(SAMPLE_RATE, 16, 1, true, false)
            }), BUFFER_SIZE, HOP_SIZE)

            val onsetDetector = ComplexOnsetDetector(BUFFER_SIZE, threshold.toDouble())

            onsetDetector.setHandler { time, _ ->
                val correctedTime = time - (latencyMs / 1000.0)
                if (correctedTime >= 0) {
                    onsets.add(correctedTime)
                }
            }

            dispatcher.addAudioProcessor(onsetDetector)
            dispatcher.run()
        } catch (e: Exception) {
            e.printStackTrace()
            return AnalysisResult(targetBpm, 0)
        }

        return calculateRhythm(onsets, targetBpm, errorMargin)
    }

    private fun calculateRhythm(onsets: List<Double>, targetBpm: Int, errorMargin: Float): AnalysisResult {
        val filteredOnsets = mutableListOf<Double>()
        if (onsets.isNotEmpty()) {
            filteredOnsets.add(onsets[0])
            for (i in 1 until onsets.size) {
                if (onsets[i] - onsets[i-1] > MIN_ONSET_INTERVAL) {
                    filteredOnsets.add(onsets[i])
                }
            }
        }

        if (filteredOnsets.size < MIN_ONSETS_REQUIRED) {
            return if (targetBpm > 0) AnalysisResult(targetBpm, 0) else AnalysisResult(0, 0)
        }

        val intervals = mutableListOf<Double>()
        for (i in 0 until filteredOnsets.size - 1) {
            intervals.add(filteredOnsets[i+1] - filteredOnsets[i])
        }

        val sessionDuration = if (filteredOnsets.isNotEmpty()) filteredOnsets.last() else 0.0

        if (targetBpm > 0) {
            val beatDuration = 60.0 / targetBpm
            val subdivisions = listOf(1.0, 0.5, 0.333, 0.25)

            val hitMarginSeconds = beatDuration * 0.2

            var totalError = 0.0

            for (interval in intervals) {
                var bestSubdivisionError = Double.MAX_VALUE
                for (sub in subdivisions) {
                    val expectedInterval = beatDuration * sub
                    val error = abs(interval - expectedInterval)
                    val relativeError = error / expectedInterval
                    if (relativeError < bestSubdivisionError) {
                        bestSubdivisionError = relativeError
                    }
                }
                if (bestSubdivisionError < errorMargin) {
                    totalError += bestSubdivisionError
                } else {
                    totalError += 0.5
                }
            }
            val avgError = if (intervals.isNotEmpty()) totalError / intervals.size else 1.0
            val accuracy = ((1.0 - avgError) * 100).toInt().coerceIn(0, 100)

            val hitPositions = mutableListOf<Float>()
            val detailedHits = mutableListOf<RhythmHit>()
            val firstOnset = filteredOnsets[0]
            var hitsOnBeat = 0

            for (onset in filteredOnsets) {
                val timeFromStart = onset - firstOnset
                val beatsPassed = timeFromStart / beatDuration

                val nearestBeat = round(beatsPassed).toInt()
                val deviationBeats = beatsPassed - nearestBeat
                val deviationMs = (deviationBeats * beatDuration * 1000).toInt()

                val isOnBeat = abs(deviationBeats * beatDuration) <= hitMarginSeconds
                if (isOnBeat) hitsOnBeat++

                val normalizedPos = 0.5f + deviationBeats.toFloat()

                if (normalizedPos in 0.0f..1.0f) {
                    hitPositions.add(normalizedPos)
                }

                detailedHits.add(
                    RhythmHit(
                        timeSeconds = onset,
                        beatNumber = nearestBeat,
                        deviationMs = deviationMs,
                        isOnBeat = isOnBeat
                    )
                )
            }

            val totalBeats = if (sessionDuration > 0) (sessionDuration / beatDuration).toInt() else 0

            return AnalysisResult(
                bpm = targetBpm,
                consistency = accuracy,
                hits = hitPositions,
                detailedHits = detailedHits,
                totalBeats = totalBeats,
                hitsOnBeat = hitsOnBeat,
                sessionDurationSeconds = sessionDuration
            )
        } else {
            val averageInterval = intervals.average()
            val bpm = if (averageInterval > 0) (60.0 / averageInterval).toInt() else 0
            var totalDeviation = 0.0
            for (interval in intervals) {
                totalDeviation += abs(interval - averageInterval)
            }
            val consistency = if (averageInterval > 0) {
                ((1.0 - (totalDeviation / intervals.size / averageInterval)) * 100).toInt().coerceIn(0, 100)
            } else 0
            return AnalysisResult(bpm, consistency, sessionDurationSeconds = sessionDuration)
        }
    }
}