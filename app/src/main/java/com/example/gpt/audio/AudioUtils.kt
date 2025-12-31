package com.example.gpt.audio

import kotlin.math.log2
import kotlin.math.roundToInt

data class TunerResult(
    val note: String = "--",
    val frequency: Float = 0f,
    val cents: Int = 0, // Odchylenie w centach (-50 do +50)
    val isLocked: Boolean = false // Czy dźwięk jest stabilny
)

object AudioUtils {
    private val noteNames = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

    fun processPitch(pitchInHz: Float): TunerResult {
        if (pitchInHz == -1f || pitchInHz < 20f) return TunerResult() // Cisza lub błąd

        // Wzór na numer nuty MIDI: n = 69 + 12 * log2(f / 440)
        val n = 69 + 12 * log2(pitchInHz / 440.0)
        val midiNote = n.roundToInt()

        // Obliczanie odchylenia (cents)
        val cents = ((n - midiNote) * 100).toInt()

        // Zamiana numeru MIDI na nazwę nuty
        val noteIndex = midiNote % 12
        val noteName = noteNames[if (noteIndex < 0) noteIndex + 12 else noteIndex]

        return TunerResult(
            note = noteName,
            frequency = pitchInHz,
            cents = cents,
            isLocked = true
        )
    }
}