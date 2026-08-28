// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Vudinhdat02

package com.example.dmsapplication.ml.analyzer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BaselineScoreCalculatorTest {
    @Test
    fun scoreEqualsOneAtEachThreshold() {
        val proposed = BaselineScoreCalculator.proposedScore(
            ear3D = 0.16f,
            earThreshold = 0.16f,
            mar3D = 0.38f,
            marThreshold = 0.38f,
            relativeHeadScore = 1f
        )
        val baseline2D = BaselineScoreCalculator.earMar2DScore(
            ear2D = 0.20f,
            earThreshold = 0.20f,
            mar2D = 0.38f,
            marThreshold = 0.38f
        )

        assertEquals(1f, proposed, 0.0001f)
        assertEquals(1f, baseline2D, 0.0001f)
    }

    @Test
    fun lowerEarProducesHigherAlertScore() {
        val openEyeScore = BaselineScoreCalculator.inverseThresholdScore(0.20f, 0.30f)
        val closedEyeScore = BaselineScoreCalculator.inverseThresholdScore(0.20f, 0.10f)

        assertTrue(openEyeScore < 1f)
        assertTrue(closedEyeScore > 1f)
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
}
