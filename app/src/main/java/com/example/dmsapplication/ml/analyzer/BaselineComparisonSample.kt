// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Vudinhdat02

package com.example.dmsapplication.ml.analyzer

data class BaselineComparisonSample(
    val timestampMs: Long,
    val ear3D: Float,
    val ear2D: Float,
    val mar3D: Float,
    val mar2D: Float,
    val relativeYaw: Float?,
    val relativePitch: Float?,
    val pnpYawDegrees: Float?,
    val pnpPitchDegrees: Float?,
    val eye3DScore: Float,
    val eye2DScore: Float,
    val mar3DScore: Float,
    val mar2DScore: Float,
    val relativeHeadScore: Float,
    val proposedScore: Float,
    val earMar2DScore: Float,
    val pnpScore: Float?,
    val proposedLatencyMs: Double,
    val earMar2DLatencyMs: Double,
    val pnpLatencyMs: Double?
)
