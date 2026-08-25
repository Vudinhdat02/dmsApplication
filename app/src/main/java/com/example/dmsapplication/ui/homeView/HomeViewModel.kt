package com.example.dmsapplication.ui.homeView

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dmsapplication.data.model.DriverStats
import com.example.dmsapplication.data.repository.AlertRepository
import com.example.dmsapplication.data.repository.StatsRepository
import com.example.dmsapplication.ml.analyzer.DmsAnalyzer
import com.example.dmsapplication.worker.SyncWorker
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = StatsRepository(application)
    private val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private val _isDrowsy = MutableStateFlow(false)
    val isDrowsy = _isDrowsy.asStateFlow()
    private val _drowsyCount = MutableStateFlow(0)
    val drowsyCount = _drowsyCount.asStateFlow()
    private val _isHeadDistracted = MutableStateFlow(false)
    val isHeadDistracted = _isHeadDistracted.asStateFlow()
    private val _headDistractedCount = MutableStateFlow(0)
    val headDistractedCount = _headDistractedCount.asStateFlow()
    private val _isYawning = MutableStateFlow(false)
    val isYawning = _isYawning.asStateFlow()
    private val _yawnCount = MutableStateFlow(0)
    val yawnCount = _yawnCount.asStateFlow()
    private val _suggestRest = MutableSharedFlow<Boolean>()
    val suggestRest = _suggestRest.asSharedFlow()
    private val _speedKmh = MutableStateFlow(0f)
    val speedKmh = _speedKmh.asStateFlow()
    private val _speed = MutableStateFlow(formatSpeed(0f))
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
    private val _earThreshold = MutableStateFlow(DmsAnalyzer.DEFAULT_EAR_THRESHOLD)
    val earThreshold = _earThreshold.asStateFlow()
    private val _alertMessage = MutableSharedFlow<String>()
    val alertMessage = _alertMessage.asSharedFlow()
    private val alertRepository = AlertRepository()
    private val _isCameraPreviewEnabled = MutableStateFlow(true)
    val isCameraPreviewEnabled = _isCameraPreviewEnabled.asStateFlow()
    private companion object {
        const val START_MONITORING_SPEED_KMH = 5f
        const val STOP_MONITORING_SPEED_KMH = 3f
    }
    init {
        loadInitialStats()
    }
    private fun loadInitialStats() {
        viewModelScope.launch(Dispatchers.IO) {
            val startOfToday = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            try {
                val list = repository.getRecentStatsByUser(userId).first()
                val todayRecords = list.filter { it.timestamp >= startOfToday }

                _drowsyCount.value = todayRecords.sumOf { it.drowsyCount }
                _headDistractedCount.value = todayRecords.sumOf { it.headDistractedCount }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
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
        val wasDrowsy = _isDrowsy.value
        if (isDrowsy != wasDrowsy) {
            _isDrowsy.value = isDrowsy
            if (isDrowsy) _drowsyCount.value += 1
        }
        val wasDistracted = _isHeadDistracted.value
        if (isHeadDistracted != wasDistracted) {
            _isHeadDistracted.value = isHeadDistracted
            if (isHeadDistracted) _headDistractedCount.value += 1
        }
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
        updateSpeed(speed)
        if (_isGpsEnabled.value) {
            val shouldMonitor = if (_isMonitoringEnabled.value) {
                speed >= STOP_MONITORING_SPEED_KMH
            } else {
                speed >= START_MONITORING_SPEED_KMH
            }
            if (_isMonitoringEnabled.value != shouldMonitor) {
                _isMonitoringEnabled.value = shouldMonitor
                if (!shouldMonitor) {
                    _isDrowsy.value = false
                    _isHeadDistracted.value = false
                    _isYawning.value = false
                }
            }
            _locationStatus.value = when {
                status.contains("GPS chưa ổn định") -> status
                shouldMonitor -> "Trạng thái: Đang lái"
                else -> "Trạng thái: Đứng yên"
            }
        }
    }
    fun setGpsEnabled(enabled: Boolean) {
        _isGpsEnabled.value = enabled
        if (!enabled) {
            _isMonitoringEnabled.value = true
            updateSpeed(0f)
            _locationStatus.value = "Giám sát không cần GPS"
        } else {
            _isMonitoringEnabled.value = false
            updateSpeed(0f)
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
    fun setEarThreshold(threshold: Float) {
        _earThreshold.value = threshold.coerceIn(
            DmsAnalyzer.MIN_EAR_THRESHOLD,
            DmsAnalyzer.MAX_EAR_THRESHOLD
        )
    }
    fun triggerCrashAlert(latitude: Double, longitude: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val isSuccess = alertRepository.sendCrashAlert(latitude, longitude)
            if (isSuccess) {
                _alertMessage.emit("Đã gửi Email cảnh báo thành công!")
            } else {
                _alertMessage.emit("Gửi cảnh báo thất bại. Vui lòng kiểm tra mạng hoặc danh bạ!")
            }
        }
    }
    fun setCalibrated(value: Boolean) { _isCalibrated.value = value }
    fun resetStats() {
        _drowsyCount.value         = 0
        _headDistractedCount.value = 0
        _yawnCount.value           = 0
        _isDrowsy.value            = false
        _isHeadDistracted.value    = false
        _isYawning.value           = false
    }
    fun saveViolationRecord(bitmap: Bitmap) {
        if (userId.isEmpty()) return
        val currentDrowsyCount = if (_isDrowsy.value) 1 else 0
        val currentHeadDistractedCount = if (_isHeadDistracted.value) 1 else 0
        val currentSpeed = _speedKmh.value
        viewModelScope.launch(Dispatchers.IO) {
            val fileName = "alert_${System.currentTimeMillis()}"
            val imagePath = repository.saveImageLocally(bitmap, fileName)
            val stats = DriverStats(
                userId = userId,
                timestamp = System.currentTimeMillis(),
                drowsyCount = currentDrowsyCount,
                headDistractedCount = currentHeadDistractedCount,
                speed = currentSpeed,
                localImagePath = imagePath,
                isSynced = false
            )
            repository.saveStats(stats)
            SyncWorker.scheduleImmediate(getApplication())
        }
    }
    private fun updateSpeed(speed: Float) {
        val safeSpeed = speed.coerceAtLeast(0f)
        _speedKmh.value = safeSpeed
        _speed.value = formatSpeed(safeSpeed)
    }
    private fun formatSpeed(speed: Float): String {
        return "Tốc độ: ${String.format(Locale.US, "%.1f", speed)} km/h"
    }
}
