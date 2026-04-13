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
        postInvalidate() // Dùng post để an toàn khi gọi từ luồng nền
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val result = results ?: return

        // Chúng ta lưu trạng thái canvas trước khi xoay
        canvas.save()

        // 1. Xoay Canvas 270 độ quanh tâm để khớp với màn hình dọc (Portrait)
        canvas.rotate(270f, width / 2f, height / 2f)

        // 2. Tính toán tỉ lệ Scale
        val scaleX = width.toFloat() / height.toFloat()
        val scaleY = height.toFloat() / width.toFloat()

        // 3. FIX LỖI NGƯỢC CHIỀU (MIRROR EFFECT)
        // Áp dụng -1f vào scaleX (lúc này đang là trục ngang của màn hình) để lật trái/phải
        canvas.scale(scaleY, scaleX * -1f, width / 2f, height / 2f)

        for (landmark in result.faceLandmarks()) {
            for (connection in FaceLandmarker.FACE_LANDMARKS_TESSELATION) {
                val start = landmark[connection.start()]
                val end = landmark[connection.end()]

                // Vẽ theo tọa độ chuẩn (0..1) nhân với kích thước view
                canvas.drawLine(
                    start.x() * width, start.y() * height,
                    end.x() * width, end.y() * height,
                    linePaint
                )
            }
        }

        // Khôi phục lại trạng thái Canvas ban đầu
        canvas.restore()
    }
}