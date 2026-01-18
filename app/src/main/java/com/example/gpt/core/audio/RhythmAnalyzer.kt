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
    val noteType: String = "Quarter",
    val isGhostNote: Boolean = false
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

        private const val CLUSTER_WINDOW = 0.08
        private const val METRONOME_BLIND_WINDOW = 0.08
        private const val SILENCE_RMS_THRESHOLD = 0.001f
    }

    suspend fun analyze(
        audioFile: File,
        targetBpm: Int,
        timeSignatureDenominator: Int = 4,
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

            val detectorThreshold = threshold.toDouble().coerceIn(0.0001, 0.5)

            val onsetDetector = ComplexOnsetDetector(BUFFER_SIZE, detectorThreshold)

            onsetDetector.setHandler { time, salience ->
                var adjustedTime = time - latencySec
                if (adjustedTime < 0) adjustedTime = 0.0

                if (salience > SILENCE_RMS_THRESHOLD) {
                    rawOnsets.add(adjustedTime to salience)
                }
            }

            dispatcher.addAudioProcessor(onsetDetector)
            dispatcher.run()
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext AnalysisResult(0, 0)
        }

        if (rawOnsets.isEmpty()) return@withContext AnalysisResult(0, 0)

        val maxSalience = rawOnsets.maxOfOrNull { it.second } ?: 1.0
        val dynamicGateRatio = if (maxSalience < 0.1) 0.05 else 0.15
        val gateThreshold = maxSalience * dynamicGateRatio

        val gatedOnsets = rawOnsets.filter { it.second >= gateThreshold }

        val nonMetronomeOnsets = if (metronomeClicksSeconds.isNotEmpty()) {
            gatedOnsets.filter { (onsetTime, onsetSalience) ->
                val isInsideMetronomeWindow = metronomeClicksSeconds.any { clickTime ->
                    abs(onsetTime - clickTime) < METRONOME_BLIND_WINDOW
                }
                if (isInsideMetronomeWindow) {
                    onsetSalience > (maxSalience * 0.5)
                } else {
                    true
                }
            }.sortedBy { it.first }
        } else {
            gatedOnsets.sortedBy { it.first }
        }

        val cleanOnsets = clusterHits(nonMetronomeOnsets)

        if (cleanOnsets.isEmpty()) return@withContext AnalysisResult(0, 0)

        val detailedHits = mutableListOf<RhythmHit>()
        var hitsOnBeat = 0
        val sessionDuration = cleanOnsets.lastOrNull() ?: 0.0

        if (targetBpm > 0) {
            val quarterDuration = 60.0 / targetBpm

            val strictWindowMs = 25.0
            val looseWindowMs = 80.0
            val allowedWindowSeconds = (strictWindowMs + (looseWindowMs - strictWindowMs) * errorMargin) / 1000.0

            for (onset in cleanOnsets) {
                val rawBeatIndex = onset / quarterDuration
                val nearestQuarter = round(rawBeatIndex).toInt()
                val positionInBeat = rawBeatIndex % 1.0

                val diffQuarter = abs(onset - (nearestQuarter * quarterDuration))
                val diffEighth = abs(positionInBeat - 0.5) * quarterDuration

                var minDiff = min(diffQuarter, diffEighth)

                if (timeSignatureDenominator >= 8 || targetBpm < 70) {
                    val diffSixteenth = min(abs(positionInBeat - 0.25), abs(positionInBeat - 0.75)) * quarterDuration
                    minDiff = min(minDiff, diffSixteenth)
                }

                if (timeSignatureDenominator >= 16) {
                    val diff32 = min(
                        min(abs(positionInBeat - 0.125), abs(positionInBeat - 0.375)),
                        min(abs(positionInBeat - 0.625), abs(positionInBeat - 0.875))
                    ) * quarterDuration
                    minDiff = min(minDiff, diff32)
                }

                val deviationMs = (minDiff * 1000).toInt()
                var isOnBeat = false

                if (minDiff <= allowedWindowSeconds) {
                    isOnBeat = true
                    hitsOnBeat++
                }

                detailedHits.add(
                    RhythmHit(
                        timeSeconds = onset,
                        beatNumber = nearestQuarter,
                        deviationMs = deviationMs,
                        isOnBeat = isOnBeat,
                        noteType = if(isOnBeat) "Hit" else "Ghost",
                        isGhostNote = !isOnBeat
                    )
                )
            }

            val totalDetected = detailedHits.size
            val accuracy = if (totalDetected > 0) {
                ((hitsOnBeat.toDouble() / totalDetected.toDouble()) * 100.0).toInt()
            } else 0

            return@withContext AnalysisResult(
                bpm = targetBpm,
                consistency = accuracy,
                hits = detailedHits.map { (it.timeSeconds / sessionDuration).toFloat() },
                detailedHits = detailedHits,
                totalBeats = (sessionDuration / quarterDuration).toInt(),
                hitsOnBeat = hitsOnBeat,
                sessionDurationSeconds = sessionDuration
            )
        } else {
            return@withContext AnalysisResult(0, 0, sessionDurationSeconds = sessionDuration)
        }
    }

    private fun clusterHits(rawHits: List<Pair<Double, Double>>): List<Double> {
        if (rawHits.isEmpty()) return emptyList()

        val clustered = mutableListOf<Double>()
        var currentCluster = mutableListOf<Pair<Double, Double>>()

        currentCluster.add(rawHits[0])

        for (i in 1 until rawHits.size) {
            val hit = rawHits[i]
            val lastInCluster = currentCluster.last()

            if (hit.first - lastInCluster.first < CLUSTER_WINDOW) {
                currentCluster.add(hit)
            } else {
                val bestHit = currentCluster.maxByOrNull { it.second }!!
                clustered.add(bestHit.first)

                currentCluster.clear()
                currentCluster.add(hit)
            }
        }

        if (currentCluster.isNotEmpty()) {
            val bestHit = currentCluster.maxByOrNull { it.second }!!
            clustered.add(bestHit.first)
        }

        return clustered
    }
}