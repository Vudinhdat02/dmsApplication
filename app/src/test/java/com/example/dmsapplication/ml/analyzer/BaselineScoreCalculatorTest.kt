// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Vudinhdat02

package com.example.dmsapplication.ml.analyzer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BaselineScoreCalculatorTest {
    @Test
    fun scoreEqualsOneAtEachThreshold() {
        assertEquals(1f, BaselineScoreCalculator.eyeScore(0.16f, 0.16f), 0.0001f)
        assertEquals(1f, BaselineScoreCalculator.mouthScore(0.38f, 0.38f), 0.0001f)
    }

    @Test
    fun lowerEarProducesHigherAlertScore() {
        val openEyeScore = BaselineScoreCalculator.eyeScore(0.30f, 0.20f)
        val closedEyeScore = BaselineScoreCalculator.eyeScore(0.10f, 0.20f)

        assertTrue(openEyeScore < 1f)
        assertTrue(closedEyeScore > 1f)
    }

    @Test
    fun largerMarProducesHigherAlertScore() {
        val closedMouthScore = BaselineScoreCalculator.mouthScore(0.20f, 0.38f)
        val openMouthScore = BaselineScoreCalculator.mouthScore(0.60f, 0.38f)

        assertTrue(closedMouthScore < 1f)
        assertTrue(openMouthScore > 1f)
    }

    @Test
    fun strongestProposedSignalControlsAggregateScore() {
        val score = BaselineScoreCalculator.proposedScore(
            ear3D = 0.25f,
            earThreshold = 0.16f,
            mar3D = 0.10f,
            marThreshold = 0.38f,
            relativeHeadScore = 1.4f
        )

        assertEquals(1.4f, score, 0.0001f)
    }

    @Test
    fun angleDifferenceWrapsAcrossMinusAndPlus180Degrees() {
        val difference = BaselineScoreCalculator.normalizedAngleDifferenceDegrees(
            current = -179f,
            baseline = 179f
        )

        assertEquals(2f, difference, 0.0001f)
    }

    @Test
    fun angleDifferenceUsesShortestRegularPath() {
        val difference = BaselineScoreCalculator.normalizedAngleDifferenceDegrees(
            current = 35f,
            baseline = 15f
        )

        assertEquals(20f, difference, 0.0001f)
    }
}
