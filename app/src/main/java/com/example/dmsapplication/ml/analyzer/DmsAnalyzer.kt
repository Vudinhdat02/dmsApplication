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
    // Cập nhật Callback có thêm isYawning
    private val onResults: (isDrowsy: Boolean, isHeadDistracted: Boolean, isYawning: Boolean, result: FaceLandmarkerResult?) -> Unit
) : ImageAnalysis.Analyzer {

    var isSunglassesMode: Boolean = false
    var isYawnMode: Boolean = true // Bật/tắt giám sát ngáp

    // Ngưỡng nhắm mắt
    private val EAR_THRESHOLD         = 0.18f
    private val SMOOTH_WINDOW         = 3
    private val MOUTH_WIDTH_THRESHOLD = 0.5f
    private val EYE_CLOSED_DURATION   = 1000L

    // Ngưỡng quay đầu
    private val HEAD_DISTRACTED_DURATION = 1000L

    // Ngưỡng ngáp ngủ
    private val MAR_THRESHOLD = 0.5f     // Độ mở miệng khi ngáp (thường > 0.5)
    private val YAWN_DURATION = 800L     // Phải há miệng đủ 0.8s mới tính là ngáp (lọc nói chuyện)
    private val YAWN_COOLDOWN = 3000L    // Cooldown 3s để không đếm 1 cái ngáp thành nhiều lần

    // ── Landmark indices ──────────────────────────────────────────────────
    private val LEFT_EYE_INDICES  = listOf(362, 385, 387, 263, 373, 380)
    private val RIGHT_EYE_INDICES = listOf(33,  160, 158, 133, 153, 144)
    private val MOUTH_LEFT  = 61
    private val MOUTH_RIGHT = 291

    // Điểm bên trong môi để tính MAR
    private val INNER_LIP_TOP    = 13
    private val INNER_LIP_BOTTOM = 14
    private val INNER_MOUTH_LEFT = 78
    private val INNER_MOUTH_RIGHT= 308

    // State
    private val earHistory = ArrayDeque<Float>(SMOOTH_WINDOW)
    private var eyesClosedStartTime = 0L
    private var isDrowsyAlerting    = false

    private var headDistractedStartTime = 0L
    private var isHeadAlerting          = false

    private var yawnStartTime = 0L
    private var lastYawnTime = 0L
    private var isYawningAlerting = false

    override fun analyze(imageProxy: ImageProxy) {
        val bitmap  = imageProxy.toBitmap()
        val mpImage = BitmapImageBuilder(bitmap).build()
        val nowMs   = System.currentTimeMillis()

        val result = faceLandmarker.detectForVideo(mpImage, nowMs)

        if (result.faceLandmarks().isNotEmpty()) {
            val landmarks = result.faceLandmarks()[0]

            // 1. Tính EAR nhắm mắt
            val leftPts  = LEFT_EYE_INDICES.map  { floatArrayOf(landmarks[it].x(), landmarks[it].y()) }
            val rightPts = RIGHT_EYE_INDICES.map { floatArrayOf(landmarks[it].x(), landmarks[it].y()) }
            val rawEar   = (EarCalculator.calculateEAR(leftPts) + EarCalculator.calculateEAR(rightPts)) / 2f

            if (earHistory.size >= SMOOTH_WINDOW) earHistory.removeFirst()
            earHistory.addLast(rawEar)
            val smoothedEar = earHistory.average().toFloat()

            val mouthWidth = abs(landmarks[MOUTH_RIGHT].x() - landmarks[MOUTH_LEFT].x())

            val isEyesClosed = if (isSunglassesMode) {
                false
            } else {
                smoothedEar < EAR_THRESHOLD && mouthWidth < MOUTH_WIDTH_THRESHOLD
            }

            // 2. Tính góc đầu
            val headAngles = HeadPoseEstimator.estimate(landmarks)
            val isHeadOff  = if (headAngles != null && calibrationManager.isCalibrated) {
                calibrationManager.isHeadDistracted(headAngles)
            } else false

            // 3. Tính MAR ngáp ngủ
            val isYawningNow = if (isYawnMode) {
                val topLip    = floatArrayOf(landmarks[INNER_LIP_TOP].x(), landmarks[INNER_LIP_TOP].y())
                val bottomLip = floatArrayOf(landmarks[INNER_LIP_BOTTOM].x(), landmarks[INNER_LIP_BOTTOM].y())
                val leftMouth = floatArrayOf(landmarks[INNER_MOUTH_LEFT].x(), landmarks[INNER_MOUTH_LEFT].y())
                val rightMouth= floatArrayOf(landmarks[INNER_MOUTH_RIGHT].x(), landmarks[INNER_MOUTH_RIGHT].y())

                val mar = MarCalculator.calculateMAR(topLip, bottomLip, leftMouth, rightMouth)
                mar > MAR_THRESHOLD
            } else false

            // State machine nhắm mắt
            if (isEyesClosed) {
                if (eyesClosedStartTime == 0L) eyesClosedStartTime = nowMs
                if (nowMs - eyesClosedStartTime >= EYE_CLOSED_DURATION) {
                    isDrowsyAlerting = true
                }
            } else {
                eyesClosedStartTime = 0L
                isDrowsyAlerting    = false
            }

            // State machine quay đầu
            if (isHeadOff) {
                if (headDistractedStartTime == 0L) headDistractedStartTime = nowMs
                if (nowMs - headDistractedStartTime >= HEAD_DISTRACTED_DURATION) {
                    isHeadAlerting = true
                }
            } else {
                headDistractedStartTime = 0L
                isHeadAlerting          = false
            }

            // State machine ngáp ngủ (Xung Trigger 1 lần)
            if (isYawningNow) {
                if (yawnStartTime == 0L) yawnStartTime = nowMs
                // Nếu há miệng đủ lâu VÀ đã qua thời gian Cooldown từ lần ngáp trước
                if (nowMs - yawnStartTime >= YAWN_DURATION && nowMs - lastYawnTime > YAWN_COOLDOWN) {
                    isYawningAlerting = true
                    lastYawnTime = nowMs // Đánh dấu thời điểm ngáp để tính Cooldown
                } else {
                    isYawningAlerting = false // Ngay frame tiếp theo sẽ tắt để đếm đúng 1 lần
                }
            } else {
                yawnStartTime = 0L
                isYawningAlerting = false
            }

            // 5. Gửi kết quả mỗi frame
            onResults(isDrowsyAlerting, isHeadAlerting, isYawningAlerting, result)

        } else {
            resetAll()
            onResults(false, false, false, null)
        }

        imageProxy.close()
    }

    fun resetAll() {
        eyesClosedStartTime     = 0L
        headDistractedStartTime = 0L
        yawnStartTime           = 0L
        isDrowsyAlerting        = false
        isHeadAlerting          = false
        isYawningAlerting       = false
        earHistory.clear()
    }
}