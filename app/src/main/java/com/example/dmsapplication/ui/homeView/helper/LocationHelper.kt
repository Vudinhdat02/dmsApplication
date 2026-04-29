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
    private val fusedLocationClient =
        LocationServices.getFusedLocationProviderClient(context)
    var currentLocation: Location? = null
    private var lastLocation: Location? = null

    private val speedHistory = ArrayDeque<Float>(3)

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { location ->
                currentLocation = location

                var speedKmh = 0f

                if (location.hasSpeed() && location.speed > 0f) {
                    speedKmh = location.speed * 3.6f
                } else if (lastLocation != null) {
                    val distanceMeters = lastLocation!!.distanceTo(location)
                    val timeDeltaSec = (location.elapsedRealtimeNanos - lastLocation!!.elapsedRealtimeNanos) / 1_000_000_000f
                    if (timeDeltaSec > 0 && distanceMeters > 0) {
                        speedKmh = (distanceMeters / timeDeltaSec) * 3.6f
                    }
                }

                val lastSpeed = speedHistory.lastOrNull() ?: 0f
                if (speedKmh - lastSpeed > 50f && lastSpeed < 10f) {
                    speedKmh = lastSpeed // Giữ tốc độ cũ, bỏ giá trị nhiễu
                }

                if (speedKmh < 3f) speedKmh = 0f

                if (speedHistory.size >= 3) speedHistory.removeFirst()
                speedHistory.addLast(speedKmh)
                val smoothedSpeed = speedHistory.average().toFloat()

                lastLocation = location

                val status = when {
                    smoothedSpeed >= 5f -> "Trạng thái: Đang lái"
                    else                -> "Trạng thái: Đứng yên"
                }
                onSpeedUpdate(smoothedSpeed, status)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startTracking() {
        // ✅ Lấy lastLocation ngay để không phải chờ GPS cold start
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
        fusedLocationClient.removeLocationUpdates(locationCallback)
        lastLocation = null
        speedHistory.clear()
    }
}