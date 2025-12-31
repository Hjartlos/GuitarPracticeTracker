package com.example.gpt.audio

import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.io.TarsosDSPAudioFormat
import be.tarsos.dsp.io.UniversalAudioInputStream
import be.tarsos.dsp.onsets.ComplexOnsetDetector
import be.tarsos.dsp.onsets.OnsetHandler
import java.io.File
import java.io.FileInputStream
import kotlin.math.abs

data class AnalysisResult(
    val bpm: Int,
    val consistency: Int // 0-100%
)

class RhythmAnalyzer {

    fun analyze(audioFile: File): AnalysisResult {
        if (!audioFile.exists() || audioFile.length() < 1000) {
            return AnalysisResult(0, 0)
        }

        val onsets = mutableListOf<Double>() // Czas wystąpienia uderzeń w sekundach

        // 1. Konfiguracja detektora Onsetów (ComplexOnsetDetector jest dobry do muzyki)
        // 512/256 to mniejszy bufor dla lepszej precyzji czasowej
        val dispatcher = AudioDispatcher(UniversalAudioInputStream(FileInputStream(audioFile), run {
            TarsosDSPAudioFormat(44100f, 16, 1, true, false)
        }), 512, 256)

        val onsetHandler = OnsetHandler { time, _ ->
            onsets.add(time)
        }

        val onsetDetector = ComplexOnsetDetector(512, 0.3) // 0.3 to próg czułości (eksperymentalnie)
        onsetDetector.setHandler(onsetHandler)
        dispatcher.addAudioProcessor(onsetDetector)

        dispatcher.run() // To uruchomi się w bieżącym wątku i zablokuje go do końca pliku (szybko)

        // 2. Obliczenia BPM
        return calculateRhythm(onsets)
    }

    private fun calculateRhythm(onsets: List<Double>): AnalysisResult {
        if (onsets.size < 4) return AnalysisResult(0, 0) // Za mało uderzeń

        // Obliczamy odstępy między uderzeniami (Inter-Onset Intervals)
        val intervals = mutableListOf<Double>()
        for (i in 0 until onsets.size - 1) {
            val diff = onsets[i+1] - onsets[i]
            if (diff > 0.1) { // Ignorujemy bardzo szybkie "duszki" (szumy poniżej 100ms)
                intervals.add(diff)
            }
        }

        if (intervals.isEmpty()) return AnalysisResult(0, 0)

        // Średni odstęp czasu
        val averageInterval = intervals.average()

        // BPM = 60 sekund / średni odstęp
        val bpm = (60.0 / averageInterval).toInt()

        // Obliczamy spójność (Consistency) na podstawie wariancji
        // Im mniejsze odchylenia od średniej, tym równiejsza gra
        var totalDeviation = 0.0
        for (interval in intervals) {
            totalDeviation += abs(interval - averageInterval)
        }
        val avgDeviation = totalDeviation / intervals.size

        // Heurystyka: Jeśli odchylenie to np. 0.05s, to gra jest dość równa.
        // Jeśli odchylenie to > 0.2s, to gra jest nierówna.
        // Mapujemy to na 0-100%
        val consistency = ((1.0 - (avgDeviation / averageInterval)) * 100).toInt().coerceIn(0, 100)

        return AnalysisResult(bpm, consistency)
    }
}