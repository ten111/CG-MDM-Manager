package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.presentation.navigation.PoshanAppRoot
import com.example.reminder.ReminderManager

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Initialize notification channel and schedule reminder alarms safely
    try {
        ReminderManager.createNotificationChannel(this)
        if (ReminderManager.isReminderEnabled(this)) {
            ReminderManager.scheduleReminders(this)
        }
    } catch (e: Throwable) {
        android.util.Log.w("MainActivity", "Reminder initialization non-fatal error: ${e.message}")
    }

    val navigateToMeal = intent?.getBooleanExtra("EXTRA_NAVIGATE_TO_MEAL", false) ?: false
    val navigateToBackup = intent?.getBooleanExtra("navigate_to_backup", false) ?: false

    // Initialize auto backup scheduler safely
    try {
        val backupConfig = com.example.cloud.GoogleDriveService(this).getAutoBackupConfig()
        if (backupConfig.enabled) {
            com.example.cloud.GoogleDriveBackupScheduler.scheduleAutoBackup(this, backupConfig)
        }
    } catch (e: Throwable) {
        android.util.Log.w("MainActivity", "Backup scheduler initialization error: ${e.message}")
    }

    setContent {
      PoshanAppRoot(
          initialNavigateToMeal = navigateToMeal,
          initialNavigateToBackup = navigateToBackup
      )
    }
  }
}

