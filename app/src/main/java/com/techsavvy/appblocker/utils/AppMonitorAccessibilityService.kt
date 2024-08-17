package com.techsavvy.appblocker.utils

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import android.widget.Toast
import com.techsavvy.appblocker.BlockDialogActivity

class AppMonitorAccessibilityService : AccessibilityService() {

    private lateinit var sharedPreferences: SharedPreferences
    private val targetPackageName = "com.dts.freefiremax" // The app to block

    override fun onServiceConnected() {
        super.onServiceConnected()
        sharedPreferences = getSharedPreferences("AppBlockerPrefs", MODE_PRIVATE)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString()
        Log.d("AppMonitorService", "Game Detected: $packageName")

        if (packageName == targetPackageName && event.eventType == 23 ) {
            val isApproved = sharedPreferences.getBoolean("APPROVED", false)
            Log.d("AppMonitorService", "Game Detected: $packageName, Approved: $isApproved")
//            Toast.makeText(applicationContext, "Game Detected: $packageName", Toast.LENGTH_SHORT).show()

            if (!isApproved) {
                Log.d("AppMonitorService", "Launching Block Dialog for: $packageName")
                val dialogIntent = Intent(this, BlockDialogActivity::class.java)
                dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(dialogIntent)
            }
        }

        // Reset approval if the app moves to the background
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && packageName != targetPackageName) {
            sharedPreferences.edit().putBoolean("APPROVED", false).apply()
        }
    }

    override fun onInterrupt() {
        // Handle interruption
    }
}
