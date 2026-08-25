package com.example.dmsapplication.ui.homeView.helper

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
class OverlayView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private var results: FaceLandmarkerResult? = null
    private val linePaint = Paint().apply {
        color = Color.CYAN
        strokeWidth = 2f
        style = Paint.Style.STROKE
        alpha = 200
    }
    fun setResults(faceLandmarkerResult: FaceLandmarkerResult?) {
        results = faceLandmarkerResult
        postInvalidate()
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val result = results ?: return
        canvas.save()
        canvas.rotate(270f, width / 2f, height / 2f)
        val scaleX = width.toFloat() / height.toFloat()
        val scaleY = height.toFloat() / width.toFloat()
        canvas.scale(scaleY, scaleX * -1f, width / 2f, height / 2f)
        for (landmark in result.faceLandmarks()) {
            for (connection in FaceLandmarker.FACE_LANDMARKS_TESSELATION) {
                val start = landmark[connection.start()]
                val end = landmark[connection.end()]
                canvas.drawLine(
                    start.x() * width, start.y() * height,
                    end.x() * width, end.y() * height,
                    linePaint
                )
            }
        }
        canvas.restore()
    }
}