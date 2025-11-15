package com.singingbowl.tuner

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.sin

class PitchDetectorTest {

    private val sampleRate = 44100
    private val bufferSize = 4096

    @Test
    fun pitchDetector_detects_440hz_sine_wave() {
        val frequency = 440.0f
        val audioBuffer = ShortArray(bufferSize) {
            (sin(2 * Math.PI * frequency * it / sampleRate) * Short.MAX_VALUE).toInt().toShort()
        }

        val pitchDetector = PitchDetector(sampleRate, bufferSize)
        val detectedPitch = pitchDetector.getPitch(audioBuffer)

        // We allow a small tolerance due to the nature of pitch detection algorithms
        assertEquals(frequency, detectedPitch, 10.0f)
    }
}
