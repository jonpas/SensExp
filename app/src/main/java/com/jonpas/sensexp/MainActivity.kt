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
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.io.File
import java.io.IOException
import kotlin.random.Random


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
    private lateinit var firePrompt: TextView
    private lateinit var captureSwitch: Switch
    private lateinit var experimentNameField: EditText

    private var fileName: String = ""
    private var capturing: Boolean = false

    // Audio
    private lateinit var recorder: MediaRecorder

    // Sensor
    private lateinit var sensorManager: SensorManager
    private lateinit var linearAccel: Sensor
    private var accDelay: Int = SensorManager.SENSOR_DELAY_FASTEST
    private var samplesFile: File? = null
    private var startTimestamp: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Capture
        ActivityCompat.requestPermissions(this, permissions, PERMISSIONS_REQUEST_CODE)
        captureSwitch = findViewById(R.id.capture)
        captureSwitch.setOnCheckedChangeListener(captureSwitchOnCheckedChangeListener)
        firePrompt = findViewById(R.id.firePrompt)
        firePrompt.visibility = View.INVISIBLE
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

    override fun onDestroy() {
        super.onDestroy()
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
        val experimentName = experimentNameField.text
        fileName = "${getExternalFilesDir(null)}/${timestamp}_${experimentName}"

        // Start gathering data
        startSensorAcquisition()
        startRecording()
        capturing = true

        // Random repeating prompt
        startCountdown(2, 5, 3)
    }

    private fun stopCapture() {
        if (capturing) {
            // Stop gathering data
            stopSensorAcquisition()
            stopRecording()
            capturing = false
            startTimestamp = 0

            // Prompt
            firePrompt.visibility = View.INVISIBLE
            Toast.makeText(this, "Experiment saved!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startCountdown(randMin: Int, randMax: Int, pause: Int) {
        // Get random countdown time
        val random = Random.nextInt((randMax - randMin) + 1) + randMin
        Log.v(LOG_TAG, "Countdown: $random seconds")

        // Start countdown
        object: CountDownTimer(random.toLong() * 1000, 100) {
            override fun onTick(millisUntilFinished: Long) {
                // Cancel if stopped mid-countdown
                if (!capturing) {
                    cancel()
                }

                // Display prompts
                if (millisUntilFinished <= 1000) {
                    firePrompt.text = getString(R.string.fire_now)
                } else {
                    firePrompt.text = getString(R.string.fire_in, millisUntilFinished / 1000)
                }
            }

            override fun onFinish() {
                firePrompt.visibility = View.INVISIBLE

                // Restart countdown after given pause
                restartCountdown(randMin, randMax, pause)
            }
        }.start()

        firePrompt.visibility = View.VISIBLE
    }

    private fun restartCountdown(randMin: Int, randMax: Int, pause: Int) {
        object: CountDownTimer(pause.toLong() * 1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                // Cancel if stopped mid-countdown
                if (!capturing) {
                    cancel()
                }
            }

            override fun onFinish() {
                startCountdown(randMin, randMax, pause)
            }
        }.start()
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
        recorder.apply {
            stop()
            release()
        }
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
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_LINEAR_ACCELERATION) {
            val values = event.values
            val x = values[0]
            val y = values[1]
            val z = values[2]

            if (startTimestamp == 0L) {
                startTimestamp = event.timestamp
            }

            val timestampDiff = (event.timestamp - startTimestamp) / 1000000
            val fireNowPromptVisible = firePrompt.text == getString(R.string.fire_now)
            samplesFile?.appendText("$timestampDiff $x $y $z ${fireNowPromptVisible}\n", Charsets.UTF_8)
        }
    }
}
