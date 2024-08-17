package com.techsavvy.appblocker.utils

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import com.techsavvy.appblocker.BlockDialogActivity
import com.techsavvy.appblocker.R
import android.app.usage.UsageStatsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND
import android.app.usage.UsageEvents.Event.MOVE_TO_BACKGROUND
import android.widget.Toast

class AppMonitorService : Service() {

    private lateinit var handler: Handler
    private lateinit var runnable: Runnable
    private val targetPackageName = "com.dts.freefiremax" // The app to block
    private lateinit var sharedPreferences: SharedPreferences


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        sharedPreferences = getSharedPreferences("AppBlockerPrefs", Context.MODE_PRIVATE)
        handler = Handler(Looper.getMainLooper())
        runnable = Runnable { monitorApp() }
        startForeground(1, createNotification())

        // Start the AccessibilityService
        startAccessibilityService()

        // Start monitoring immediately (optional if using AccessibilityService)
        handler.post(runnable)

        return START_STICKY
    }

    private fun startAccessibilityService() {
        val intent = Intent(this, AppMonitorAccessibilityService::class.java)
        startService(intent)
    }



/*
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        sharedPreferences = getSharedPreferences("AppBlockerPrefs", Context.MODE_PRIVATE)
        handler = Handler(Looper.getMainLooper())
        startForeground(1, createNotification())
        runnable = Runnable { monitorApp() }

        // Start monitoring immediately
        handler.post(runnable)

        // Ensure usage access permission
        if (!hasUsageStatsPermission()) {
            val usageIntent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            usageIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(usageIntent)
        }

        return START_STICKY
    }
*/

    private fun monitorApp() {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val beginTime = endTime - 1000 // Check last 1 second

        val usageEvents = usageStatsManager.queryEvents(beginTime, endTime)
        var latestEvent: UsageEvents.Event? = null

        while (usageEvents.hasNextEvent()) {
            val event = UsageEvents.Event()
            usageEvents.getNextEvent(event)
            Log.d("Service", "Event: ${event.packageName}, EventType: ${event.eventType}")

            if(event?.packageName == targetPackageName) {
                latestEvent = event
            }

            if(latestEvent?.packageName == targetPackageName && latestEvent?.eventType == 2){
                sharedPreferences.edit().putBoolean("APPROVED", false).apply()
            }



        }
        if (latestEvent?.packageName == targetPackageName) {
            val isApproved = sharedPreferences.getBoolean("APPROVED", false)
            Log.d("AppMonitorService", "App Detected: ${latestEvent.packageName}, Approved: $isApproved")
//            Toast.makeText(applicationContext, latestEvent.packageName, Toast.LENGTH_SHORT).show()

            if (!isApproved) {
                Log.d("AppMonitorService", "Launching Block Dialog for: ${latestEvent.packageName}")
                val dialogIntent = Intent(this, BlockDialogActivity::class.java)
                dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(dialogIntent)
            }
        }
        // Re-run the monitor periodically
        handler.postDelayed({ monitorApp() }, 1000) // Check every second
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), packageName
            )
        } else {
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun createNotification(): Notification {
        val notificationChannelId = "PARENTAL_CONTROL_SERVICE_CHANNEL"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                notificationChannelId,
                "Parental Control Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(notificationChannel)
        }

        return NotificationCompat.Builder(this, notificationChannelId)
            .setContentTitle("Parental Control Service")
            .setContentText("Monitoring app launches.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setAutoCancel(false)
            .build()
    }

    override fun onDestroy() {
        handler.removeCallbacks(runnable)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}


