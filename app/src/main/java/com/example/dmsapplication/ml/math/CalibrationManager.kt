package com.example.dmsapplication.ml.math

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth
import kotlin.math.abs

/**
 * Lưu và so sánh góc đầu chuẩn (baseline) sau khi người dùng calibrate.
 * Dữ liệu được lưu riêng biệt cho từng người dùng dựa trên Firebase UID.
 *
 * Ngưỡng phát hiện lệch mới (sử dụng công thức Z-Depth):
 *   YAW_THRESHOLD  : 0.20f (~ cho phép quay góc khoảng 15-20 độ)
 *   PITCH_THRESHOLD: 0.25f (~ cho phép cúi/ngẩng góc khoảng 15-20 độ)
 */
class CalibrationManager(context: Context) {

    companion object {
        private const val PREFS_NAME         = "dms_calibration"
        private const val KEY_BASELINE_YAW   = "baseline_yaw"
        private const val KEY_BASELINE_PITCH = "baseline_pitch"
        private const val KEY_IS_CALIBRATED  = "is_calibrated"

        const val YAW_THRESHOLD   = 0.20f
        const val PITCH_THRESHOLD = 0.25f
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Lấy UID của user hiện tại để làm prefix cho key
    private val currentUserUid: String
        get() = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"

    private fun getFullKey(key: String): String {
        return "${currentUserUid}_$key"
    }

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
        val yawDiff   = abs(current.yaw   - baselineYaw)
        val pitchDiff = abs(current.pitch - baselinePitch)
        return yawDiff > YAW_THRESHOLD || pitchDiff > PITCH_THRESHOLD
    }
}