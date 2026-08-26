// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Vudinhdat02

package com.example.dmsapplication.ui.homeView.helper

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class LocationHelper(
    private val context: Context,
    private val onSpeedUpdate: (Float, String) -> Unit
) {
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    var currentLocation: Location? = null
    private var lastLocation: Location? = null
    private var isTracking = false
    private val speedHistory = ArrayDeque<Float>(SPEED_HISTORY_SIZE)
    private companion object {
        const val MAX_ACCEPTED_ACCURACY_METERS = 50f
        const val MAX_ACCEPTED_SPEED_ACCURACY_MPS = 2.5f
        const val MIN_MOVING_SPEED_KMH = 3f
        const val DRIVING_SPEED_KMH = 5f
        const val MAX_LOW_SPEED_SPIKE_KMH = 50f
        const val SPEED_HISTORY_SIZE = 5
    }
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { location ->
                currentLocation = location
                if (location.hasAccuracy() && location.accuracy > MAX_ACCEPTED_ACCURACY_METERS) {
                    onSpeedUpdate(speedHistory.averageOrZero(), "Trạng thái: GPS chưa ổn định")
                    return@let
                }
                var speedKmh = calculateSpeedKmh(location)
                val lastSpeed = speedHistory.lastOrNull() ?: 0f
                if (speedKmh - lastSpeed > MAX_LOW_SPEED_SPIKE_KMH && lastSpeed < 10f) { speedKmh = lastSpeed }
                if (speedKmh < MIN_MOVING_SPEED_KMH) speedKmh = 0f
                if (speedHistory.size >= SPEED_HISTORY_SIZE) speedHistory.removeFirst()
                speedHistory.addLast(speedKmh)
                val smoothedSpeed = speedHistory.average().toFloat()
                lastLocation = location
                val status = when {
                    smoothedSpeed >= DRIVING_SPEED_KMH -> "Trạng thái: Đang lái"
                    else -> "Trạng thái: Đứng yên"
                }
                onSpeedUpdate(smoothedSpeed, status)
            }
        }
    }
    private fun calculateSpeedKmh(location: Location): Float {
        val hasReliableGpsSpeed = location.hasSpeed() &&
            location.speed > 0f &&
            (!location.hasSpeedAccuracy() ||
                location.speedAccuracyMetersPerSecond <= MAX_ACCEPTED_SPEED_ACCURACY_MPS)
        if (hasReliableGpsSpeed) {
            return location.speed * 3.6f
        }
        val previousLocation = lastLocation ?: return 0f
        val distanceMeters = previousLocation.distanceTo(location)
        val timeDeltaSec =
            (location.elapsedRealtimeNanos - previousLocation.elapsedRealtimeNanos) / 1_000_000_000f
        return if (timeDeltaSec > 0f && distanceMeters > 0f) {
            (distanceMeters / timeDeltaSec) * 3.6f
        } else {
            0f
        }
    }
    @SuppressLint("MissingPermission")
    fun startTracking() {
        if (isTracking || !hasLocationPermission()) return
        isTracking = true
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                currentLocation = it
                lastLocation = it
            }
        }
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 1000L
        )
            .setMinUpdateIntervalMillis(500L)
            .setWaitForAccurateLocation(false)
            .build()
        fusedLocationClient.requestLocationUpdates(
            locationRequest, locationCallback, Looper.getMainLooper()
        )
    }
    fun stopTracking() {
        if (!isTracking) return
        isTracking = false
        fusedLocationClient.removeLocationUpdates(locationCallback)
        lastLocation = null
        speedHistory.clear()
    }
    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
    }
    private fun ArrayDeque<Float>.averageOrZero(): Float {
        return if (isEmpty()) 0f else average().toFloat()
    }
}
