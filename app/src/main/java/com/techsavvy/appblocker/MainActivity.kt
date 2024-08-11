package com.techsavvy.appblocker

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import android.app.usage.UsageStatsManager
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.techsavvy.appblocker.utils.AppMonitorService

class MainActivity : ComponentActivity() {

    companion object {
        const val REQUEST_CODE_USAGE_ACCESS = 1
        const val REQUEST_CODE_OVERLAY_PERMISSION = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                // UI content for your main app
                Text("Parental Control App For game")
            }
        }

        checkAndRequestPermissions()
    }

    private fun checkAndRequestPermissions() {
        if (!hasUsageStatsPermission()) {
            requestUsageStatsPermission()
        } else if (!hasOverlayPermission()) {
            requestOverlayPermission()
        } else if(!hasNotificationPermission()){
            requestNotificationPermission()
        }else if(!hasForegroundServicePermission()){
            requestForegroundServicePermission()
        } else{
            startAppMonitorService()
        }
    }

    private fun hasForegroundServicePermission():Boolean{
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            this.checkSelfPermission(Manifest.permission.FOREGROUND_SERVICE) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun requestForegroundServicePermission(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            this.requestPermissions(arrayOf(Manifest.permission.FOREGROUND_SERVICE),1)
        }
    }


    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_CODE_OVERLAY_PERMISSION
            )
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

    private fun startAppMonitorService() {
        val serviceIntent = Intent(this, AppMonitorService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CODE_USAGE_ACCESS -> {
                if (hasUsageStatsPermission()) {
                    checkAndRequestPermissions()
                } else {
                    Toast.makeText(
                        this,
                        "Usage Stats permission is required.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            REQUEST_CODE_OVERLAY_PERMISSION -> {
                if (hasOverlayPermission()) {
                    checkAndRequestPermissions()
                } else {
                    Toast.makeText(
                        this,
                        "Overlay permission is required.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}
