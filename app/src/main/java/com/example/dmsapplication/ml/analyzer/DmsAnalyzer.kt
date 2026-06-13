package com.example.dmsapplication.ml.analyzer

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.dmsapplication.ml.math.CalibrationManager
import com.example.dmsapplication.ml.math.EarCalculator
import com.example.dmsapplication.ml.math.HeadPoseEstimator
import com.example.dmsapplication.ml.math.MarCalculator
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import kotlin.math.abs

class DmsAnalyzer(
    private val faceLandmarker: FaceLandmarker,
    private val calibrationManager: CalibrationManager,
    private val onResults: (isDrowsy: Boolean, isHeadDistracted: Boolean, isYawning: Boolean, result: FaceLandmarkerResult?) -> Unit
) : ImageAnalysis.Analyzer {

    var isSunglassesMode: Boolean = false
    var isYawnMode: Boolean = true
    // Ngưỡng nhắm mắt
    private val EAR_THRESHOLD         = 0.16f
    private val SMOOTH_WINDOW         = 3
    private val MOUTH_WIDTH_THRESHOLD = 0.5f
    private val EYE_CLOSED_DURATION   = 800L
    //ngưỡng quay đầu
    private val HEAD_DISTRACTED_DURATION = 1000L
    //Nếu mất mặt NGẮN hơn mốc này → đang quay đầu nhanh → cảnh báo.
    private val FACE_LOST_ALERT_DURATION   = 800L
    //Nếu mất mặt DÀI hơn mốc này  → người dùng có thể rời đi → tắt cảnh báo.
    private val FACE_LOST_SILENCE_DURATION = 5000L

    // Ngưỡng ngáp ngủ
    private val MAR_THRESHOLD = 0.38f
    private val YAWN_DURATION = 800L
    private val YAWN_COOLDOWN = 3000L

    private val LEFT_EYE_INDICES  = listOf(362, 385, 387, 263, 373, 380)
    private val RIGHT_EYE_INDICES = listOf(33,  160, 158, 133, 153, 144)
    private val MOUTH_LEFT  = 61
    private val MOUTH_RIGHT = 291
    private val INNER_LIP_TOP    = 13
    private val INNER_LIP_BOTTOM = 14
    private val INNER_MOUTH_LEFT = 78
    private val INNER_MOUTH_RIGHT= 308

    // State — mắt & ngáp
    private val earHistory = ArrayDeque<Float>(SMOOTH_WINDOW)
    private var eyesClosedStartTime = 0L
    private var isDrowsyAlerting    = false
    private var yawnStartTime     = 0L
    private var lastYawnTime      = 0L
    private var isYawningAlerting = false

    // State — quay đầu (có mặt)
    private var headDistractedStartTime = 0L
    private var isHeadAlerting          = false

    // State — mất khuôn mặt
    private var faceLostStartTime = 0L   // Thời điểm bắt đầu mất mặt

    override fun analyze(imageProxy: ImageProxy) {
        val bitmap  = imageProxy.toBitmap()
        val mpImage = BitmapImageBuilder(bitmap).build()
        val nowMs   = System.currentTimeMillis()
        //AI Facelandmarker xử lý ảnh
        val result = faceLandmarker.detectForVideo(mpImage, nowMs)

        if (result.faceLandmarks().isNotEmpty()) {
            // Khuôn mặt được phát hiện → reset bộ đếm mất mặt
            faceLostStartTime = 0L
            val landmarks = result.faceLandmarks()[0]
            // 1. EAR — nhắm mắt
            val leftPts  = LEFT_EYE_INDICES.map  { landmarks[it] }
            val rightPts = RIGHT_EYE_INDICES.map { landmarks[it] }
            val rawEar   = (EarCalculator.calculateEAR(leftPts) + EarCalculator.calculateEAR(rightPts)) / 2f

            if (earHistory.size >= SMOOTH_WINDOW) earHistory.removeFirst()
            earHistory.addLast(rawEar)
            val smoothedEar = earHistory.average().toFloat()

            val mouthWidth = abs(landmarks[MOUTH_RIGHT].x() - landmarks[MOUTH_LEFT].x())
            val isEyesClosed = if (isSunglassesMode) false
            else smoothedEar < EAR_THRESHOLD && mouthWidth < MOUTH_WIDTH_THRESHOLD

            // 2. Góc đầu
            val headAngles = HeadPoseEstimator.estimate(landmarks)
            val isHeadOff  = if (headAngles != null && calibrationManager.isCalibrated)
                calibrationManager.isHeadDistracted(headAngles)
            else false

            // 3. MAR — ngáp
            val isYawningNow = if (isYawnMode) {
                val mar = MarCalculator.calculateMAR(
                    landmarks[INNER_LIP_TOP],
                    landmarks[INNER_LIP_BOTTOM],
                    landmarks[INNER_MOUTH_LEFT],
                    landmarks[INNER_MOUTH_RIGHT]
                )
                mar > MAR_THRESHOLD
            } else false

            // State machine nhắm mắt
            if (isEyesClosed) {
                if (eyesClosedStartTime == 0L) eyesClosedStartTime = nowMs
                if (nowMs - eyesClosedStartTime >= EYE_CLOSED_DURATION) isDrowsyAlerting = true
            } else {
                eyesClosedStartTime = 0L
                isDrowsyAlerting    = false
            }

            // State machine quay đầu (có mặt)
            if (isHeadOff) {
                if (headDistractedStartTime == 0L) headDistractedStartTime = nowMs
                if (nowMs - headDistractedStartTime >= HEAD_DISTRACTED_DURATION) isHeadAlerting = true
            } else {
                headDistractedStartTime = 0L
                isHeadAlerting          = false
            }

            // State machine ngáp
            if (isYawningNow) {
                if (yawnStartTime == 0L) yawnStartTime = nowMs
                if (nowMs - yawnStartTime >= YAWN_DURATION && nowMs - lastYawnTime > YAWN_COOLDOWN) {
                    isYawningAlerting = true
                    lastYawnTime = nowMs
                } else {
                    isYawningAlerting = false
                }
            } else {
                yawnStartTime     = 0L
                isYawningAlerting = false
            }

            onResults(isDrowsyAlerting, isHeadAlerting, isYawningAlerting, result)

        } else {
            // Không phát hiện khuôn mặt
            resetEyeAndYawnState()
            // Chỉ áp dụng logic "mất mặt = quay đầu" khi đã calibrate
            if (calibrationManager.isCalibrated) {
                if (faceLostStartTime == 0L) faceLostStartTime = nowMs
                val faceLostMs = nowMs - faceLostStartTime
                isHeadAlerting = when {
                    // Vừa mất mặt, chưa đủ ngưỡng thời gian → chưa cảnh báo
                    faceLostMs < FACE_LOST_ALERT_DURATION   -> false
                    // Mất mặt trong khoảng cảnh báo → đang quay đầu
                    faceLostMs < FACE_LOST_SILENCE_DURATION -> true
                    // Mất mặt quá lâu → người dùng rời đi, im lặng
                    else                                    -> false
                }
                // Giữ nguyên headDistractedStartTime = 0 vì không cần dùng ở nhánh này
                headDistractedStartTime = 0L
                onResults(false, isHeadAlerting, false, null)
            } else {
                resetAll()
                onResults(false, false, false, null)
            }
        }
        imageProxy.close()
    }

    private fun resetEyeAndYawnState() {
        earHistory.clear()
        eyesClosedStartTime = 0L
        isDrowsyAlerting    = false
        yawnStartTime       = 0L
        isYawningAlerting   = false
    }

    fun resetAll() {
        eyesClosedStartTime     = 0L
        headDistractedStartTime = 0L
        yawnStartTime           = 0L
        faceLostStartTime       = 0L
        isDrowsyAlerting        = false
        isHeadAlerting          = false
        isYawningAlerting       = false
        earHistory.clear()
    }
}