// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Vudinhdat02

package com.example.dmsapplication.ml.math

import android.content.Context
import android.content.SharedPreferences
import com.example.dmsapplication.ml.analyzer.BaselineScoreCalculator
import com.google.firebase.auth.FirebaseAuth
import kotlin.math.abs
import kotlin.math.max

class CalibrationManager(context: Context) {
    companion object {
        private const val PREFS_NAME = "dms_calibration"
        private const val KEY_BASELINE_YAW = "baseline_yaw"
        private const val KEY_BASELINE_PITCH = "baseline_pitch"
        private const val KEY_IS_CALIBRATED = "is_calibrated"
        private const val KEY_PNP_BASELINE_YAW = "pnp_baseline_yaw"
        private const val KEY_PNP_BASELINE_PITCH = "pnp_baseline_pitch"
        private const val KEY_IS_PNP_CALIBRATED = "is_pnp_calibrated"
        private const val KEY_PNP_CALIBRATION_VERSION = "pnp_calibration_version"
        private const val PNP_CALIBRATION_VERSION = 2

        const val YAW_THRESHOLD = 0.20f
        const val PITCH_THRESHOLD = 0.25f
        const val PNP_YAW_THRESHOLD_DEGREES = 30f
        const val PNP_PITCH_THRESHOLD_DEGREES = 20f
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val currentUserUid: String
        get() = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"

    private fun getFullKey(key: String): String = "${currentUserUid}_$key"

    val isCalibrated: Boolean
        get() = prefs.getBoolean(getFullKey(KEY_IS_CALIBRATED), false)
    val baselineYaw: Float
        get() = prefs.getFloat(getFullKey(KEY_BASELINE_YAW), 0f)
    val baselinePitch: Float
        get() = prefs.getFloat(getFullKey(KEY_BASELINE_PITCH), 0f)

    val isPnpCalibrated: Boolean
        get() =
            prefs.getBoolean(getFullKey(KEY_IS_PNP_CALIBRATED), false) &&
                prefs.getInt(getFullKey(KEY_PNP_CALIBRATION_VERSION), 0) ==
                PNP_CALIBRATION_VERSION
    val pnpBaselineYaw: Float
        get() = prefs.getFloat(getFullKey(KEY_PNP_BASELINE_YAW), 0f)
    val pnpBaselinePitch: Float
        get() = prefs.getFloat(getFullKey(KEY_PNP_BASELINE_PITCH), 0f)

    fun saveBaseline(angles: HeadPoseEstimator.HeadAngles) {
        prefs.edit()
            .putFloat(getFullKey(KEY_BASELINE_YAW), angles.yaw)
            .putFloat(getFullKey(KEY_BASELINE_PITCH), angles.pitch)
            .putBoolean(getFullKey(KEY_IS_CALIBRATED), true)
            .apply()
    }

    fun savePnpBaseline(angles: PnPHeadPoseEstimator.HeadAngles) {
        prefs.edit()
            .putFloat(getFullKey(KEY_PNP_BASELINE_YAW), angles.yawDegrees)
            .putFloat(getFullKey(KEY_PNP_BASELINE_PITCH), angles.pitchDegrees)
            .putBoolean(getFullKey(KEY_IS_PNP_CALIBRATED), true)
            .putInt(getFullKey(KEY_PNP_CALIBRATION_VERSION), PNP_CALIBRATION_VERSION)
            .apply()
    }

    fun reset() {
        prefs.edit()
            .remove(getFullKey(KEY_BASELINE_YAW))
            .remove(getFullKey(KEY_BASELINE_PITCH))
            .remove(getFullKey(KEY_PNP_BASELINE_YAW))
            .remove(getFullKey(KEY_PNP_BASELINE_PITCH))
            .remove(getFullKey(KEY_PNP_CALIBRATION_VERSION))
            .putBoolean(getFullKey(KEY_IS_CALIBRATED), false)
            .putBoolean(getFullKey(KEY_IS_PNP_CALIBRATED), false)
            .apply()
    }

    fun isHeadDistracted(current: HeadPoseEstimator.HeadAngles): Boolean {
        if (!isCalibrated) return false
        val yawDiff = abs(current.yaw - baselineYaw)
        val pitchDiff = abs(current.pitch - baselinePitch)
        return yawDiff > YAW_THRESHOLD || pitchDiff > PITCH_THRESHOLD
    }

    fun relativeHeadScore(current: HeadPoseEstimator.HeadAngles?): Float {
        if (!isCalibrated || current == null) return 0f
        return max(
            abs(current.yaw - baselineYaw) / YAW_THRESHOLD,
            abs(current.pitch - baselinePitch) / PITCH_THRESHOLD
        )
    }

    fun pnpHeadScore(current: PnPHeadPoseEstimator.HeadAngles?): Float? {
        if (!isPnpCalibrated || current == null) return null
        val yawDifference = BaselineScoreCalculator.normalizedAngleDifferenceDegrees(
            current.yawDegrees,
            pnpBaselineYaw
        )
        val pitchDifference = BaselineScoreCalculator.normalizedAngleDifferenceDegrees(
            current.pitchDegrees,
            pnpBaselinePitch
        )
        return max(
            yawDifference / PNP_YAW_THRESHOLD_DEGREES,
            pitchDifference / PNP_PITCH_THRESHOLD_DEGREES
        )
    }
}
