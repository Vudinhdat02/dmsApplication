package com.example.dmsapplication.utils

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import kotlin.math.sqrt

class CrashDetector(context: Context, private val onCrashDetected: () -> Unit) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var lastCrashTimeMs = 0L
    private var consecutiveHighSamples = 0
    private var lastHighSampleTimeNs = 0L

    private companion object {
        const val CRASH_THRESHOLD_G = 4.5f
        const val REQUIRED_CONSECUTIVE_SAMPLES = 2
        const val MAX_SAMPLE_GAP_NS = 150_000_000L
        const val CRASH_COOLDOWN_MS = 30_000L
    }

    fun startListening() {
        consecutiveHighSamples = 0
        lastHighSampleTimeNs = 0L
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
        consecutiveHighSamples = 0
        lastHighSampleTimeNs = 0L
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return

        val gX = event.values[0] / SensorManager.GRAVITY_EARTH
        val gY = event.values[1] / SensorManager.GRAVITY_EARTH
        val gZ = event.values[2] / SensorManager.GRAVITY_EARTH
        val gForce = sqrt((gX * gX + gY * gY + gZ * gZ).toDouble()).toFloat()

        if (gForce >= CRASH_THRESHOLD_G) {
            consecutiveHighSamples = if (
                lastHighSampleTimeNs > 0L &&
                event.timestamp - lastHighSampleTimeNs <= MAX_SAMPLE_GAP_NS
            ) {
                consecutiveHighSamples + 1
            } else {
                1
            }
            lastHighSampleTimeNs = event.timestamp

            if (consecutiveHighSamples >= REQUIRED_CONSECUTIVE_SAMPLES) {
                consecutiveHighSamples = 0
                val currentTimeMs = SystemClock.elapsedRealtime()
                if (lastCrashTimeMs == 0L || currentTimeMs - lastCrashTimeMs >= CRASH_COOLDOWN_MS) {
                    lastCrashTimeMs = currentTimeMs
                    onCrashDetected()
                }
            }
        } else {
            consecutiveHighSamples = 0
            lastHighSampleTimeNs = 0L
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
