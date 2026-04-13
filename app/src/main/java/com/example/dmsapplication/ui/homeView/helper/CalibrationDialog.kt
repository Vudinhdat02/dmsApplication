package com.example.dmsapplication.ui.homeView.helper

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.example.dmsapplication.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 *  4. Kết thúc → gọi [onCalibrationComplete] để Fragment lưu baseline
 */
class CalibrationDialog : DialogFragment() {

    /** Fragment chủ implement interface này để nhận sự kiện hoàn tất calibration */
    interface CalibrationListener {
        fun onCalibrationComplete()
        fun onCalibrationCancelled()
    }

    private var listener: CalibrationListener? = null
    private var countDownTimer: CountDownTimer? = null
    private lateinit var tvDesc: TextView
    private lateinit var tvCountdown: TextView
    private lateinit var pbCalibration: ProgressBar
    private lateinit var btnStart: Button

    fun setCalibrationListener(l: CalibrationListener) {
        listener = l
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_calibration, null)

        tvDesc        = view.findViewById(R.id.tvCalibrationDesc)
        tvCountdown   = view.findViewById(R.id.tvCountdown)
        pbCalibration = view.findViewById(R.id.pbCalibration)
        btnStart      = view.findViewById(R.id.btnStartCalibration)

        btnStart.setOnClickListener { startCountdown() }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(view)
            .setCancelable(false)
            .create()

        // Nút Cancel ngoài builder → thêm bằng tay
        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        }

        return dialog
    }

    private fun startCountdown() {
        btnStart.isEnabled = false
        btnStart.text      = "Đang đếm ngược..."
        tvDesc.text        = "Hãy ngồi thẳng, nhìn về phía trước như khi lái xe bình thường."
        pbCalibration.visibility = View.VISIBLE
        tvCountdown.visibility   = View.VISIBLE

        countDownTimer = object : CountDownTimer(5_000L, 1_000L) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = (millisUntilFinished / 1_000L) + 1
                tvCountdown.text = secondsLeft.toString()
            }

            override fun onFinish() {
                tvCountdown.text     = "OK"
                tvCountdown.setTextColor(Color.GREEN)
                pbCalibration.visibility = View.INVISIBLE
                tvDesc.text          = "Đã lưu hướng lái chuẩn!"
                btnStart.text        = "Hoàn tất"
                btnStart.isEnabled   = true
                btnStart.setOnClickListener {
                    listener?.onCalibrationComplete()
                    dismissAllowingStateLoss()
                }
            }
        }.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countDownTimer?.cancel()
    }
}