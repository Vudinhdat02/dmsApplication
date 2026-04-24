package com.example.dmsapplication.ui.homeView.helper

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.*

class LocationHelper(
    context: Context,
    private val onSpeedUpdate: (Float, String) -> Unit
) {
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    var currentLocation: Location? = null
    private var lastLocation: Location? = null

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return

            if (location.accuracy > 50f) return

            var speedKmh = 0f

            if (location.hasSpeed() && location.speed >= 0f) {
                speedKmh = location.speed * 3.6f
            } else if (lastLocation != null) {
                val distance = lastLocation!!.distanceTo(location)
                val timeDelta = (location.time - lastLocation!!.time) / 1000f
                if (timeDelta > 0 && distance > 2f) {
                    speedKmh = (distance / timeDelta) * 3.6f
                }
            }

            if (speedKmh < 3.0f) speedKmh = 0f

            if (speedKmh > 200f) speedKmh = 0f

            currentLocation = location
            lastLocation = location

            val status = when {
                speedKmh >= 5f -> "Trạng thái: Đang lái"
                else           -> "Trạng thái: Đứng yên"
            }
            onSpeedUpdate(speedKmh, status)
        }
    }

    @SuppressLint("MissingPermission")
    fun startTracking() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setMinUpdateIntervalMillis(500L)
            .setMaxUpdateDelayMillis(2000L)
            .setWaitForAccurateLocation(false)
            .build()

        fusedLocationClient.lastLocation.addOnSuccessListener { lastKnown ->
            lastKnown?.let {
                currentLocation = it
                lastLocation = it
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    fun stopTracking() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        lastLocation = null
    }
}