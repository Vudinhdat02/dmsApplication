// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Vudinhdat02

package com.example.dmsapplication.ui.homeView.helper

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.dmsapplication.R
import com.example.dmsapplication.ml.analyzer.BaselineComparisonSample

/**
 * Rolling visualization of threshold-normalized scores. It is a UI aid, not an accuracy chart.
 * A score of 1.0 means the corresponding method has reached its alert threshold.
 */
class BaselineComparisonChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    companion object {
        private const val MAX_SAMPLES = 150
        private const val MAX_SCORE = 2f
        private const val UI_INTERVAL_MS = 100L
    }

    private data class ChartPoint(
        val proposed: Float,
        val earMar2D: Float,
        val pnp: Float?
    )

    private val points = ArrayDeque<ChartPoint>(MAX_SAMPLES)
    private var lastUiUpdateMs = 0L
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = resources.displayMetrics.density * 2f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.divider)
        style = Paint.Style.STROKE
        strokeWidth = resources.displayMetrics.density
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.text_muted)
        textSize = resources.displayMetrics.scaledDensity * 10f
    }

    fun addSample(sample: BaselineComparisonSample) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastUiUpdateMs < UI_INTERVAL_MS) return
        lastUiUpdateMs = now

        if (points.size >= MAX_SAMPLES) points.removeFirst()
        points.addLast(
            ChartPoint(
                proposed = sample.proposedScore.coerceIn(0f, MAX_SCORE),
                earMar2D = sample.earMar2DScore.coerceIn(0f, MAX_SCORE),
                pnp = sample.pnpScore?.coerceIn(0f, MAX_SCORE)
            )
        )
        invalidate()
    }

    fun clear() {
        points.clear()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val left = paddingLeft + resources.displayMetrics.density * 28f
        val right = width - paddingRight.toFloat()
        val top = paddingTop.toFloat()
        val bottom = height - paddingBottom.toFloat() - resources.displayMetrics.density * 12f
        if (right <= left || bottom <= top) return

        drawGuide(canvas, left, right, top, bottom, 0f)
        drawGuide(canvas, left, right, top, bottom, 1f)
        drawGuide(canvas, left, right, top, bottom, 2f)

        if (points.size < 2) {
            canvas.drawText("Waiting for data", left + 8f, (top + bottom) / 2f, labelPaint)
            return
        }

        val snapshot = points.toList()
        drawSeries(canvas, snapshot.map { it.proposed }, left, right, top, bottom, R.color.brand_primary)
        drawSeries(canvas, snapshot.map { it.earMar2D }, left, right, top, bottom, R.color.warning)
        drawSeries(canvas, snapshot.map { it.pnp }, left, right, top, bottom, R.color.accent_purple)
    }

    private fun drawGuide(
        canvas: Canvas,
        left: Float,
        right: Float,
        top: Float,
        bottom: Float,
        value: Float
    ) {
        val y = valueToY(value, top, bottom)
        canvas.drawLine(left, y, right, y, gridPaint)
        canvas.drawText(String.format(java.util.Locale.US, "%.0f", value), paddingLeft.toFloat(), y + 4f, labelPaint)
    }

    private fun drawSeries(
        canvas: Canvas,
        values: List<Float?>,
        left: Float,
        right: Float,
        top: Float,
        bottom: Float,
        colorRes: Int
    ) {
        linePaint.color = ContextCompat.getColor(context, colorRes)
        val step = (right - left) / (MAX_SAMPLES - 1).coerceAtLeast(1)
        val startIndex = MAX_SAMPLES - values.size
        var previousX: Float? = null
        var previousY: Float? = null

        values.forEachIndexed { index, value ->
            if (value == null) {
                previousX = null
                previousY = null
                return@forEachIndexed
            }
            val x = left + (startIndex + index) * step
            val y = valueToY(value, top, bottom)
            if (previousX != null && previousY != null) {
                canvas.drawLine(previousX!!, previousY!!, x, y, linePaint)
            }
            previousX = x
            previousY = y
        }
    }

    private fun valueToY(value: Float, top: Float, bottom: Float): Float =
        bottom - (value.coerceIn(0f, MAX_SCORE) / MAX_SCORE) * (bottom - top)
}
