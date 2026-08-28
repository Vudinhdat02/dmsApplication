// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Vudinhdat02

package com.example.dmsapplication.ml.analyzer

object BaselineScoreCalculator {
    fun proposedScore(
        ear3D: Float,
        earThreshold: Float,
        mar3D: Float,
        marThreshold: Float,
        relativeHeadScore: Float
    ): Float = maxOf(
        inverseThresholdScore(earThreshold, ear3D),
        ratioScore(mar3D, marThreshold),
        relativeHeadScore.coerceAtLeast(0f)
    )

    fun earMar2DScore(
        ear2D: Float,
        earThreshold: Float,
        mar2D: Float,
        marThreshold: Float
    ): Float = maxOf(
        inverseThresholdScore(earThreshold, ear2D),
        ratioScore(mar2D, marThreshold)
    )

    fun inverseThresholdScore(threshold: Float, value: Float): Float =
        if (threshold > 0f && value > 0.000001f) threshold / value else 0f

    private fun ratioScore(value: Float, threshold: Float): Float =
        if (threshold > 0f) (value / threshold).coerceAtLeast(0f) else 0f
}
