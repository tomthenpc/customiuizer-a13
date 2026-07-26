package name.monwf.customiuizer.mods.utils

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import name.monwf.customiuizer.mods.GlobalActions

class ShakeManager(private val helperContext: Context) : SensorEventListener {

    private var xAccel: Float = 0f
    private var yAccel: Float = 0f
    private var zAccel: Float = 0f

    private var xPreviousAccel: Float = 0f
    private var yPreviousAccel: Float = 0f
    private var zPreviousAccel: Float = 0f

    private var firstUpdate = true
    private var shakeInitiated = false
    private var lastShakeEvent = System.currentTimeMillis()

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onSensorChanged(se: SensorEvent) {
        updateAccelParameters(se.values[0], se.values[1], se.values[2])
        val changed = isAccelerationChanged()
        when {
            !shakeInitiated && changed -> shakeInitiated = true
            shakeInitiated && changed -> executeShakeActionDelayed()
            shakeInitiated && !changed -> shakeInitiated = false
        }
    }

    fun reset() {
        xAccel = 0f
        yAccel = 0f
        zAccel = 0f
        xPreviousAccel = 0f
        yPreviousAccel = 0f
        zPreviousAccel = 0f
        firstUpdate = true
        shakeInitiated = false
    }

    private fun updateAccelParameters(xNewAccel: Float, yNewAccel: Float, zNewAccel: Float) {
        if (firstUpdate) {
            xPreviousAccel = xNewAccel
            yPreviousAccel = yNewAccel
            zPreviousAccel = zNewAccel
            firstUpdate = false
        } else {
            xPreviousAccel = xAccel
            yPreviousAccel = yAccel
            zPreviousAccel = zAccel
        }
        xAccel = xNewAccel
        yAccel = yNewAccel
        zAccel = zNewAccel
    }

    private fun isAccelerationChanged(): Boolean {
        val deltaX = kotlin.math.abs(xPreviousAccel - xAccel)
        val deltaY = kotlin.math.abs(yPreviousAccel - yAccel)
        val deltaZ = kotlin.math.abs(zPreviousAccel - zAccel)
        return (deltaX > SHAKE_THRESHOLD_XY && deltaY > SHAKE_THRESHOLD_XY)
            || (deltaX > SHAKE_THRESHOLD_XY && deltaZ > SHAKE_THRESHOLD_Z)
            || (deltaY > SHAKE_THRESHOLD_XY && deltaZ > SHAKE_THRESHOLD_Z)
    }

    private fun executeShakeActionDelayed() {
        val now = System.currentTimeMillis()
        if (now - lastShakeEvent > SHAKE_EVENT_THROTTLE) {
            lastShakeEvent = now
            executeShakeAction()
        }
    }

    private fun executeShakeAction() {
        GlobalActions.handleAction(helperContext, "pref_key_launcher_shake")
    }

    companion object {
        private const val SHAKE_THRESHOLD_XY = 4f
        private const val SHAKE_THRESHOLD_Z = 8f
        private const val SHAKE_EVENT_THROTTLE = 750L
    }
}
