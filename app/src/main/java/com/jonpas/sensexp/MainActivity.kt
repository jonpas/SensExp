package com.jonpas.sensexp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.CompoundButton
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.io.IOException


class MainActivity : AppCompatActivity(), SensorEventListener {

    companion object {
        private const val LOG_TAG: String = "SensExp"
        private const val PERMISSIONS_REQUEST_CODE = 333
    }

    // Capture (wraps audio, sensor, UI and permissions)
    private var permissions: Array<String> = arrayOf(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.RECORD_AUDIO
    )
    private var permissionsAccepted = false
    private var firePrompt: TextView? = null

    // Audio
    private var recorder: MediaRecorder? = null
    private var fileName: String = ""

    // Sensor
    private lateinit var sensorManager: SensorManager
    private lateinit var linearAccel: Sensor
    private var accDelay: Int = SensorManager.SENSOR_DELAY_FASTEST

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Capture
        ActivityCompat.requestPermissions(this, permissions, PERMISSIONS_REQUEST_CODE)
        val captureSwitch: Switch = findViewById(R.id.capture)
        captureSwitch.setOnCheckedChangeListener(captureSwitchOnCheckedChangeListener)
        firePrompt = findViewById(R.id.firePrompt)
        firePrompt?.visibility = View.INVISIBLE

        // Sensor
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        linearAccel = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
    }

    override fun onStop() {
        super.onStop()
        stopCapture()
    }

    override fun onPause() {
        super.onPause()
        stopCapture()
    }

    // Capture
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                finish()
            }
        }
    }

    private val captureSwitchOnCheckedChangeListener =
        CompoundButton.OnCheckedChangeListener { _: CompoundButton, state: Boolean ->
            if (state) {
                startCapture()
            } else {
                stopCapture()
            }
        }

    private fun startCapture() {
        // Sensor
        sensorManager.registerListener(this, linearAccel, accDelay)

        // Audio
        startRecording()

        // Prompt
        firePrompt?.visibility = View.VISIBLE
        firePrompt?.text = getString(R.string.fire_in)
    }

    private fun stopCapture() {
        // Sensor
        sensorManager.unregisterListener(this)

        // Audio
        stopRecording()

        // Prompt
        firePrompt?.visibility = View.INVISIBLE
    }

    // Audio
    private fun startRecording() {
        var timestamp = "timestamp"
        fileName = "${getExternalFilesDir("SensExp")}/${timestamp}_audio.3gp"
        Log.i(LOG_TAG, "Writing to: $fileName")

        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setOutputFile(fileName)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)

            try {
                prepare()
            } catch (e: IOException) {
                Log.e(LOG_TAG, "prepare() failed")
            }

            start()
        }
    }

    private fun stopRecording() {
        recorder?.apply {
            stop()
            release()
        }
        recorder = null
    }

    // Sensor
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_LINEAR_ACCELERATION) {
            processLinearAccel(event)
        }
    }

    private fun processLinearAccel(event: SensorEvent) {
        val values = event.values
        val x = values[0]
        val y = values[1]
        val z = values[2]
        //Log.v(LOG_TAG, "x = $x, y = $y, z = $z")
    }
}
