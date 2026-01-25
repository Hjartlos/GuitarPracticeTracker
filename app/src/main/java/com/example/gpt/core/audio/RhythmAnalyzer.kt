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
import kotlin.math.roundToInt
import kotlin.math.round

data class RhythmHit(
    val timeSeconds: Double,
    val targetTimeSeconds: Double,
    val beatNumber: Int,
    val deviationMs: Int,
    val isOnBeat: Boolean,
    val noteType: String,
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
        private const val SILENCE_RMS_THRESHOLD = 0.005f
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
            val detectorThreshold = threshold.toDouble().coerceIn(0.005, 0.4)
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
        val averageSalience = rawOnsets.map { it.second }.average()

        val adaptiveThreshold = (averageSalience * 0.5).coerceAtLeast(SILENCE_RMS_THRESHOLD.toDouble())
        val gatedOnsets = rawOnsets.filter { it.second >= adaptiveThreshold }

        val guitarOnsets = if (metronomeClicksSeconds.isNotEmpty()) {
            separateGuitarFromMetronome(gatedOnsets, metronomeClicksSeconds, maxSalience, targetBpm)
        } else {
            gatedOnsets
        }

        val safeBpm = if (targetBpm > 0) targetBpm else 120
        val sixteenthDuration = (60.0 / safeBpm) / 4.0

        val dynamicClusterWindow = (sixteenthDuration * 0.80).coerceIn(0.050, 0.130)

        val cleanOnsets = clusterHits(guitarOnsets, dynamicClusterWindow)

        if (cleanOnsets.isEmpty()) return@withContext AnalysisResult(0, 0)

        val sessionDuration = cleanOnsets.lastOrNull() ?: 0.0

        if (targetBpm <= 0) {
            val freeHits = cleanOnsets.map {
                RhythmHit(it, it, 0, 0, true, "Good")
            }
            return@withContext AnalysisResult(
                0, 0,
                hits = cleanOnsets.map { it.toFloat() },
                detailedHits = freeHits,
                sessionDurationSeconds = sessionDuration
            )
        }

        val detailedHits = mutableListOf<RhythmHit>()
        var hitsOnBeat = 0

        val quarterDuration = 60.0 / targetBpm

        val strictWindowMs = 35.0
        val looseWindowMs = 35.0 + (80.0 * errorMargin)
        val ghostNoteThresholdMs = (quarterDuration / 4.0 * 1000.0) * 0.9

        for (onset in cleanOnsets) {
            val rawBeatIndex = onset / quarterDuration
            val nearestQuarter = round(rawBeatIndex).toInt()
            val positionInBeat = rawBeatIndex - nearestQuarter

            val subdivisions = listOf(-0.75, -0.5, -0.25, 0.0, 0.25, 0.5, 0.75)

            var bestSubdivision = 0.0
            var minAbsDiff = Double.MAX_VALUE

            for (sub in subdivisions) {
                val diff = positionInBeat - sub
                val absDiff = abs(diff)

                if (absDiff < minAbsDiff) {
                    minAbsDiff = absDiff
                    bestSubdivision = sub
                }
            }

            val targetBeatIndex = nearestQuarter + bestSubdivision
            val targetTime = targetBeatIndex * quarterDuration

            val signedDeviationSeconds = onset - targetTime
            val deviationMs = (signedDeviationSeconds * 1000).toInt()
            val absDeviationMs = abs(deviationMs).toDouble()

            var noteType = "Ghost"
            var isGhost = true
            var isPerfect = false

            when {
                absDeviationMs <= strictWindowMs -> {
                    noteType = "Perfect"
                    isGhost = false
                    isPerfect = true
                    hitsOnBeat++
                }
                absDeviationMs <= looseWindowMs -> {
                    noteType = "Good"
                    isGhost = false
                }
                absDeviationMs <= ghostNoteThresholdMs -> {
                    noteType = "Miss"
                    isGhost = false
                }
                else -> {
                    noteType = "Ghost"
                    isGhost = true
                }
            }

            detailedHits.add(
                RhythmHit(
                    timeSeconds = onset,
                    targetTimeSeconds = targetTime,
                    beatNumber = nearestQuarter,
                    deviationMs = deviationMs,
                    isOnBeat = isPerfect,
                    noteType = noteType,
                    isGhostNote = isGhost
                )
            )
        }

        val perfectHitsCount = detailedHits.count { it.noteType == "Perfect" }
        val goodHitsCount = detailedHits.count { it.noteType == "Good" }
        val meaningfulHitsCount = detailedHits.count { !it.isGhostNote }

        val accuracy = if (meaningfulHitsCount > 0) {
            val perfectPct = (perfectHitsCount.toDouble() / meaningfulHitsCount * 100.0).roundToInt()
            val goodPct = (goodHitsCount.toDouble() / meaningfulHitsCount * 100.0).roundToInt()
            perfectPct + goodPct
        } else {
            0
        }

        return@withContext AnalysisResult(
            bpm = targetBpm,
            consistency = accuracy,
            hits = detailedHits.map { (it.timeSeconds / sessionDuration).toFloat() },
            detailedHits = detailedHits,
            totalBeats = (sessionDuration / quarterDuration).toInt(),
            hitsOnBeat = hitsOnBeat,
            sessionDurationSeconds = sessionDuration
        )
    }

    private fun separateGuitarFromMetronome(
        onsets: List<Pair<Double, Double>>,
        metronomeClicks: List<Double>,
        maxSalience: Double,
        targetBpm: Int
    ): List<Pair<Double, Double>> {
        val beatDuration = 60.0 / targetBpm
        val metronomeOnlyAmplitudes = mutableListOf<Double>()
        for (clickTime in metronomeClicks) {
            val nearbyOnsets = onsets.filter { abs(it.first - clickTime) < 0.04 }
            if (nearbyOnsets.size == 1) {
                metronomeOnlyAmplitudes.add(nearbyOnsets[0].second)
            }
        }
        val baselineMetronomeAmplitude = if (metronomeOnlyAmplitudes.size >= 3) {
            metronomeOnlyAmplitudes.sorted()[metronomeOnlyAmplitudes.size / 2]
        } else {
            maxSalience * 0.20
        }
        val guitarOnsets = mutableListOf<Pair<Double, Double>>()
        for (onset in onsets) {
            val nearestClick = metronomeClicks.minByOrNull { abs(it - onset.first) }
            val distanceToClick = nearestClick?.let { abs(onset.first - it) } ?: Double.MAX_VALUE
            when {
                distanceToClick > 0.12 -> guitarOnsets.add(onset)
                distanceToClick < 0.05 -> {
                    val amplitudeRatio = onset.second / baselineMetronomeAmplitude
                    if (amplitudeRatio > 1.10 || onset.second > maxSalience * 0.25) {
                        guitarOnsets.add(onset)
                    }
                }
                else -> {
                    if (onset.second > baselineMetronomeAmplitude * 0.9) {
                        guitarOnsets.add(onset)
                    }
                }
            }
        }
        return guitarOnsets
    }

    private fun clusterHits(
        rawHits: List<Pair<Double, Double>>,
        clusterWindow: Double
    ): List<Double> {
        if (rawHits.isEmpty()) return emptyList()

        val clustered = mutableListOf<Double>()
        var currentCluster = mutableListOf<Pair<Double, Double>>()
        currentCluster.add(rawHits[0])

        for (i in 1 until rawHits.size) {
            val hit = rawHits[i]
            val lastInCluster = currentCluster.last()

            if (hit.first - lastInCluster.first < clusterWindow) {
                currentCluster.add(hit)
            } else {
                clustered.add(getBestHitFromCluster(currentCluster))
                currentCluster = mutableListOf(hit)
            }
        }
        if (currentCluster.isNotEmpty()) {
            clustered.add(getBestHitFromCluster(currentCluster))
        }

        return clustered
    }

    private fun getBestHitFromCluster(cluster: List<Pair<Double, Double>>): Double {
        if (cluster.size == 1) return cluster[0].first
        return cluster.maxByOrNull { it.second }?.first ?: cluster[0].first
    }
}