package com.example.safetybeacon.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.example.safetybeacon.service.EmergencySosService
import com.example.safetybeacon.ui.SmsActivity

object EmergencySosHelper {

    @RequiresApi(Build.VERSION_CODES.O)
    fun triggerEmergencySos(context: Context) {
        if (hasAllPermissions(context)) {
            // Start the emergency SOS service
            val sosIntent = Intent(context, EmergencySosService::class.java)
            context.startForegroundService(sosIntent)
        } else {
            // If permissions are not granted, open the main activity
            Toast.makeText(context, "Please grant permissions in the app first", Toast.LENGTH_LONG).show()
            val intent = Intent(context, SmsActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    private fun hasAllPermissions(context: Context): Boolean {
        val requiredPermissions = arrayOf(
            Manifest.permission.SEND_SMS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CALL_PHONE
        )

        return requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun hasEmergencyContactsConfigured(context: Context): Boolean {
        val sharedPreferences = context.getSharedPreferences("sms_prefs", Context.MODE_PRIVATE)
        val phNo1 = sharedPreferences.getString("phNo1", "")
        val phNo2 = sharedPreferences.getString("phNo2", "")
        return !phNo1.isNullOrEmpty() || !phNo2.isNullOrEmpty()
    }
}