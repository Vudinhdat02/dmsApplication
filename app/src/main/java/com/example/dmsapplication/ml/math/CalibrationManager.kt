// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Vudinhdat02

package com.example.dmsapplication.ml.math

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth
import kotlin.math.abs

class CalibrationManager(context: Context) {
    companion object {
        private const val PREFS_NAME = "dms_calibration"
        private const val KEY_BASELINE_YAW = "baseline_yaw"
        private const val KEY_BASELINE_PITCH = "baseline_pitch"
        private const val KEY_IS_CALIBRATED = "is_calibrated"
        const val YAW_THRESHOLD = 0.20f
        const val PITCH_THRESHOLD = 0.25f
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

    fun saveBaseline(angles: HeadPoseEstimator.HeadAngles) {
        prefs.edit()
            .putFloat(getFullKey(KEY_BASELINE_YAW), angles.yaw)
            .putFloat(getFullKey(KEY_BASELINE_PITCH), angles.pitch)
            .putBoolean(getFullKey(KEY_IS_CALIBRATED), true)
            .apply()
    }

    fun reset() {
        prefs.edit()
            .remove(getFullKey(KEY_BASELINE_YAW))
            .remove(getFullKey(KEY_BASELINE_PITCH))
            .putBoolean(getFullKey(KEY_IS_CALIBRATED), false)
            .apply()
    }

    fun isHeadDistracted(current: HeadPoseEstimator.HeadAngles): Boolean {
        if (!isCalibrated) return false
        val yawDiff = abs(current.yaw - baselineYaw)
        val pitchDiff = abs(current.pitch - baselinePitch)
        return yawDiff > YAW_THRESHOLD || pitchDiff > PITCH_THRESHOLD
    }

}
