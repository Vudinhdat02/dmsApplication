package com.example.dmsapplication.ml.math

import kotlin.math.pow
import kotlin.math.sqrt

object EarCalculator {

    private fun distance(p1: FloatArray, p2: FloatArray): Float {
        return sqrt((p1[0] - p2[0]).pow(2) + (p1[1] - p2[1]).pow(2))
    }

    fun calculateEAR(landmarks: List<FloatArray>): Float {
        val a = distance(landmarks[1], landmarks[5])
        val b = distance(landmarks[2], landmarks[4])
        val c = distance(landmarks[0], landmarks[3])
        return (a + b) / (2.0f * c)
    }
}