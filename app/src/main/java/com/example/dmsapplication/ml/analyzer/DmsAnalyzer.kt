// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Vudinhdat02

package com.example.dmsapplication.ml.analyzer

import android.os.SystemClock
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
    private val onResults: (
        isDrowsy: Boolean,
        isHeadDistracted: Boolean,
        isFaceNotVisible: Boolean,
        isYawning: Boolean,
        result: FaceLandmarkerResult?
    ) -> Unit
) : ImageAnalysis.Analyzer {
    companion object {
        const val DEFAULT_EAR_THRESHOLD = 0.16f
        const val MIN_EAR_THRESHOLD = 0.10f
        const val MAX_EAR_THRESHOLD = 0.30f
    }

    var isSunglassesMode: Boolean = false
    var isYawnMode: Boolean = true

    @Volatile
    var earThreshold: Float = DEFAULT_EAR_THRESHOLD
        set(value) {
            field = value.coerceIn(MIN_EAR_THRESHOLD, MAX_EAR_THRESHOLD)
        }

    private val smoothWindow = 3
    private val mouthWidthThreshold = 0.5f
    private val eyeClosedDurationMs = 800L
    private val headDistractedDurationMs = 1000L
    private val faceLostAlertDurationMs = 800L
    private val marThreshold = 0.38f
    private val yawnDurationMs = 800L
    private val yawnCooldownMs = 3000L

    private val leftEyeIndices = listOf(362, 385, 387, 263, 373, 380)
    private val rightEyeIndices = listOf(33, 160, 158, 133, 153, 144)
    private val mouthLeft = 61
    private val mouthRight = 291
    private val innerLipTop = 13
    private val innerLipBottom = 14
    private val innerMouthLeft = 78
    private val innerMouthRight = 308

    private val ear3DHistory = ArrayDeque<Float>(smoothWindow)
    private var eyesClosedStartTime = 0L
    private var isDrowsyAlerting = false
    private var yawnStartTime = 0L
    private var lastYawnTime = 0L
    private var isYawningAlerting = false
    private var headDistractedStartTime = 0L
    private var isHeadAlerting = false
    private var faceLostStartTime = 0L
    private var isFaceNotVisibleAlerting = false
    private val benchmarkRecorder = DmsBenchmarkRecorder()

    override fun analyze(imageProxy: ImageProxy) {
        val totalStartNs = SystemClock.elapsedRealtimeNanos()
        val preprocessingStartNs = totalStartNs
        var faceDetected = false

        try {
            val bitmap = imageProxy.toBitmap()
            val mpImage = BitmapImageBuilder(bitmap).build()
            val preprocessingEndNs = SystemClock.elapsedRealtimeNanos()
            val nowMs = SystemClock.elapsedRealtime()

            val inferenceStartNs = preprocessingEndNs
            val result = faceLandmarker.detectForVideo(mpImage, nowMs)
            val inferenceEndNs = SystemClock.elapsedRealtimeNanos()
            val postprocessingStartNs = inferenceEndNs
            faceDetected = result.faceLandmarks().isNotEmpty()

            if (faceDetected) {
                processDetectedFace(result, nowMs)
            } else {
                processMissingFace(nowMs)
            }

            val postprocessingEndNs = SystemClock.elapsedRealtimeNanos()
            benchmarkRecorder.record(
                preprocessingLatencyMs = (preprocessingEndNs - preprocessingStartNs) / 1_000_000.0,
                inferenceLatencyMs = (inferenceEndNs - inferenceStartNs) / 1_000_000.0,
                postprocessingLatencyMs = (postprocessingEndNs - postprocessingStartNs) / 1_000_000.0,
                totalLatencyMs = (postprocessingEndNs - totalStartNs) / 1_000_000.0,
                faceDetected = faceDetected
            )
        } finally {
            imageProxy.close()
        }
    }

    private fun processDetectedFace(
        result: FaceLandmarkerResult,
        nowMs: Long
    ) {
        faceLostStartTime = 0L
        isFaceNotVisibleAlerting = false
        val landmarks = result.faceLandmarks()[0]
        val leftEye = leftEyeIndices.map { landmarks[it] }
        val rightEye = rightEyeIndices.map { landmarks[it] }

        val rawEar3D =
            (EarCalculator.calculateEAR(leftEye) + EarCalculator.calculateEAR(rightEye)) / 2f
        val smoothedEar3D = smooth(ear3DHistory, rawEar3D)
        val mouthWidth = abs(landmarks[mouthRight].x() - landmarks[mouthLeft].x())
        val isEyesClosed =
            !isSunglassesMode &&
                smoothedEar3D < earThreshold &&
                mouthWidth < mouthWidthThreshold

        val relativeHeadAngles = HeadPoseEstimator.estimate(landmarks)
        val isHeadOff =
            relativeHeadAngles != null &&
                calibrationManager.isCalibrated &&
                calibrationManager.isHeadDistracted(relativeHeadAngles)

        val mar3D = MarCalculator.calculateMAR(
            landmarks[innerLipTop],
            landmarks[innerLipBottom],
            landmarks[innerMouthLeft],
            landmarks[innerMouthRight]
        )
        val isYawningNow = isYawnMode && mar3D > marThreshold
        updateEyeState(isEyesClosed, nowMs)
        updateHeadState(isHeadOff, nowMs)
        updateYawnState(isYawningNow, nowMs)
        onResults(
            isDrowsyAlerting,
            isHeadAlerting,
            isFaceNotVisibleAlerting,
            isYawningAlerting,
            result
        )
    }

    private fun processMissingFace(nowMs: Long) {
        resetEyeAndYawnState()
        if (calibrationManager.isCalibrated) {
            if (faceLostStartTime == 0L) faceLostStartTime = nowMs
            val faceLostMs = nowMs - faceLostStartTime
            isFaceNotVisibleAlerting = faceLostMs >= faceLostAlertDurationMs
            isHeadAlerting = false
            headDistractedStartTime = 0L
            onResults(false, false, isFaceNotVisibleAlerting, false, null)
        } else {
            resetAll()
            onResults(false, false, false, false, null)
        }
    }

    private fun updateEyeState(isEyesClosed: Boolean, nowMs: Long) {
        if (isEyesClosed) {
            if (eyesClosedStartTime == 0L) eyesClosedStartTime = nowMs
            if (nowMs - eyesClosedStartTime >= eyeClosedDurationMs) isDrowsyAlerting = true
        } else {
            eyesClosedStartTime = 0L
            isDrowsyAlerting = false
        }
    }

    private fun updateHeadState(isHeadOff: Boolean, nowMs: Long) {
        if (isHeadOff) {
            if (headDistractedStartTime == 0L) headDistractedStartTime = nowMs
            if (nowMs - headDistractedStartTime >= headDistractedDurationMs) {
                isHeadAlerting = true
            }
        } else {
            headDistractedStartTime = 0L
            isHeadAlerting = false
        }
    }

    private fun updateYawnState(isYawningNow: Boolean, nowMs: Long) {
        if (isYawningNow) {
            if (yawnStartTime == 0L) yawnStartTime = nowMs
            if (nowMs - yawnStartTime >= yawnDurationMs && nowMs - lastYawnTime > yawnCooldownMs) {
                isYawningAlerting = true
                lastYawnTime = nowMs
            } else {
                isYawningAlerting = false
            }
        } else {
            yawnStartTime = 0L
            isYawningAlerting = false
        }
    }

    private fun smooth(history: ArrayDeque<Float>, value: Float): Float {
        if (history.size >= smoothWindow) history.removeFirst()
        history.addLast(value)
        return history.average().toFloat()
    }


    private fun resetEyeAndYawnState() {
        ear3DHistory.clear()
        eyesClosedStartTime = 0L
        isDrowsyAlerting = false
        yawnStartTime = 0L
        isYawningAlerting = false
    }

    fun resetAll() {
        eyesClosedStartTime = 0L
        headDistractedStartTime = 0L
        yawnStartTime = 0L
        faceLostStartTime = 0L
        isDrowsyAlerting = false
        isHeadAlerting = false
        isFaceNotVisibleAlerting = false
        isYawningAlerting = false
        ear3DHistory.clear()
    }
}
