package com.example.dmsapplication.ml.analyzer

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.dmsapplication.ml.math.EarCalculator
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker

class DmsAnalyzer(
    private val faceLandmarker: FaceLandmarker,
    private val onDrowsinessDetected: (Boolean) -> Unit
) : ImageAnalysis.Analyzer {

    private val EAR_THRESHOLD = 0.2f
    private var blinkCounter = 0
    private val BLINK_FRAME_LIMIT = 5
    private val LEFT_EYE_INDICES = listOf(362, 385, 387, 263, 373, 380)

    override fun analyze(imageProxy: ImageProxy) {
        val bitmap = imageProxy.toBitmap()
        val mpImage = BitmapImageBuilder(bitmap).build()

        // Sử dụng detectForVideo cho luồng Camera
        val result = faceLandmarker.detectForVideo(mpImage, imageProxy.imageInfo.timestamp)

        if (result.faceLandmarks().isNotEmpty()) {
            val landmarks = result.faceLandmarks()[0]
            val leftEyePoints = LEFT_EYE_INDICES.map { index ->
                val landmark = landmarks[index]
                floatArrayOf(landmark.x(), landmark.y())
            }
            val ear = EarCalculator.calculateEAR(leftEyePoints)

            if (ear < EAR_THRESHOLD) {
                blinkCounter++
                if (blinkCounter >= BLINK_FRAME_LIMIT) {
                    onDrowsinessDetected(true)
                    blinkCounter = 0
                }
            } else {
                blinkCounter = 0
            }
        }
        imageProxy.close()
    }
}