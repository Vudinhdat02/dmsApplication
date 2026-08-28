// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Vudinhdat02

package com.example.dmsapplication.ml.analyzer

import kotlin.math.abs

object BaselineScoreCalculator {
    fun eyeScore(ear: Float, threshold: Float): Float =
        if (threshold > 0f && ear > 0.000001f) threshold / ear else 0f

    fun mouthScore(mar: Float, threshold: Float): Float =
        if (threshold > 0f) (mar / threshold).coerceAtLeast(0f) else 0f

    fun proposedScore(
        ear3D: Float,
        earThreshold: Float,
        mar3D: Float,
        marThreshold: Float,
        relativeHeadScore: Float
    ): Float = maxOf(
        eyeScore(ear3D, earThreshold),
        mouthScore(mar3D, marThreshold),
        relativeHeadScore.coerceAtLeast(0f)
    )

    fun earMar2DScore(
        ear2D: Float,
        earThreshold: Float,
        mar2D: Float,
        marThreshold: Float
    ): Float = maxOf(
        eyeScore(ear2D, earThreshold),
        mouthScore(mar2D, marThreshold)
    )

    fun normalizedAngleDifferenceDegrees(current: Float, baseline: Float): Float {
        val wrapped = ((current - baseline + 180f) % 360f + 360f) % 360f - 180f
        return abs(wrapped)
    }
}
