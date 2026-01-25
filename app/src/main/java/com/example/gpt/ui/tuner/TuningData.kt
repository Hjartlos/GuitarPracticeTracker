package com.example.gpt.ui.tuner

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

    const val MIN_FREQUENCY = 25f
    const val MAX_FREQUENCY = 1000f

    val validOctaves = listOf(0, 1, 2, 3, 4, 5, 6)

    fun getValidNotesForOctave(octave: Int, baseFreq: Float = 440f): List<String> {
        return notes.filter { note ->
            val freq = calculateFreq(note, octave, baseFreq)
            freq in MIN_FREQUENCY..MAX_FREQUENCY
        }
    }

    fun isValidTuning(note: String, octave: Int, baseFreq: Float = 440f): Boolean {
        val freq = calculateFreq(note, octave, baseFreq)
        return freq in MIN_FREQUENCY..MAX_FREQUENCY
    }

    fun clampToValidRange(note: String, octave: Int, baseFreq: Float = 440f): Pair<String, Int> {
        val freq = calculateFreq(note, octave, baseFreq)

        return when {
            freq < MIN_FREQUENCY -> {
                "E" to 1
            }
            freq > MAX_FREQUENCY -> {
                "E" to 5
            }
            else -> note to octave
        }
    }

    fun generateStandardTuning(stringCount: Int, baseFreq: Float = 440f): List<GuitarString> {
        return when (stringCount) {
            7 -> listOf(
                GuitarString(7, "B", 1, calculateFreq("B", 1, baseFreq)),
                GuitarString(6, "E", 2, calculateFreq("E", 2, baseFreq)),
                GuitarString(5, "A", 2, calculateFreq("A", 2, baseFreq)),
                GuitarString(4, "D", 3, calculateFreq("D", 3, baseFreq)),
                GuitarString(3, "G", 3, calculateFreq("G", 3, baseFreq)),
                GuitarString(2, "B", 3, calculateFreq("B", 3, baseFreq)),
                GuitarString(1, "E", 4, calculateFreq("E", 4, baseFreq))
            )
            else -> listOf(
                GuitarString(6, "E", 2, calculateFreq("E", 2, baseFreq)),
                GuitarString(5, "A", 2, calculateFreq("A", 2, baseFreq)),
                GuitarString(4, "D", 3, calculateFreq("D", 3, baseFreq)),
                GuitarString(3, "G", 3, calculateFreq("G", 3, baseFreq)),
                GuitarString(2, "B", 3, calculateFreq("B", 3, baseFreq)),
                GuitarString(1, "E", 4, calculateFreq("E", 4, baseFreq))
            )
        }
    }

    fun calculateFreq(note: String, octave: Int, baseFreq: Float = 440f): Float {
        val normalizedNote = convertFlatToSharp(note)
        val noteIndex = notes.indexOf(normalizedNote)
        if (noteIndex == -1) return 0f

        val midiNote = (octave + 1) * 12 + noteIndex
        val semitonesFromA4 = midiNote - 69

        return (baseFreq * 2.0.pow(semitonesFromA4 / 12.0)).toFloat()
    }

    fun convertSharpToFlat(note: String): String {
        return when (note) {
            "C#" -> "Db"
            "D#" -> "Eb"
            "F#" -> "Gb"
            "G#" -> "Ab"
            "A#" -> "Bb"
            else -> note
        }
    }

    fun convertFlatToSharp(note: String): String {
        return when (note) {
            "Db" -> "C#"
            "Eb" -> "D#"
            "Gb" -> "F#"
            "Ab" -> "G#"
            "Bb" -> "A#"
            else -> note
        }
    }
}