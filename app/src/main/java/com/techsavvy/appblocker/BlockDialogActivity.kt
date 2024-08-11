package com.techsavvy.appblocker

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import com.techsavvy.appblocker.ui.BlockDialog

class BlockDialogActivity : ComponentActivity() {

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = getSharedPreferences("AppBlockerPrefs", Context.MODE_PRIVATE)

        setContent {
            MaterialTheme {
                BlockDialog(onApprove = {
                    // Set the approval flag to true
                    sharedPreferences.edit().putBoolean("APPROVED", true).apply()
                    // Allow the game to proceed
                    finish()
                })
            }
        }
    }

    override fun onBackPressed() {
        // Prevent back press
    }
}
