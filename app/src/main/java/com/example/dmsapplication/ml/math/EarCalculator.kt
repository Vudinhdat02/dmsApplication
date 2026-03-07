package com.example.dmsapplication.ml.math

import kotlin.math.pow
import kotlin.math.sqrt

object EarCalculator {

    // Công thức tính khoảng cách Euclidean giữa 2 điểm
    private fun distance(p1: FloatArray, p2: FloatArray): Float {
        return sqrt((p1[0] - p2[0]).pow(2) + (p1[1] - p2[1]).pow(2))
    }

    // Tính EAR cho một mắt dựa trên 6 điểm mốc
    fun calculateEAR(landmarks: List<FloatArray>): Float {
        // landmarks truyền vào là danh sách 6 điểm của một mắt theo thứ tự:
        // P1(trái), P2(trên trái), P3(trên phải), P4(phải), P5(dưới phải), P6(dưới trái)

        val a = distance(landmarks[1], landmarks[5])
        val b = distance(landmarks[2], landmarks[4])
        val c = distance(landmarks[0], landmarks[3])

        return (a + b) / (2.0f * c)
    }
}