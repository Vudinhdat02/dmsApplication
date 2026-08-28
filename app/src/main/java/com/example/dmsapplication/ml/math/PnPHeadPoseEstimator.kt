// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Vudinhdat02

package com.example.dmsapplication.ml.math

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import org.opencv.calib3d.Calib3d
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfDouble
import org.opencv.core.MatOfPoint2f
import org.opencv.core.MatOfPoint3f
import org.opencv.core.Point
import org.opencv.core.Point3
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.sqrt

/** Six-point solvePnP baseline using the same MediaPipe landmarks as the proposed method. */
object PnPHeadPoseEstimator {
    data class HeadAngles(val yawDegrees: Float, val pitchDegrees: Float, val rollDegrees: Float)

    private const val NOSE_TIP = 1
    private const val CHIN = 152
    private const val LEFT_EYE_OUTER = 33
    private const val RIGHT_EYE_OUTER = 263
    private const val LEFT_MOUTH = 61
    private const val RIGHT_MOUTH = 291

    fun estimate(
        landmarks: List<NormalizedLandmark>,
        imageWidth: Int,
        imageHeight: Int
    ): HeadAngles? {
        if (landmarks.size < 468 || imageWidth <= 0 || imageHeight <= 0) return null

        val imagePoints = MatOfPoint2f(
            point(landmarks[NOSE_TIP], imageWidth, imageHeight),
            point(landmarks[CHIN], imageWidth, imageHeight),
            point(landmarks[LEFT_EYE_OUTER], imageWidth, imageHeight),
            point(landmarks[RIGHT_EYE_OUTER], imageWidth, imageHeight),
            point(landmarks[LEFT_MOUTH], imageWidth, imageHeight),
            point(landmarks[RIGHT_MOUTH], imageWidth, imageHeight)
        )
        val modelPoints = MatOfPoint3f(
            Point3(0.0, 0.0, 0.0),
            Point3(0.0, -330.0, -65.0),
            Point3(-225.0, 170.0, -135.0),
            Point3(225.0, 170.0, -135.0),
            Point3(-150.0, -150.0, -125.0),
            Point3(150.0, -150.0, -125.0)
        )

        // This pinhole approximation is suitable for an in-app baseline. A publication-grade
        // experiment should replace it with checkerboard-calibrated intrinsics for the device.
        val focalLength = max(imageWidth, imageHeight).toDouble()
        val cameraMatrix = Mat.eye(3, 3, CvType.CV_64F).apply {
            put(0, 0, focalLength)
            put(1, 1, focalLength)
            put(0, 2, imageWidth / 2.0)
            put(1, 2, imageHeight / 2.0)
        }
        val distortionCoefficients = MatOfDouble()
        val rotationVector = Mat()
        val translationVector = Mat()
        val rotationMatrix = Mat()

        return try {
            val solved = Calib3d.solvePnP(
                modelPoints,
                imagePoints,
                cameraMatrix,
                distortionCoefficients,
                rotationVector,
                translationVector,
                false,
                Calib3d.SOLVEPNP_ITERATIVE
            )
            if (!solved) return null

            Calib3d.Rodrigues(rotationVector, rotationMatrix)
            val r00 = rotationMatrix.get(0, 0)[0]
            val r10 = rotationMatrix.get(1, 0)[0]
            val r20 = rotationMatrix.get(2, 0)[0]
            val r21 = rotationMatrix.get(2, 1)[0]
            val r22 = rotationMatrix.get(2, 2)[0]
            val singularity = sqrt(r00 * r00 + r10 * r10)

            val pitch = atan2(r21, r22)
            val yaw = atan2(-r20, singularity)
            val roll = atan2(r10, r00)
            val degrees = 180.0 / Math.PI
            HeadAngles(
                yawDegrees = (yaw * degrees).toFloat(),
                pitchDegrees = (pitch * degrees).toFloat(),
                rollDegrees = (roll * degrees).toFloat()
            )
        } catch (_: RuntimeException) {
            null
        } finally {
            imagePoints.release()
            modelPoints.release()
            cameraMatrix.release()
            distortionCoefficients.release()
            rotationVector.release()
            translationVector.release()
            rotationMatrix.release()
        }
    }

    private fun point(landmark: NormalizedLandmark, width: Int, height: Int): Point =
        Point((landmark.x() * width).toDouble(), (landmark.y() * height).toDouble())
}
