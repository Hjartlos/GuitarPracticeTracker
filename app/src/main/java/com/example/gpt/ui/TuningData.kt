package com.example.gpt.ui

import kotlin.math.pow

data class GuitarString(
    val id: Int,
    var name: String,
    var octave: Int,
    var frequency: Float
) {
    fun fullName(): String = "$name$octave"
}

object TuningData {
    val notes = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

    fun generateStandardTuning(stringCount: Int): List<GuitarString> {
        return when (stringCount) {
            7 -> listOf(
                GuitarString(7, "B", 1, calculateFreq("B", 1)),
                GuitarString(6, "E", 2, calculateFreq("E", 2)),
                GuitarString(5, "A", 2, calculateFreq("A", 2)),
                GuitarString(4, "D", 3, calculateFreq("D", 3)),
                GuitarString(3, "G", 3, calculateFreq("G", 3)),
                GuitarString(2, "B", 3, calculateFreq("B", 3)),
                GuitarString(1, "E", 4, calculateFreq("E", 4))
            )
            8 -> listOf(
                GuitarString(8, "F#", 1, calculateFreq("F#", 1)),
                GuitarString(7, "B", 1, calculateFreq("B", 1)),
                GuitarString(6, "E", 2, calculateFreq("E", 2)),
                GuitarString(5, "A", 2, calculateFreq("A", 2)),
                GuitarString(4, "D", 3, calculateFreq("D", 3)),
                GuitarString(3, "G", 3, calculateFreq("G", 3)),
                GuitarString(2, "B", 3, calculateFreq("B", 3)),
                GuitarString(1, "E", 4, calculateFreq("E", 4))
            )
            else -> listOf(
                GuitarString(6, "E", 2, calculateFreq("E", 2)),
                GuitarString(5, "A", 2, calculateFreq("A", 2)),
                GuitarString(4, "D", 3, calculateFreq("D", 3)),
                GuitarString(3, "G", 3, calculateFreq("G", 3)),
                GuitarString(2, "B", 3, calculateFreq("B", 3)),
                GuitarString(1, "E", 4, calculateFreq("E", 4))
            )
        }
    }

    fun calculateFreq(note: String, octave: Int): Float {
        val noteIndex = notes.indexOf(note)
        if (noteIndex == -1) return 0f


        val midiNote = (octave + 1) * 12 + noteIndex

        val semitonesFromA4 = midiNote - 69

        return (440.0 * 2.0.pow(semitonesFromA4 / 12.0)).toFloat()
    }
}