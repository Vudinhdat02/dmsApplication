// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Vudinhdat02

package com.example.dmsapplication.ui.dashboardView

import android.animation.ValueAnimator
import androidx.core.content.ContextCompat
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
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
class DashboardFragment : Fragment() {
    private val viewModel: DashboardViewModel by viewModels {
        DashboardViewModelFactory(requireActivity().application)
    }
    private var typeWriteJob: Job? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val tvScore       = view.findViewById<TextView>(R.id.tvSafetyScore)
        val progressScore = view.findViewById<CircularProgressIndicator>(R.id.progressScore)
        val tvStatus      = view.findViewById<TextView>(R.id.tvScoreStatus)
        val tvAiInsight   = view.findViewById<TextView>(R.id.tvAiInsight)
        val tvTotalDrowsy = view.findViewById<TextView>(R.id.tvTotalDrowsy)
        val tvTotalHead   = view.findViewById<TextView>(R.id.tvTotalHead)
        val btnRequestAi  = view.findViewById<MaterialButton>(R.id.btnRequestAi)
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
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.safetyScore.collect { score ->
                val colorRes = when {
                    score >= 80 -> R.color.success
                    score >= 50 -> R.color.warning
                    else        -> R.color.danger
                }
                val parsedColor = ContextCompat.getColor(requireContext(), colorRes)
                tvStatus.text = when {
                    score >= 80 -> "Trạng thái: Rất Tốt"
                    score >= 50 -> "Trạng thái: Cần chú ý"
                    else        -> "Trạng thái: Nguy hiểm"
                }
                tvStatus.setTextColor(parsedColor)
                tvScore.setTextColor(parsedColor)
                progressScore.setIndicatorColor(parsedColor)
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
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.aiInsight.collect { insight ->
                typeWriteJob?.cancel()
                typeWriteJob = viewLifecycleOwner.lifecycleScope.launch {
                    tvAiInsight.typeWrite(insight)
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isAiLoading.collect { isLoading ->
                btnRequestAi.isEnabled = !isLoading
                btnRequestAi.text = if (isLoading) "Đang phân tích..." else "Phân tích và nhận gợi ý AI"
            }
        }
        btnRequestAi.setOnClickListener {
            viewModel.requestAiAnalysis()
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.totalDrowsy.collect { tvTotalDrowsy.text = it.toString() }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.totalHead.collect { tvTotalHead.text = it.toString() }
        }
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
        this.text = ""
        for (char in text) {
            this.append(char.toString())
            delay(delayMs)
        }
    }
    private fun updateNativeBarChart(barView: View, spaceView: View, targetPercentage: Float) {
        val safePercent = targetPercentage.coerceIn(0f, 100f)
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