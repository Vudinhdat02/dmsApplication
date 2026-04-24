package com.example.dmsapplication.ui.homeView

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dmsapplication.data.repository.AlertRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    private val _isDrowsy = MutableStateFlow(false)
    val isDrowsy = _isDrowsy.asStateFlow()
    private val _drowsyCount = MutableStateFlow(0)
    val drowsyCount = _drowsyCount.asStateFlow()

    private val _isHeadDistracted = MutableStateFlow(false)
    val isHeadDistracted = _isHeadDistracted.asStateFlow()
    private val _headDistractedCount = MutableStateFlow(0)
    val headDistractedCount = _headDistractedCount.asStateFlow()

    // BIẾN MỚI: Ngáp ngủ
    private val _isYawning = MutableStateFlow(false)
    val isYawning = _isYawning.asStateFlow()
    private val _yawnCount = MutableStateFlow(0)
    val yawnCount = _yawnCount.asStateFlow()

    // Tín hiệu bắn ra Fragment để phát âm thanh nghỉ ngơi
    private val _suggestRest = MutableSharedFlow<Boolean>()
    val suggestRest = _suggestRest.asSharedFlow()

    private val _speed = MutableStateFlow("Tốc độ: 0.0 km/h")
    val speed = _speed.asStateFlow()
    private val _locationStatus = MutableStateFlow("Trạng thái: Đang chờ")
    val locationStatus = _locationStatus.asStateFlow()
    private val _isGpsEnabled = MutableStateFlow(true)
    val isGpsEnabled = _isGpsEnabled.asStateFlow()

    private val _isMonitoringEnabled = MutableStateFlow(false)
    val isMonitoringEnabled = _isMonitoringEnabled.asStateFlow()

    private val _isCalibrated = MutableStateFlow(false)
    val isCalibrated = _isCalibrated.asStateFlow()

    private val _isSunglassesMode = MutableStateFlow(false)
    val isSunglassesMode = _isSunglassesMode.asStateFlow()

    private val _isYawnMode = MutableStateFlow(true)
    val isYawnMode = _isYawnMode.asStateFlow()

    private val _alertMessage = MutableSharedFlow<String>()
    val alertMessage = _alertMessage.asSharedFlow()

    private val alertRepository = AlertRepository()

    private val _isCameraPreviewEnabled = MutableStateFlow(true)
    val isCameraPreviewEnabled = _isCameraPreviewEnabled.asStateFlow()

    fun setCameraPreviewEnabled(enabled: Boolean) {
        _isCameraPreviewEnabled.value = enabled
    }

    fun onDmsResult(isDrowsy: Boolean, isHeadDistracted: Boolean, isYawning: Boolean) {
        if (!_isMonitoringEnabled.value) {
            if (_isDrowsy.value) _isDrowsy.value = false
            if (_isHeadDistracted.value) _isHeadDistracted.value = false
            if (_isYawning.value) _isYawning.value = false
            return
        }

        // Xử lý nhắm mắt
        val wasDrowsy = _isDrowsy.value
        if (isDrowsy != wasDrowsy) {
            _isDrowsy.value = isDrowsy
            if (isDrowsy) _drowsyCount.value += 1
        }

        // Xử lý quay đầu
        val wasDistracted = _isHeadDistracted.value
        if (isHeadDistracted != wasDistracted) {
            _isHeadDistracted.value = isHeadDistracted
            if (isHeadDistracted) _headDistractedCount.value += 1
        }

        // Xử lý ngáp ngủ
        val wasYawning = _isYawning.value
        if (isYawning != wasYawning) {
            _isYawning.value = isYawning
            if (isYawning) {
                _yawnCount.value += 1

                if (_yawnCount.value > 0 && _yawnCount.value % 3 == 0) {
                    viewModelScope.launch {
                        _suggestRest.emit(true)
                    }
                }
            }
        }
    }

    fun updateLocation(speed: Float, status: String) {
        _speed.value = "Tốc độ: ${"%.1f".format(speed)} km/h"
        _locationStatus.value = status

        if (_isGpsEnabled.value) {
            val shouldMonitor = speed >= 5f
            if (_isMonitoringEnabled.value != shouldMonitor) {
                _isMonitoringEnabled.value = shouldMonitor
                if (!shouldMonitor) {
                    _isDrowsy.value = false
                    _isHeadDistracted.value = false
                    _isYawning.value = false
                }
            }
        }
    }

    fun setGpsEnabled(enabled: Boolean) {
        _isGpsEnabled.value = enabled
        if (!enabled) {
            _isMonitoringEnabled.value = true
            _speed.value = "GPS đã tắt"
            _locationStatus.value = "Giám sát không cần GPS"
        } else {
            _isMonitoringEnabled.value = false
            _locationStatus.value = "Trạng thái: Đang chờ GPS..."
        }
    }

    fun setSunglassesMode(enabled: Boolean) {
        _isSunglassesMode.value = enabled
        if (enabled && _isDrowsy.value) {
            _isDrowsy.value = false
        }
    }

    fun setYawnMode(enabled: Boolean) {
        _isYawnMode.value = enabled
    }

    fun triggerCrashAlert(latitude: Double, longitude: Double) {
        val mapLink = "https://www.google.com/maps/search/?api=1&query=$latitude,$longitude"

        viewModelScope.launch(Dispatchers.IO) {
            val isSuccess = alertRepository.sendCrashAlert(mapLink)

            if (isSuccess) {
                _alertMessage.emit("Đã gửi Email cảnh báo thành công!")
            } else {
                _alertMessage.emit("Gửi cảnh báo thất bại. Vui lòng kiểm tra mạng hoặc danh bạ!")
            }
        }
    }

    fun setCalibrated(value: Boolean)          { _isCalibrated.value = value }

    fun resetStats() {
        _drowsyCount.value         = 0
        _headDistractedCount.value = 0
        _yawnCount.value           = 0
        _isDrowsy.value            = false
        _isHeadDistracted.value    = false
        _isYawning.value           = false
    }
}
