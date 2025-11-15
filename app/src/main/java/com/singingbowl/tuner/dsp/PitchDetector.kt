package com.singingbowl.tuner.dsp

import kotlin.math.abs

class PitchDetector(private val sampleRate: Int, private val bufferSize: Int) {

    private val yinBuffer = FloatArray(bufferSize / 2)
    private var probability: Float = 0.0f
    private var threshold: Float = 0.15f // Standard YIN threshold

    /**
     * Estimates the fundamental frequency of the audio buffer.
     * @param audioBuffer The audio buffer to analyze.
     * @return The estimated frequency in Hz, or -1.0f if no pitch is detected.
     */
    fun getPitch(audioBuffer: ShortArray): Float {
        // Step 1: Convert ShortArray to FloatArray
        val floatAudioBuffer = audioBuffer.map { it / 32768.0f }.toFloatArray()

        // Step 2: Difference function
        difference(floatAudioBuffer)

        // Step 3: Cumulative mean normalized difference
        cumulativeMeanNormalizedDifference()

        // Step 4: Absolute threshold
        val period = absoluteThreshold()

        // Step 5: Parabolic interpolation for better accuracy
        val estimatedPeriod = if (period != -1) {
            parabolicInterpolation(period)
        } else {
            -1.0f
        }

        // Step 6: Convert period to frequency
        return if (estimatedPeriod != -1.0f) {
            sampleRate / estimatedPeriod
        } else {
            -1.0f
        }
    }

    private fun difference(audioBuffer: FloatArray) {
        for (tau in 0 until yinBuffer.size) {
            yinBuffer[tau] = 0.0f
        }
        for (tau in 1 until yinBuffer.size) {
            for (j in 0 until yinBuffer.size) {
                val delta = audioBuffer[j] - audioBuffer[j + tau]
                yinBuffer[tau] += delta * delta
            }
        }
    }

    private fun cumulativeMeanNormalizedDifference() {
        var runningSum = 0.0f
        yinBuffer[0] = 1.0f
        for (tau in 1 until yinBuffer.size) {
            runningSum += yinBuffer[tau]
            yinBuffer[tau] *= tau / runningSum
        }
    }

    private fun absoluteThreshold(): Int {
        var tau = -1
        for (t in 1 until yinBuffer.size) {
            if (yinBuffer[t] < threshold) {
                tau = t
                while (t + 1 < yinBuffer.size && yinBuffer[t + 1] < yinBuffer[t]) {
                    tau = t + 1
                }
                probability = 1.0f - yinBuffer[tau]
                break
            }
        }
        return tau
    }

    private fun parabolicInterpolation(tau: Int): Float {
        if (tau == 0 || tau >= yinBuffer.size - 1) {
            return tau.toFloat()
        }
        val x0 = tau - 1
        val x2 = tau + 1
        val y0 = yinBuffer[x0]
        val y1 = yinBuffer[tau]
        val y2 = yinBuffer[x2]
        val betterTau = tau + (y2 - y0) / (2 * (2 * y1 - y2 - y0))
        return betterTau
    }
}
