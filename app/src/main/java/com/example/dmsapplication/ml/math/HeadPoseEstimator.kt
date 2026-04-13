package com.example.dmsapplication.ml.math

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.sqrt

/**
 * Tính góc đầu (yaw, pitch) từ landmark khuôn mặt MediaPipe.
 *
 * YAW  (xoay trái/phải): dùng asymmetry ratio khoảng cách mũi–mắt
 * PITCH (cúi/ngẩng)     : dùng tỉ lệ động nose-to-eye / eye-to-chin
 *                         KHÔNG dùng hằng số cứng → chuẩn với mọi người
 *
 * Giá trị trả về đã chuẩn hoá:
 *   yaw   > 0 → quay phải  | yaw   < 0 → quay trái
 *   pitch > 0 → cúi xuống  | pitch < 0 → ngẩng lên
 *   Cả hai đều = 0.0 khi nhìn thẳng (sau calibration)
 */
object HeadPoseEstimator {

    data class HeadAngles(val yaw: Float, val pitch: Float)

    fun estimate(landmarks: List<NormalizedLandmark>): HeadAngles? {
        if (landmarks.size < 468) return null

        val noseTip  = landmarks[1]
        val chin     = landmarks[152]
        val leftEye  = landmarks[33]
        val rightEye = landmarks[263]

        // ── YAW ───────────────────────────────────────────────────────────
        val faceWidth = dist(leftEye, rightEye)
        if (faceWidth < 0.01f) return null

        // Khi quay phải: mũi gần mắt phải hơn → distLeft > distRight → yaw > 0
        val distLeft  = dist(noseTip, leftEye)
        val distRight = dist(noseTip, rightEye)
        val yaw = (distLeft - distRight) / faceWidth

        // ── PITCH ─────────────────────────────────────────────────────────
        // Dùng tỉ lệ: noseToEye / totalFaceHeight
        // Khi nhìn thẳng: tỉ lệ này ≈ 0.45 (mũi nằm gần giữa mắt–cằm)
        // Khi cúi: mũi đi xuống → tỉ lệ tăng
        // Khi ngẩng: mũi đi lên → tỉ lệ giảm
        // Để pitch = 0 khi thẳng → trừ đi giá trị trung bình thực tế
        // Thay vì hằng số cứng 0.40, dùng tỉ lệ giữa 2 khoảng cách:
        //   ratio = dist(nose, eyeMid) / dist(nose, chin)
        // Khi thẳng: ratio ≈ 0.55 (mũi gần mắt hơn cằm)
        // Khi cúi:   ratio giảm (mũi xa mắt hơn)
        // Khi ngẩng: ratio tăng (mũi gần mắt hơn)

        val eyeMidX = (leftEye.x() + rightEye.x()) / 2f
        val eyeMidY = (leftEye.y() + rightEye.y()) / 2f

        val noseToEye  = distXY(noseTip.x(), noseTip.y(), eyeMidX, eyeMidY)
        val noseToChin = distXY(noseTip.x(), noseTip.y(), chin.x(), chin.y())

        if (noseToChin < 0.01f) return null

        // ratio > 0.55 → ngẩng (pitch âm), ratio < 0.55 → cúi (pitch dương)
        // Đảo dấu để: cúi → pitch dương
        val pitchRatio = noseToEye / noseToChin
        val pitch = 0.55f - pitchRatio   // = 0 khi thẳng, >0 khi cúi, <0 khi ngẩng

        return HeadAngles(yaw = yaw, pitch = pitch)
    }

    private fun dist(a: NormalizedLandmark, b: NormalizedLandmark): Float {
        val dx = a.x() - b.x()
        val dy = a.y() - b.y()
        return sqrt(dx * dx + dy * dy)
    }

    private fun distXY(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x1 - x2
        val dy = y1 - y2
        return sqrt(dx * dx + dy * dy)
    }
}