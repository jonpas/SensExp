package com.jonpas.sensexp

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
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

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        linearAccel = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        sensorManager.registerListener(this, linearAccel, accDelay)
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
        Log.d(TAG, "x= $x y= $y z= $z")
    }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(this, linearAccel, accDelay)
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }
}
