package com.example.gpt.audio

import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.io.TarsosDSPAudioFormat
import be.tarsos.dsp.io.UniversalAudioInputStream
import be.tarsos.dsp.onsets.ComplexOnsetDetector
import java.io.File
import java.io.FileInputStream
import kotlin.math.abs

data class AnalysisResult(
    val bpm: Int,
    val consistency: Int
)

class RhythmAnalyzer {
    fun analyze(audioFile: File, targetBpm: Int = 0, threshold: Float = 0.15f, errorMargin: Float = 0.3f, latencyMs: Int = 0): AnalysisResult {
        if (!audioFile.exists() || audioFile.length() < 1000) {
            return if (targetBpm > 0) AnalysisResult(targetBpm, 0) else AnalysisResult(0, 0)
        }

        val onsets = mutableListOf<Double>()
        val dispatcher = AudioDispatcher(UniversalAudioInputStream(FileInputStream(audioFile), run {
            TarsosDSPAudioFormat(44100f, 16, 1, true, false)
        }), 1024, 512)

        val onsetDetector = ComplexOnsetDetector(1024, threshold.toDouble())

        onsetDetector.setHandler { time, _ ->
            val correctedTime = time - (latencyMs / 1000.0)
            if (correctedTime >= 0) {
                onsets.add(correctedTime)
            }
        }

        dispatcher.addAudioProcessor(onsetDetector)
        dispatcher.run()

        return calculateRhythm(onsets, targetBpm, errorMargin)
    }

    private fun calculateRhythm(onsets: List<Double>, targetBpm: Int, errorMargin: Float): AnalysisResult {
        val filteredOnsets = mutableListOf<Double>()
        if (onsets.isNotEmpty()) {
            filteredOnsets.add(onsets[0])
            for (i in 1 until onsets.size) {
                if (onsets[i] - onsets[i-1] > 0.08) {
                    filteredOnsets.add(onsets[i])
                }
            }
        }

        if (filteredOnsets.size < 4) {
            return if (targetBpm > 0) AnalysisResult(targetBpm, 0) else AnalysisResult(0, 0)
        }

        val intervals = mutableListOf<Double>()
        for (i in 0 until filteredOnsets.size - 1) {
            intervals.add(filteredOnsets[i+1] - filteredOnsets[i])
        }

        if (targetBpm > 0) {
            val beatDuration = 60.0 / targetBpm
            val subdivisions = listOf(1.0, 0.5, 0.333, 0.25)

            var totalError = 0.0
            var matchedNotes = 0

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
                    matchedNotes++
                } else {
                    totalError += 0.5
                }
            }

            val avgError = if (intervals.isNotEmpty()) totalError / intervals.size else 1.0
            val accuracy = ((1.0 - avgError) * 100).toInt().coerceIn(0, 100)

            return AnalysisResult(targetBpm, accuracy)
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
            return AnalysisResult(bpm, consistency)
        }
    }
}