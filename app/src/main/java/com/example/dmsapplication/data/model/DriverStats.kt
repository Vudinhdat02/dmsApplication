// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Vudinhdat02

package com.example.dmsapplication.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "driver_stats")
data class DriverStats(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String = "",
    val timestamp: Long = 0L,
    val drowsyCount: Int = 0,
    val headDistractedCount: Int = 0,
    val speed: Float = 0f,
    val localImagePath: String = "",
    val cloudImageUrl: String = "",
    val isSynced: Boolean = false
)
