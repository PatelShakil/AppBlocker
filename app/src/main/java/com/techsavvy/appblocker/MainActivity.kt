package com.techsavvy.appblocker

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.techsavvy.appblocker.utils.AppMonitorService
import android.app.usage.UsageStatsManager
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {

    companion object {
        const val REQUEST_CODE_USAGE_ACCESS = 1
        const val REQUEST_CODE_OVERLAY_PERMISSION = 2
        const val REQUEST_CODE_NOTIFICATION_PERMISSION = 3
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
       val  sharedPreferences = getSharedPreferences("AppBlockerPrefs", Context.MODE_PRIVATE)

        setContent {
            MaterialTheme {
                Column (
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ){
                    Text("Parental Control App For game")
                    val currentStatus = if(sharedPreferences.getBoolean("APPROVED",false)) "Approved" else "Not Approved"
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Current Status: $currentStatus")
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                            onClick={
                                sharedPreferences.edit().putBoolean("APPROVED", false).apply()
                            }
                            ){
                        Text("Reset Session")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            checkAndRequestPermissions()
                        }
                    ){
                        Text("Start Monitoring")
                    }
                }

            }
        }

        checkAndRequestPermissions()
    }

    private fun checkAndRequestPermissions() {
        when {
            !hasUsageStatsPermission() -> requestUsageStatsPermission()
            !hasOverlayPermission() -> requestOverlayPermission()
            !hasNotificationPermission() -> requestNotificationPermission()
            else -> startAppMonitorService()
        }
    }

    private fun hasUsageStatsPermission(): Boolean {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val appList = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            System.currentTimeMillis() - 1000 * 60,
            System.currentTimeMillis()
        )
        return appList?.isNotEmpty() == true
    }

    private fun requestUsageStatsPermission() {
        startActivityForResult(
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS),
            REQUEST_CODE_USAGE_ACCESS
        )
    }

    private fun hasOverlayPermission(): Boolean {
        return Settings.canDrawOverlays(this)
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivityForResult(intent, REQUEST_CODE_OVERLAY_PERMISSION)
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_CODE_NOTIFICATION_PERMISSION
            )
        }
    }

    private fun startAppMonitorService() {
        val serviceIntent = Intent(this, AppMonitorService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_USAGE_ACCESS || requestCode == REQUEST_CODE_OVERLAY_PERMISSION) {
            checkAndRequestPermissions()
        }
    }
}
