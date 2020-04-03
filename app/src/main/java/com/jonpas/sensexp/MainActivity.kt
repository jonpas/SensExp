package com.jonpas.sensexp

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.CompoundButton
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private lateinit var linearAccel: Sensor
    private var accDelay: Int = SensorManager.SENSOR_DELAY_FASTEST

    companion object {
        private var TAG: String = "linearAccel"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val captureSwitch: Switch = findViewById(R.id.capture)
        captureSwitch.setOnCheckedChangeListener(captureSwitchOnCheckedChangeListener)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        linearAccel = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_LINEAR_ACCELERATION) {
            processAccelerometer(event)
        }
    }

    private fun processAccelerometer(event: SensorEvent) {
        val values = event.values

        val x = values[0]
        val y = values[1]
        val z = values[2]
        Log.v(TAG, "x = $x, y = $y, z = $z")
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    private val captureSwitchOnCheckedChangeListener = CompoundButton.OnCheckedChangeListener { compoundButton: CompoundButton, state: Boolean ->
        if (state) {
            sensorManager.registerListener(this, linearAccel, accDelay)
        } else {
            sensorManager.unregisterListener(this)
        }
    }
}
