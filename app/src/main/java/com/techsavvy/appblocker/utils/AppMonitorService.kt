package com.techsavvy.appblocker.utils

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.techsavvy.appblocker.BlockDialogActivity
import com.techsavvy.appblocker.R
import android.app.usage.UsageStatsManager
import android.app.usage.UsageEvents
import android.content.SharedPreferences

class AppMonitorService : Service() {

    private lateinit var handler: Handler
    private lateinit var runnable: Runnable
    private val targetPackageName = "com.whatsapp" // The app to block
    private lateinit var sharedPreferences: SharedPreferences

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        sharedPreferences = getSharedPreferences("AppBlockerPrefs", Context.MODE_PRIVATE)
        handler = Handler(Looper.getMainLooper())
        runnable = Runnable { monitorApp() }
        handler.post(runnable)
        startForeground(1, createNotification())

        return START_STICKY
    }

    private fun monitorApp() {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val beginTime = endTime - 1000 // Check last 2 seconds

        val usageEvents = usageStatsManager.queryEvents(beginTime, endTime)
        var latestEvent: UsageEvents.Event? = null
        Log.d("Service", "monitorApp: $usageStatsManager")

        while (usageEvents.hasNextEvent()) {
            val event = UsageEvents.Event()
            usageEvents.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                latestEvent = event
            }
        }

        val isApproved = sharedPreferences.getBoolean("APPROVED", false)

        if (latestEvent?.packageName == targetPackageName && !isApproved) {
            Log.d("AppMonitorService", "Blocked app detected: ${latestEvent.packageName}")
            // Launch the dialog activity when the target app is detected
            val dialogIntent = Intent(this, BlockDialogActivity::class.java)
            dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(dialogIntent)
        }

        // Re-run the monitor periodically
        handler.postDelayed(runnable, 1000) // Check every second
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
