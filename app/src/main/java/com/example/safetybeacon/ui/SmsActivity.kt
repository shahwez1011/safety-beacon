package com.example.safetybeacon.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telephony.SmsManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.example.safetybeacon.R
import com.example.safetybeacon.utils.EmergencySosHelper
import com.example.safetybeacon.widget.EmergencySosWidget
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.io.IOException
import java.util.*

class SmsActivity : AppCompatActivity(), SensorEventListener {

    override fun onSensorChanged(event: SensorEvent?) {}
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private lateinit var inp_phNo1: TextView
    private lateinit var inp_phNo2: TextView
    private lateinit var inp_msg: TextView
    private lateinit var submit_btn: Button
    private lateinit var sos_btn: Button
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val sharedPrefsKey = "sms_prefs"
    private val permissionsRequestCode = 100

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sms)

        inp_phNo1 = findViewById(R.id.textInputPhNumber1)
        inp_phNo2 = findViewById(R.id.textInputPhNumber2)
        inp_msg = findViewById(R.id.textInputMessage)
        submit_btn = findViewById(R.id.submit_btn)
        sos_btn = findViewById(R.id.sos_btn)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        getSavedData()
        sos_btn.setOnClickListener {
            onSendClicked()
        }
        submit_btn.setOnClickListener {
            saveData()
            // Update all widgets after saving data
            EmergencySosWidget.updateAllWidgets(this)
        }
    }

    private fun getSavedData() {
        val sharedPreferences = getSharedPreferences(sharedPrefsKey, Context.MODE_PRIVATE)
        val phNo1 = sharedPreferences.getString("phNo1", "")
        val phNo2 = sharedPreferences.getString("phNo2", "")
        val msg = sharedPreferences.getString("msg", "I'm in danger please help")
        inp_phNo1.text = phNo1
        inp_phNo2.text = phNo2
        inp_msg.text = msg
    }

    private fun saveData() {
        val sharedPreferences = getSharedPreferences(sharedPrefsKey, Context.MODE_PRIVATE)
        sharedPreferences.edit {
            putString("phNo1", inp_phNo1.text.toString())
            putString("phNo2", inp_phNo2.text.toString())
            putString("msg", inp_msg.text.toString())
        }
        Toast.makeText(this, "Data saved successfully", Toast.LENGTH_SHORT).show()
    }

    private fun onSendClicked() {
        if (checkForRequestsAndPermissions()) {
            sendSmsWithLocation()
            makeCallToNumber()
        }
    }

    private fun checkForRequestsAndPermissions(): Boolean {
        val permissionsNeeded = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.SEND_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsNeeded.add(Manifest.permission.SEND_SMS)
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CALL_PHONE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsNeeded.add(Manifest.permission.CALL_PHONE)
        }

        return if (permissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsNeeded.toTypedArray(),
                permissionsRequestCode
            )
            false
        } else {
            true
        }
    }

    private fun sendSmsWithLocation() {
        val num1 = inp_phNo1.text.toString().trim()
        val num2 = inp_phNo2.text.toString().trim()
        val baseMessage = inp_msg.text.toString().trim()
        val recipients = listOf(num1, num2).filter { it.isNotEmpty() }

        if (recipients.isEmpty()) {
            Toast.makeText(this, "Please enter at least one phone number", Toast.LENGTH_SHORT).show()
            return
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permission not granted", Toast.LENGTH_SHORT).show()
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            var fullMessage = baseMessage
            if (location != null) {
                try {
                    val geocoder = Geocoder(this, Locale.getDefault())
                    val address = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                        ?.firstOrNull()
                    val locationInfo =
                        "Lat: ${location.latitude}, Long: ${location.longitude}\n${address?.getAddressLine(0) ?: ""}"
                    fullMessage += "\nMyLocation\n$locationInfo"
                } catch (e: IOException) {
                    Toast.makeText(this, "Error getting location", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Location Unavailable", Toast.LENGTH_SHORT).show()
            }

            val sms = SmsManager.getDefault()
            recipients.forEach { number ->
                val parts = sms.divideMessage(fullMessage)
                sms.sendMultipartTextMessage(number, null, parts, null, null)
                Toast.makeText(this, "Message sent to $number", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == permissionsRequestCode && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            onSendClicked()
        } else {
            Toast.makeText(this, "All permissions are required", Toast.LENGTH_LONG).show()
        }
    }

    private fun makeCallToNumber() {
        val number = inp_phNo1.text.toString().trim()
        if (number.isNotEmpty()) {
            val callIntent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$number")
            }
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CALL_PHONE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                startActivity(callIntent)
            } else {
                Toast.makeText(this, "Call permission not granted", Toast.LENGTH_SHORT).show()
            }
        }
    }
}