// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Vudinhdat02

package com.example.dmsapplication.ml.math

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.sqrt
/**
 * YAW (quay trái/phải): Chênh lệch độ sâu giữa mắt trái và mắt phải.
 * PITCH (cúi/ngẩng) : Chênh lệch độ sâu giữa Trán và Chóp mũi.
 * Việc sử dụng trục Z giúp thuật toán miễn nhiễm hoàn toàn với việc nghiêng đầu (Roll)
 * hoặc dịch chuyển tịnh tiến khuôn mặt trước camera.
 */
object HeadPoseEstimator {
    data class HeadAngles(val yaw: Float, val pitch: Float)
    fun estimate(landmarks: List<NormalizedLandmark>): HeadAngles? {
        if (landmarks.size < 468) return null
        val leftEye  = landmarks[33]   // Mép ngoài mắt trái
        val rightEye = landmarks[263]  // Mép ngoài mắt phải
        val forehead = landmarks[10]   // Đỉnh trán
        val noseTip  = landmarks[1]    // Chóp mũi
        // Tính khoảng cách 2D làm gốc chuẩn hoá
        // (Để tỷ lệ không bị ảnh hưởng khi mặt tiến lại gần hay lùi xa camera)
        val faceWidth = distXY(leftEye.x(), leftEye.y(), rightEye.x(), rightEye.y())
        val faceHeight = distXY(forehead.x(), forehead.y(), noseTip.x(), noseTip.y())
        if (faceWidth < 0.01f || faceHeight < 0.01f) return null
        // 1. YAW (Quay trái/phải)
        // Khi quay mặt, một mắt sẽ tiến gần camera hơn (Z âm hơn), mắt kia lùi xa hơn (Z dương hơn).
        // Nghiêng đầu (áp tai vào vai) không làm thay đổi độ sâu Z của 2 mắt.
        val yaw = (leftEye.z() - rightEye.z()) / faceWidth
        // 2. PITCH (Cúi/Ngửa)
        // Khi cúi, trán tiến gần camera hơn chóp mũi.
        // Khi ngửa, chóp mũi tiến gần camera hơn trán.
        // Trán và Mũi là xương cứng, há miệng ngáp không làm thay đổi khoảng cách này.
        val pitch = (forehead.z() - noseTip.z()) / faceHeight
        return HeadAngles(yaw = yaw, pitch = pitch)
    }
    private fun distXY(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x1 - x2
        val dy = y1 - y2
        return sqrt((dx * dx + dy * dy).toDouble()).toFloat()
    }
}