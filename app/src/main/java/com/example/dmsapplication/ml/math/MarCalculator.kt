// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Vudinhdat02

package com.example.dmsapplication.ml.math

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.sqrt
/**
 * Tính toán độ mở của miệng (Mouth Aspect Ratio - MAR) để nhận diện ngáp.
 * Sử dụng khoảng cách 3D (X, Y, Z) để tránh sai số khi tài xế hơi quay đầu.
 */
object MarCalculator {
    private fun distance2D(
        p1: NormalizedLandmark,
        p2: NormalizedLandmark,
        imageWidth: Int,
        imageHeight: Int
    ): Float {
        val dx = (p1.x() - p2.x()) * imageWidth
        val dy = (p1.y() - p2.y()) * imageHeight
        return sqrt(dx * dx + dy * dy)
    }

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

    /** Classical 2D MAR baseline. Depth is deliberately ignored. */
    fun calculateMAR2D(
        topInner: NormalizedLandmark,
        bottomInner: NormalizedLandmark,
        leftInner: NormalizedLandmark,
        rightInner: NormalizedLandmark,
        imageWidth: Int,
        imageHeight: Int
    ): Float {
        val height = distance2D(topInner, bottomInner, imageWidth, imageHeight)
        val width = distance2D(leftInner, rightInner, imageWidth, imageHeight)
        if (width == 0f) return 0f
        return height / width
    }
}
