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

    // BIẾN ĐƯỢC THÊM VÀO ĐỂ LƯU LẠI VỊ TRÍ HIỆN TẠI
    var currentLocation: Location? = null

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { location ->
                // Cập nhật vị trí mới nhất vào biến để HomeFragment lấy được khi có va chạm
                currentLocation = location

                // Kiểm tra GPS có tính được tốc độ không (hasSpeed = false khi tín hiệu yếu)
                val speedKmh = if (location.hasSpeed()) location.speed * 3.6f else 0f
                val status = when {
                    speedKmh >= 5f  -> "Trạng thái: Đang lái (${speedKmh.toInt()} km/h)"
                    speedKmh > 0f   -> "Trạng thái: Chậm (${speedKmh.toInt()} km/h)"
                    else            -> "Trạng thái: Đứng yên"
                }
                onSpeedUpdate(speedKmh, status)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startTracking() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 1000L  //1s thay vì 2s để cập nhật nhanh hơn
        ).setMinUpdateIntervalMillis(500L).build()
        fusedLocationClient.requestLocationUpdates(
            locationRequest, locationCallback, Looper.getMainLooper()
        )
    }

    fun stopTracking() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}