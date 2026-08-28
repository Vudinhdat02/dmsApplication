// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Vudinhdat02

package com.example.dmsapplication.ml.analyzer

import android.content.Context
import java.io.BufferedWriter
import java.io.Closeable
import java.io.File
import java.io.FileWriter
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Records matched per-frame outputs for offline participant-held-out evaluation.
 * The CSV deliberately contains no image, account identifier, or claimed ground-truth label.
 */
class BaselineComparisonRecorder(context: Context) : Closeable {
    companion object {
        private const val SAMPLE_INTERVAL_MS = 100L
        private const val RETENTION_MS = 7L * 24L * 60L * 60L * 1000L
    }

    private val outputDirectory =
        context.getExternalFilesDir("baseline-comparison")
            ?: File(context.filesDir, "baseline-comparison")
    private val sessionId = System.currentTimeMillis()
    val outputFile = File(outputDirectory, "baseline_comparison_$sessionId.csv")
    private val writerExecutor = Executors.newSingleThreadExecutor()
    private var writer: BufferedWriter? = null
    private var lastQueuedTimestampMs = 0L

    init {
        outputDirectory.mkdirs()
        deleteExpiredFiles()
        writer = runCatching {
            BufferedWriter(FileWriter(outputFile, false)).apply {
                write(
                    "session_id,timestamp_ms,ear_3d,ear_2d,mar_3d,mar_2d," +
                        "relative_yaw,relative_pitch,pnp_yaw_deg,pnp_pitch_deg," +
                        "eye_3d_score,eye_2d_score,mar_3d_score,mar_2d_score," +
                        "relative_head_score,proposed_score,ear_mar_2d_score,pnp_score," +
                        "proposed_latency_ms,ear_mar_2d_latency_ms,pnp_latency_ms," +
                        "proposed_threshold_exceeded,ear_mar_2d_threshold_exceeded," +
                        "pnp_threshold_exceeded"
                )
                newLine()
                flush()
            }
        }.getOrNull()
    }

    @Synchronized
    fun record(sample: BaselineComparisonSample) {
        if (sample.timestampMs - lastQueuedTimestampMs < SAMPLE_INTERVAL_MS) return
        lastQueuedTimestampMs = sample.timestampMs
        if (writer == null || writerExecutor.isShutdown) return
        runCatching { writerExecutor.execute { writeSample(sample) } }
    }

    private fun writeSample(sample: BaselineComparisonSample) {
        runCatching {
            writer?.apply {
                write(
                    listOf(
                        sessionId,
                        sample.timestampMs,
                        number(sample.ear3D),
                        number(sample.ear2D),
                        number(sample.mar3D),
                        number(sample.mar2D),
                        number(sample.relativeYaw),
                        number(sample.relativePitch),
                        number(sample.pnpYawDegrees),
                        number(sample.pnpPitchDegrees),
                        number(sample.eye3DScore),
                        number(sample.eye2DScore),
                        number(sample.mar3DScore),
                        number(sample.mar2DScore),
                        number(sample.relativeHeadScore),
                        number(sample.proposedScore),
                        number(sample.earMar2DScore),
                        number(sample.pnpScore),
                        number(sample.proposedLatencyMs),
                        number(sample.earMar2DLatencyMs),
                        number(sample.pnpLatencyMs),
                        sample.proposedScore >= 1f,
                        sample.earMar2DScore >= 1f,
                        sample.pnpScore?.let { it >= 1f } ?: ""
                    ).joinToString(",")
                )
                newLine()
                flush()
            }
        }
    }

    override fun close() {
        writerExecutor.shutdown()
        try {
            writerExecutor.awaitTermination(2, TimeUnit.SECONDS)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        runCatching { writer?.close() }
        writer = null
    }

    private fun number(value: Float?): String =
        value?.let { String.format(Locale.US, "%.6f", it) } ?: ""

    private fun number(value: Double?): String =
        value?.let { String.format(Locale.US, "%.6f", it) } ?: ""

    private fun deleteExpiredFiles() {
        val cutoff = System.currentTimeMillis() - RETENTION_MS
        outputDirectory.listFiles()
            ?.filter {
                it.isFile &&
                    it.name.startsWith("baseline_comparison_") &&
                    it.lastModified() < cutoff
            }
            ?.forEach { it.delete() }
    }
}
