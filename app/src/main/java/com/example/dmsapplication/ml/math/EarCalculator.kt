package com.example.dmsapplication.ml.math

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.sqrt

/**
 * Tính toán độ mở của mắt (Eye Aspect Ratio - EAR).
 * Sử dụng khoảng cách 3D (X, Y, Z) để giữ độ chuẩn xác khi mặt hơi quay/nghiêng.
 */
object EarCalculator {

    private fun distance3D(p1: NormalizedLandmark, p2: NormalizedLandmark): Float {
        val dx = p1.x() - p2.x()
        val dy = p1.y() - p2.y()
        val dz = p1.z() - p2.z()
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    fun calculateEAR(landmarks: List<NormalizedLandmark>): Float {
        // a, b là 2 đường chiều dọc của mắt
        val a = distance3D(landmarks[1], landmarks[5])
        val b = distance3D(landmarks[2], landmarks[4])
        // c là đường chiều ngang (chiều dài mắt)
        val c = distance3D(landmarks[0], landmarks[3])

        if (c == 0f) return 0f
        return (a + b) / (2.0f * c)
    }
}