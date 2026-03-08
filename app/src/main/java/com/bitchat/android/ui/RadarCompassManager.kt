package com.bitchat.android.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Publishes device heading as a StateFlow<Float> (degrees from true north, 0-360).
 * Uses TYPE_ROTATION_VECTOR for accuracy and applies a low-pass filter for smoothness.
 * If no compass sensor is available, heading stays at 0.
 */
class RadarCompassManager(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private val _heading = MutableStateFlow(0f)
    val heading: StateFlow<Float> = _heading.asStateFlow()

    private val orientationAngles = FloatArray(3)
    private val rotationMatrix = FloatArray(9)

    // Low-pass filter coefficient (0..1). Lower = smoother but slower response.
    private val alpha = 0.15f
    private var smoothedHeading: Float = 0f
    private var initialised = false

    fun start() {
        rotationSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return

        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
        SensorManager.getOrientation(rotationMatrix, orientationAngles)

        // azimuth in radians → degrees, normalise to 0-360
        var azimuthDeg = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
        if (azimuthDeg < 0) azimuthDeg += 360f

        if (!initialised) {
            smoothedHeading = azimuthDeg
            initialised = true
        } else {
            // Handle wrap-around (e.g. 359° → 1°) by using shortest angular path
            var delta = azimuthDeg - smoothedHeading
            if (delta > 180f) delta -= 360f
            if (delta < -180f) delta += 360f
            smoothedHeading += alpha * delta
            if (smoothedHeading < 0f) smoothedHeading += 360f
            if (smoothedHeading >= 360f) smoothedHeading -= 360f
        }

        _heading.value = smoothedHeading
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { /* no-op */ }
}
