package com.example.safetybeacon.service

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.telephony.SmsManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.safetybeacon.R
import com.example.safetybeacon.ui.SmsActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.io.IOException
import java.util.*

class EmergencySosService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val sharedPrefsKey = "sms_prefs"
    private val NOTIFICATION_ID = 1001
    private val CHANNEL_ID = "emergency_sos_channel"

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Create a notification for the foreground service
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        // Execute SOS functionality
        executeSosEmergency()

        // Stop the service after execution
        stopSelf()

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Emergency SOS",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Emergency SOS notifications"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, SmsActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Emergency SOS Activated")
            .setContentText("Sending emergency messages...")
            .setSmallIcon(R.drawable.ic_sos)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }

    private fun executeSosEmergency() {
        if (checkPermissions()) {
            sendSmsWithLocation()
            makeCallToNumber()
        } else {
            Toast.makeText(this, "Emergency SOS requires permissions. Please open the app to grant permissions.", Toast.LENGTH_LONG).show()
            // Open the main activity to request permissions
            val intent = Intent(this, SmsActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
        }
    }

    private fun checkPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
    }

    private fun getSavedData(): Triple<String, String, String> {
        val sharedPreferences = getSharedPreferences(sharedPrefsKey, Context.MODE_PRIVATE)
        val phNo1 = sharedPreferences.getString("phNo1", "") ?: ""
        val phNo2 = sharedPreferences.getString("phNo2", "") ?: ""
        val msg = sharedPreferences.getString("msg", "I'm in danger please help") ?: "I'm in danger please help"
        return Triple(phNo1, phNo2, msg)
    }

    private fun sendSmsWithLocation() {
        val (num1, num2, baseMessage) = getSavedData()
        val recipients = listOf(num1, num2).filter { it.isNotEmpty() }

        if (recipients.isEmpty()) {
            Toast.makeText(this, "No emergency contacts configured", Toast.LENGTH_SHORT).show()
            return
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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
                    // Location couldn't be converted to address
                }
            }

            val sms = SmsManager.getDefault()
            recipients.forEach { number ->
                try {
                    val parts = sms.divideMessage(fullMessage)
                    sms.sendMultipartTextMessage(number, null, parts, null, null)
                } catch (e: Exception) {
                    // Handle SMS sending error
                }
            }

            Toast.makeText(this, "Emergency messages sent", Toast.LENGTH_SHORT).show()
        }
    }

    private fun makeCallToNumber() {
        val (num1, _, _) = getSavedData()
        if (num1.isNotEmpty()) {
            val callIntent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$num1")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                startActivity(callIntent)
            }
        }
    }
}