package com.example.dmsapplication.ml.math

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.sqrt
/**
 * Tính toán độ mở của miệng (Mouth Aspect Ratio - MAR) để nhận diện ngáp.
 * Sử dụng khoảng cách 3D (X, Y, Z) để tránh sai số khi tài xế hơi quay đầu.
 */
object MarCalculator {
    private fun distance3D(p1: NormalizedLandmark, p2: NormalizedLandmark): Float {
        val dx = p1.x() - p2.x()
        val dy = p1.y() - p2.y()
        val dz = p1.z() - p2.z()
        return sqrt(dx * dx + dy * dy + dz * dz)
    }
    fun calculateMAR(
        topInner: NormalizedLandmark,
        bottomInner: NormalizedLandmark,
        leftInner: NormalizedLandmark,
        rightInner: NormalizedLandmark
    ): Float {
        val height = distance3D(topInner, bottomInner)
        val width = distance3D(leftInner, rightInner)
        if (width == 0f) return 0f
        return height / width
    }
}