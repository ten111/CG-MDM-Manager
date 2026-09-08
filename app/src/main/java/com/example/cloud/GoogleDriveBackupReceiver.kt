package com.example.cloud

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GoogleDriveBackupReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d("BackupReceiver", "Received action: $action")

        val database = AppDatabase.getDatabase(context)
        val engine = GoogleDriveBackupEngine(context, database)
        val config = engine.driveService.getAutoBackupConfig()

        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            // Reschedule after device reboot
            GoogleDriveBackupScheduler.scheduleAutoBackup(context, config)
            return
        }

        if (action == GoogleDriveBackupScheduler.ACTION_AUTO_DRIVE_BACKUP) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val result = engine.performCloudBackup(createdBy = "Automated Engine")
                    if (result.isSuccess) {
                        val meta = result.getOrNull()
                        val msg = "शाला डेटा Google Drive में सुरक्षित किया गया (${meta?.recordCounts?.mealsCount ?: 0} भोजन रिकॉर्ड)"
                        GoogleDriveBackupScheduler.showBackupNotification(
                            context,
                            true,
                            msg,
                            meta?.backupId ?: "AUTO-SUCCESS"
                        )
                    } else {
                        val err = result.exceptionOrNull()?.message ?: "Unknown error"
                        GoogleDriveBackupScheduler.showBackupNotification(
                            context,
                            false,
                            "ऑटो बैकअप में त्रुटि: $err",
                            "ERR-AUTO"
                        )
                    }
                } catch (e: Exception) {
                    Log.e("BackupReceiver", "Error during automated backup execution", e)
                } finally {
                    // Reschedule for next occurrence
                    GoogleDriveBackupScheduler.scheduleAutoBackup(context, config)
                    pendingResult.finish()
                }
            }
        }
    }
}
