package com.jonpas.sensexp

import android.Manifest
import android.app.Activity
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
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.io.File
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
    private var experimentNameField: EditText? = null

    private var fileName: String = ""

    // Audio
    private var recorder: MediaRecorder? = null

    // Sensor
    private lateinit var sensorManager: SensorManager
    private lateinit var linearAccel: Sensor
    private var accDelay: Int = SensorManager.SENSOR_DELAY_FASTEST
    private var samplesFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Capture
        ActivityCompat.requestPermissions(this, permissions, PERMISSIONS_REQUEST_CODE)
        val captureSwitch: Switch = findViewById(R.id.capture)
        captureSwitch.setOnCheckedChangeListener(captureSwitchOnCheckedChangeListener)
        firePrompt = findViewById(R.id.firePrompt)
        firePrompt?.visibility = View.INVISIBLE
        experimentNameField = findViewById(R.id.name)

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
        // Hide keyboard
        val inputManager: InputMethodManager =
            getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.hideSoftInputFromWindow(
            currentFocus!!.windowToken,
            InputMethodManager.HIDE_NOT_ALWAYS
        )

        // Create base file name
        val timestamp = (System.currentTimeMillis() / 1000).toString()
        val experimentName = experimentNameField?.text ?: ""
        fileName = "${getExternalFilesDir(null)}/${timestamp}_${experimentName}"

        // Start gathering data
        startSensorAcquisition()
        startRecording()

        // Prompt
        firePrompt?.visibility = View.VISIBLE
        firePrompt?.text = getString(R.string.fire_in)
    }

    private fun stopCapture() {
        if (firePrompt?.visibility != View.INVISIBLE) {
            // Stop gathering data
            stopSensorAcquisition()
            stopRecording()

            // Prompt
            firePrompt?.visibility = View.INVISIBLE
        }
    }

    // Audio
    private fun startRecording() {
        val filePath = "${fileName}_audio.3gp"
        Log.i(LOG_TAG, "Writing audio to: $filePath")

        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setOutputFile(filePath)
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
    private fun startSensorAcquisition() {
        val filePath = "${fileName}_samples.csv"
        Log.i(LOG_TAG, "Writing sensor data to: $filePath}")

        samplesFile = File(filePath)
        samplesFile!!.createNewFile()

        sensorManager.registerListener(this, linearAccel, accDelay)
    }

    private fun stopSensorAcquisition() {
        sensorManager.unregisterListener(this)

        Toast.makeText(this, "Experiment saved!", Toast.LENGTH_SHORT).show()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_LINEAR_ACCELERATION) {
            val values = event.values
            val x = values[0]
            val y = values[1]
            val z = values[2]

            //Log.v(LOG_TAG, "x = $x, y = $y, z = $z")
            samplesFile?.appendText("${event.timestamp.toString()} $x $y $z \n", Charsets.UTF_8)
        }
    }
}
