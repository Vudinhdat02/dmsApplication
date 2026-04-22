package com.example.dmsapplication.ui.dashboardView

import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.dmsapplication.R
import com.google.android.material.progressindicator.CircularProgressIndicator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DashboardFragment : Fragment() {

    private val viewModel: DashboardViewModel by viewModels {
        DashboardViewModelFactory(requireActivity().application)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvScore = view.findViewById<TextView>(R.id.tvSafetyScore)
        val progressScore = view.findViewById<CircularProgressIndicator>(R.id.progressScore)
        val tvStatus = view.findViewById<TextView>(R.id.tvScoreStatus)
        val tvAiInsight = view.findViewById<TextView>(R.id.tvAiInsight)
        val tvTotalDrowsy = view.findViewById<TextView>(R.id.tvTotalDrowsy)
        val tvTotalHead = view.findViewById<TextView>(R.id.tvTotalHead)

        val bars = listOf(
            view.findViewById<View>(R.id.barT2), view.findViewById<View>(R.id.barT3),
            view.findViewById<View>(R.id.barT4), view.findViewById<View>(R.id.barT5),
            view.findViewById<View>(R.id.barT6), view.findViewById<View>(R.id.barT7),
            view.findViewById<View>(R.id.barCN)
        )
        val spaces = listOf(
            view.findViewById<View>(R.id.spaceT2), view.findViewById<View>(R.id.spaceT3),
            view.findViewById<View>(R.id.spaceT4), view.findViewById<View>(R.id.spaceT5),
            view.findViewById<View>(R.id.spaceT6), view.findViewById<View>(R.id.spaceT7),
            view.findViewById<View>(R.id.spaceCN)
        )

        // 1. ANIMATION VÒNG TRÒN ĐIỂM SỐ
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.safetyScore.collect { score ->

                // Quyết định màu sắc dựa trên điểm
                val colorString = when {
                    score >= 80 -> "#4CAF50"
                    score >= 50 -> "#FF9800"
                    else -> "#EF4444"
                }
                val parsedColor = Color.parseColor(colorString)

                // Cập nhật chữ trạng thái
                tvStatus.text = when {
                    score >= 80 -> "Trạng thái: Rất Tốt"
                    score >= 50 -> "Trạng thái: Cần chú ý"
                    else -> "Trạng thái: Nguy hiểm"
                }
                tvStatus.setTextColor(parsedColor)
                tvScore.setTextColor(parsedColor)
                progressScore.setIndicatorColor(parsedColor)

                // Hiệu ứng chạy từ 0 -> Điểm thực tế trong 1.5 giây
                val animator = ValueAnimator.ofInt(0, score)
                animator.duration = 1500
                animator.addUpdateListener { animation ->
                    val value = animation.animatedValue as Int
                    tvScore.text = value.toString()
                    progressScore.progress = value
                }
                animator.start()
            }
        }

        // 2. ANIMATION GÕ CHỮ CHO TRỢ LÝ AI
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.aiInsight.collect { insight ->
                // Gọi hàm mở rộng typeWrite để tạo hiệu ứng
                tvAiInsight.typeWrite(insight)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.totalDrowsy.collect { tvTotalDrowsy.text = it.toString() }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.totalHead.collect { tvTotalHead.text = it.toString() }
        }

        // 3. XỬ LÝ DỮ LIỆU BIỂU ĐỒ CỘT
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.weeklyData.collect { weekData ->
                val maxError = weekData.maxOrNull() ?: 1f
                val scaleFactor = if (maxError > 0) 100f / maxError else 0f

                for (i in weekData.indices) {
                    val percentage = weekData[i] * scaleFactor
                    updateNativeBarChart(bars[i], spaces[i], percentage)
                }
            }
        }
    }

    private suspend fun TextView.typeWrite(text: String, delayMs: Long = 15) {
        this.text = "" // Xóa sạch chữ "Đang tải dữ liệu..." cũ
        for (char in text) {
            this.append(char.toString())
            delay(delayMs) // Nghỉ 15ms trước khi gõ chữ tiếp theo
        }
    }

    private fun updateNativeBarChart(barView: View, spaceView: View, targetPercentage: Float) {
        val safePercent = targetPercentage.coerceIn(0f, 100f)

        // Nếu điểm bằng 0 thì không cần chạy animation
        if (safePercent == 0f) {
            barView.post {
                val spaceParams = spaceView.layoutParams as LinearLayout.LayoutParams
                spaceParams.weight = 100f
                spaceView.layoutParams = spaceParams

                val barParams = barView.layoutParams as LinearLayout.LayoutParams
                barParams.weight = 0f
                barView.layoutParams = barParams
            }
            return
        }

        // Chạy hiệu ứng mọc lên từ 0 -> phần trăm mục tiêu trong 1 giây
        val animator = ValueAnimator.ofFloat(0f, safePercent)
        animator.duration = 1000
        animator.addUpdateListener { animation ->
            val currentVal = animation.animatedValue as Float

            barView.post {
                val spaceParams = spaceView.layoutParams as LinearLayout.LayoutParams
                spaceParams.weight = 100f - currentVal
                spaceView.layoutParams = spaceParams

                val barParams = barView.layoutParams as LinearLayout.LayoutParams
                barParams.weight = currentVal
                barView.layoutParams = barParams
            }
        }
        animator.start()
    }
}