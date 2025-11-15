package com.singingbowl.tuner

import kotlin.math.log2
import kotlin.math.roundToInt

object PitchConverter {

    private val noteNames = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    private var a4Frequency = 440.0

    fun setA4(frequency: Double) {
        a4Frequency = frequency
    }

    fun a4(): Double {
        return a4Frequency
    }

    fun frequencyToNote(frequency: Double): NoteResult {
        if (frequency <= 0) {
            return NoteResult("", "", 0)
        }

        val midiNote = 12 * log2(frequency / a4Frequency) + 69
        val noteIndex = midiNote.roundToInt()
        if (noteIndex < 0 || noteIndex >= 128) {
            return NoteResult("", "", 0)
        }

        val octave = noteIndex / 12 - 1
        val noteName = noteNames[noteIndex % 12]

        val expectedFrequency = a4Frequency * Math.pow(2.0, (noteIndex - 69) / 12.0)
        val cents = 1200 * log2(frequency / expectedFrequency)

        return NoteResult(noteName, "$noteName$octave", cents.roundToInt())
    }

    data class NoteResult(val noteName: String, val fullNoteName: String, val cents: Int)
}
