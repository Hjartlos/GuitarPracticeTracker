package com.example.gpt.core.audio

import kotlin.math.log2
import kotlin.math.roundToInt

data class TunerResult(
    val note: String = "--",
    val octave: Int? = null,
    val frequency: Float = 0f,
    val cents: Int = 0,
    val isLocked: Boolean = false
)

object AudioUtils {
    private val noteNames = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

    @Volatile
    var referenceFrequency: Float = 440f

    fun processPitch(pitchInHz: Float): TunerResult {
        if (pitchInHz == -1f || pitchInHz < 20f) return TunerResult()

        val n = 69 + 12 * log2(pitchInHz / referenceFrequency.toDouble())
        val midiNote = n.roundToInt()

        val cents = ((n - midiNote) * 100).toInt()

        val octave = (midiNote / 12) - 1
        val noteIndex = midiNote % 12
        val noteName = noteNames[if (noteIndex < 0) noteIndex + 12 else noteIndex]

        return TunerResult(
            note = noteName,
            octave = octave,
            frequency = pitchInHz,
            cents = cents,
            isLocked = true
        )
    }
}