package com.example.safetybeacon.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.safetybeacon.R
import kotlin.math.sqrt
import java.math.BigDecimal
import java.math.RoundingMode

class MagnetometerActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var magnetometer: Sensor? = null
    private lateinit var mediaPlayer: MediaPlayer

    private lateinit var magValueText: TextView
    private lateinit var xText: TextView
    private lateinit var yText: TextView
    private lateinit var zText: TextView
    private lateinit var conditionText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_magnetometer)
        //setContentView(R.layout.fragment_spy)

        // Bind views
        magValueText = findViewById(R.id.value)
        xText = findViewById(R.id.x_cor)
        yText = findViewById(R.id.y_cor)
        zText = findViewById(R.id.z_cor)
        conditionText = findViewById(R.id.show_conditions)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        if (magnetometer == null) {
            magValueText.text = "Magnetometer not supported"
        }
    }

    override fun onResume() {
        super.onResume()
        magnetometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_MAGNETIC_FIELD) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            val mag = sqrt(x * x + y * y + z * z)

            val formatted = mag.toBigDecimal().setScale(0, RoundingMode.HALF_UP).toDouble()
            magValueText.text = "$formatted μT"

            xText.text = x.toBigDecimal().setScale(0, RoundingMode.HALF_UP).toString()
            yText.text = y.toBigDecimal().setScale(0, RoundingMode.HALF_UP).toString()
            zText.text = z.toBigDecimal().setScale(0, RoundingMode.HALF_UP).toString()

            when {
                mag in 65.0..90.0 -> {
                    playBeep(R.raw.beep)
                    conditionText.text = "Electronic devices are within range"
                }
                mag > 90 -> {
                    playBeep(R.raw.beepd)
                    conditionText.text = "Device detected"
                }
                else -> {
                    conditionText.text = "No device is detected"
                }
            }
        }
    }

    private fun playBeep(resId: Int) {
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.release()
        }
        mediaPlayer = MediaPlayer.create(this, resId)
        mediaPlayer.start()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
