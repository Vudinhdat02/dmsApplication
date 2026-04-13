package com.example.dmsapplication.ml.math

import android.content.Context
import android.content.SharedPreferences
import kotlin.math.abs

/**
 * Lưu và so sánh góc đầu chuẩn (baseline) sau khi người dùng calibrate.
 *
 * Ngưỡng phát hiện lệch (sau khi đã trừ baseline):
 *   YAW_THRESHOLD  : 0.08 → quay ~10° là cảnh báo
 *   PITCH_THRESHOLD: 0.06 → cúi/ngẩng ~8° là cảnh báo
 *
 * Các ngưỡng này nhỏ để đủ nhạy, nhưng timer 1s trong DmsAnalyzer
 * sẽ lọc những lệch nhất thời (nhìn sang gương chiếu hậu nhanh).
 */
class CalibrationManager(context: Context) {

    companion object {
        private const val PREFS_NAME         = "dms_calibration"
        private const val KEY_BASELINE_YAW   = "baseline_yaw"
        private const val KEY_BASELINE_PITCH = "baseline_pitch"
        private const val KEY_IS_CALIBRATED  = "is_calibrated"

        const val YAW_THRESHOLD   = 0.08f
        const val PITCH_THRESHOLD = 0.06f
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val isCalibrated: Boolean
        get() = prefs.getBoolean(KEY_IS_CALIBRATED, false)

    val baselineYaw: Float
        get() = prefs.getFloat(KEY_BASELINE_YAW, 0f)

    val baselinePitch: Float
        get() = prefs.getFloat(KEY_BASELINE_PITCH, 0f)

    fun saveBaseline(angles: HeadPoseEstimator.HeadAngles) {
        prefs.edit()
            .putFloat(KEY_BASELINE_YAW, angles.yaw)
            .putFloat(KEY_BASELINE_PITCH, angles.pitch)
            .putBoolean(KEY_IS_CALIBRATED, true)
            .apply()
    }

    fun reset() {
        prefs.edit()
            .remove(KEY_BASELINE_YAW)
            .remove(KEY_BASELINE_PITCH)
            .putBoolean(KEY_IS_CALIBRATED, false)
            .apply()
    }

    fun isHeadDistracted(current: HeadPoseEstimator.HeadAngles): Boolean {
        if (!isCalibrated) return false
        val yawDiff   = abs(current.yaw   - baselineYaw)
        val pitchDiff = abs(current.pitch - baselinePitch)
        return yawDiff > YAW_THRESHOLD || pitchDiff > PITCH_THRESHOLD
    }
}