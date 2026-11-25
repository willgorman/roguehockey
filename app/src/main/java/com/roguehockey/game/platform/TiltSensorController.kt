package com.roguehockey.game.platform

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.roguehockey.game.engine.Vec2
import kotlin.math.abs

class TiltSensorController(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val gravitySensor: Sensor? =
        sensorManager?.getDefaultSensor(Sensor.TYPE_GRAVITY)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    val isAvailable: Boolean = gravitySensor != null

    @Volatile
    private var rawX: Float = 0f

    @Volatile
    private var rawY: Float = 0f

    @Volatile
    private var zeroX: Float = 0f

    @Volatile
    private var zeroY: Float = 0f

    @Volatile
    private var filteredX: Float = 0f

    @Volatile
    private var filteredY: Float = 0f

    private var registered = false

    fun start() {
        if (registered || gravitySensor == null) return
        sensorManager?.registerListener(this, gravitySensor, SensorManager.SENSOR_DELAY_GAME)
        registered = true
    }

    fun stop() {
        if (!registered) return
        sensorManager?.unregisterListener(this)
        registered = false
    }

    /**
     * Sets the tablet's current tilt angle as neutral (0, 0), so players can play flat
     * on a table or angled comfortably in their hands.
     */
    fun calibrateNeutral() {
        zeroX = rawX
        zeroY = rawY
        filteredX = 0f
        filteredY = 0f
    }

    fun currentTilt(): Vec2 = Vec2(filteredX, filteredY)

    override fun onSensorChanged(event: SensorEvent) {
        // In portrait orientation:
        // event.values[0] > 0 when tilted left (left edge down) -> so right tilt is -values[0]
        // event.values[1] > 0 when tilted upright (bottom edge down) -> pushing puck down (+Y)
        val gx = (-event.values[0] / 6.2f).coerceIn(-1.5f, 1.5f)
        val gy = (event.values[1] / 6.2f).coerceIn(-1.5f, 1.5f)

        rawX = gx
        rawY = gy

        val adjX = applyDeadzone((gx - zeroX).coerceIn(-1f, 1f))
        val adjY = applyDeadzone((gy - zeroY).coerceIn(-1f, 1f))

        val alpha = 0.22f
        filteredX += (adjX - filteredX) * alpha
        filteredY += (adjY - filteredY) * alpha
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun applyDeadzone(v: Float): Float {
        val dz = 0.06f
        return if (abs(v) < dz) 0f else ((abs(v) - dz) / (1f - dz)) * if (v > 0f) 1f else -1f
    }
}
