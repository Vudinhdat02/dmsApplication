package com.example.dmsapplication.ml.analyzer

import android.os.Debug
import android.os.SystemClock
import android.util.Log
import java.util.Locale
import kotlin.math.ceil

internal class DmsBenchmarkRecorder {
    private companion object {
        const val TAG = "DMS_BENCH"
        const val WARMUP_NS = 30_000_000_000L
        const val MEASUREMENT_NS = 60_000_000_000L
    }
    private data class Summary(val mean: Double, val median: Double, val p95: Double)

    private val createdNs = SystemClock.elapsedRealtimeNanos()
    private var measurementStartNs = 0L
    private var finished = false
    private val preprocessingMs = ArrayList<Double>(2_000)
    private val inferenceMs = ArrayList<Double>(2_000)
    private val postprocessingMs = ArrayList<Double>(2_000)
    private val totalMs = ArrayList<Double>(2_000)
    private var faceFrames = 0
    private var noFaceFrames = 0
    fun record(
        preprocessingLatencyMs: Double,
        inferenceLatencyMs: Double,
        postprocessingLatencyMs: Double,
        totalLatencyMs: Double,
        faceDetected: Boolean,
    ) {
        if (finished) return
        val nowNs = SystemClock.elapsedRealtimeNanos()

        if (measurementStartNs == 0L) {
            if (nowNs - createdNs < WARMUP_NS) return
            measurementStartNs = nowNs
            Log.i(TAG, "WARMUP_COMPLETE measurement_window_s=60")
            return
        }
        preprocessingMs.add(preprocessingLatencyMs)
        inferenceMs.add(inferenceLatencyMs)
        postprocessingMs.add(postprocessingLatencyMs)
        totalMs.add(totalLatencyMs)
        if (faceDetected) faceFrames++ else noFaceFrames++
        val elapsedNs = nowNs - measurementStartNs
        if (elapsedNs < MEASUREMENT_NS) return
        val durationSec = elapsedNs / 1_000_000_000.0
        val frameCount = totalMs.size
        if (frameCount == 0) {
            Log.e(TAG, "RESULT_ERROR no_processed_frames")
            finished = true
            return
        }
        val processedFps = frameCount / durationSec
        val pssMb = Debug.getPss() / 1024.0
        val faceFramePercent = 100.0 * faceFrames / frameCount
        Log.i(
            TAG,
            String.format(
                Locale.US,
                "RESULT_RUN duration_s=%.3f frames=%d processed_fps=%.3f " +
                    "face_frames=%d no_face_frames=%d face_frame_percent=%.2f pss_mb=%.2f",
                durationSec, frameCount, processedFps, faceFrames, noFaceFrames,
                faceFramePercent, pssMb,
            ),
        )
        logSummary("PREPROCESSING", summarize(preprocessingMs))
        logSummary("INFERENCE", summarize(inferenceMs))
        logSummary("POSTPROCESSING", summarize(postprocessingMs))
        logSummary("TOTAL", summarize(totalMs))
        finished = true
    }
    private fun summarize(values: List<Double>): Summary {
        val sorted = values.sorted()
        val count = sorted.size
        val median = if (count % 2 == 0) {
            (sorted[count / 2 - 1] + sorted[count / 2]) / 2.0
        } else {
            sorted[count / 2]
        }
        val p95Index = (ceil(count * 0.95).toInt() - 1).coerceIn(0, count - 1)
        return Summary(sorted.average(), median, sorted[p95Index])
    }
    private fun logSummary(stage: String, summary: Summary) {
        Log.i(
            TAG,
            String.format(
                Locale.US,
                "RESULT_%s mean_ms=%.3f median_ms=%.3f p95_ms=%.3f",
                stage, summary.mean, summary.median, summary.p95,
            ),
        )
    }
}
