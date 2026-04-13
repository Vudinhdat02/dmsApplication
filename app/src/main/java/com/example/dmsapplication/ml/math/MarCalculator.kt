package com.example.dmsapplication.ml.math

import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Tính toán độ mở của miệng (Mouth Aspect Ratio - MAR) để nhận diện ngáp.
 */
object MarCalculator {

    private fun distance(p1: FloatArray, p2: FloatArray): Float {
        return sqrt((p1[0] - p2[0]).pow(2) + (p1[1] - p2[1]).pow(2))
    }

    fun calculateMAR(topInner: FloatArray, bottomInner: FloatArray, leftInner: FloatArray, rightInner: FloatArray): Float {
        val height = distance(topInner, bottomInner)
        val width = distance(leftInner, rightInner)

        if (width == 0f) return 0f
        return height / width
    }
}